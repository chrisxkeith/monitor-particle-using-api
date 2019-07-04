package com.ckkeith.monitor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GoogleSheetsWriter extends Thread {

	private AccountMonitor accountMonitor;
	private ConcurrentSkipListMap<LocalDateTime, ConcurrentSkipListMap<String, String>> sensorData =
		new ConcurrentSkipListMap<LocalDateTime, ConcurrentSkipListMap<String, String>>();
	private ConcurrentSkipListMap<String, String> sensorNames = 
		new ConcurrentSkipListMap<String, String>();

	public GoogleSheetsWriter(AccountMonitor accountMonitor) {
		this.accountMonitor = accountMonitor;
	}

	public void addData(EventData eventData) {
		synchronized (this) {
			Map.Entry<String, String> sensorDataEntry = eventData.getNextSensorData();
			while (sensorDataEntry != null) {
				String fullSensorName = eventData.deviceName + sensorDataEntry.getKey();
				sensorNames.put(fullSensorName, sensorDataEntry.getKey());
	
				// Don't need time granularity finer than reporting granularity.
				int seconds = eventData.timestamp.getSecond();
				LocalDateTime truncatedTime = eventData.timestamp
						.minusSeconds(seconds % accountMonitor.runParams.sheetsWriteIntervalInSeconds).withNano(0);
				ConcurrentSkipListMap<String, String> sensorValues = sensorData.get(truncatedTime);
				if (sensorValues == null) {
					sensorValues = new ConcurrentSkipListMap<String, String>();
				}
				sensorValues.put(fullSensorName, sensorDataEntry.getValue());
				sensorData.put(truncatedTime, sensorValues);
				sensorDataEntry = eventData.getNextSensorData();
			}
		}
	}

	void deleteOldData() {
		LocalDateTime start = LocalDateTime.now().minusMinutes(accountMonitor.runParams.sheetsDataIntervalInMinutes);
		Set<LocalDateTime> keys = sensorData.keySet();
		Iterator<LocalDateTime> itr = keys.iterator();
		while (itr.hasNext()) {
			LocalDateTime timestamp = itr.next();
			if (timestamp.isBefore(start)) {
				sensorData.remove(timestamp);
			}
		}
	}

	private void initFirstRow(List<Object> sensorNameRow,
			Map.Entry<String, ArrayList<RunParams.Dataset>> entry,
			List<Object> mostRecentDataRow) {
		sensorNameRow.add("timestamp");
		// Put a blank row with a timestamp that is sure to be less than any timestamp in the data.
		mostRecentDataRow.add(Utils.googleSheetsDateFormat.format(LocalDateTime.now().withYear(1980)));
		Iterator<RunParams.Dataset> datasetIt = entry.getValue().iterator();
		while (datasetIt.hasNext()) {
			RunParams.Dataset d = datasetIt.next();
			for (Map.Entry<String, HashSet<String>> mc : d.microcontrollers.entrySet()) {
				for (String sensorName : mc.getValue()) {
					String fullSensorName = mc.getKey() + sensorName;
					sensorNameRow.add(fullSensorName);
				}
			}
			mostRecentDataRow.add("");
		}
		sensorNameRow.add(Utils.getHostName());
	}

	int findSensorIndex(List<Object> sensorNameRow, String fullSensorName) {
		int i = 0;
		for (Object n : sensorNameRow.toArray()) {
			if (((String)n).equals(fullSensorName)) {
				return i;
			}
			i++;
		}
		return -1;
	}
	void loadRows(List<Object> sensorNameRow,
					List<Object> mostRecentDataRow,
					List<List<Object>> listOfRows) {
		Iterator<LocalDateTime> itr = sensorData.keySet().iterator();
		while (itr.hasNext()) {
			LocalDateTime timestamp = itr.next();
			LocalDateTime prev = Utils.getLocalDateTime((String)mostRecentDataRow.get(0));
			if (prev.isBefore(timestamp)) {
				List<Object> sensorDataRow = new ArrayList<Object>();
				sensorDataRow.addAll(mostRecentDataRow);
				sensorDataRow.set(0, Utils.googleSheetsDateFormat.format(timestamp));

				ConcurrentSkipListMap<String, String> entries = sensorData.get(timestamp);
				Iterator<String> sensorNameIt = sensorNames.keySet().iterator();
				while (sensorNameIt.hasNext()) {
					String fullSensorName = sensorNameIt.next();
					int sensorIndex = findSensorIndex(sensorNameRow, fullSensorName);
					if (sensorIndex > -1) {
						String val = entries.get(fullSensorName);
						if (val != null && !val.isEmpty()) {
							sensorDataRow.set(sensorIndex, val);
						}
					}
				}
				listOfRows.add(sensorDataRow);
				mostRecentDataRow.clear();
				mostRecentDataRow.addAll(sensorDataRow);
			}
		}
	}

	// \x2D == '-'
	private Pattern p = Pattern.compile("[a-zA-Z_0-9\\x2D]*");

	private boolean validateSheetId(String sheetId) {
		if (sheetId == null) {
			Utils.logToConsole("Google Sheets id is null.");
			return false;
		}
		Matcher m = p.matcher(sheetId);
		if (!m.matches()) {
			Utils.logToConsole("Incorrect Google Sheets id: " + sheetId);
			return false;
		}
		return true;
	}
 
	private void updateSheetById(Map.Entry<String, ArrayList<RunParams.Dataset>>
			entry) throws Exception {
		String sheetId = entry.getKey();
		if (!validateSheetId(sheetId)) {
			return;
		}
		String opToDo = "unknown";
		try {
			List<Object> sensorNameRow = new ArrayList<Object>();

			// Keep most recent data values around to fill out potential 'holes' in the graph.
			List<Object> mostRecentDataRow = new ArrayList<Object>();
			initFirstRow(sensorNameRow, entry, mostRecentDataRow);
			List<List<Object>> listOfRows = new ArrayList<List<Object>>();
			listOfRows.add(sensorNameRow);
			loadRows(sensorNameRow, mostRecentDataRow, listOfRows);
			if (listOfRows.size() > 1) {
				for (Integer i = listOfRows.size() - 1; i >= 0; i--) {
					String itemForChecking = (String)listOfRows.get(i).get(0);
					if (itemForChecking == null || itemForChecking.isEmpty()) {
						listOfRows.remove(listOfRows.get(i));
						Utils.logToConsole("WARNING : Removed row: " +  i.toString() + " on sheet: " + sheetId);
					}
				}
				opToDo = "deleteRows";
				GSheetsUtility.deleteRows(sheetId, 0, 1000); // for the future: keep previous count around and only delete those rows.
				opToDo = "updateData";
				GSheetsUtility.updateData(sheetId, "A1", listOfRows);
				Utils.logToConsole("Updated Google Sheet : " + sheetId + ", rows : " + listOfRows.size()
					+ ", columns : " + listOfRows.get(0).size());
			}
		} catch (Exception e) {
			Utils.logToConsole("updateSheetById(): " + opToDo + " FAILED, Google Sheet : " +
				entry.getKey() + " : " + e.getClass().getCanonicalName() + " " + e.getMessage());
			e.printStackTrace();
			throw e;
		}
	}

	void updateBySheet() {
		for (Map.Entry<String, ArrayList<RunParams.Dataset>> entry :
						accountMonitor.runParams.sheets.entrySet()) {
			try {
				updateSheetById(entry);
			} catch (Exception e) {
				Utils.logToConsole("updateBySheet(): FAILED to update Google Sheet : " +
						entry.getKey() + " : " + e.getClass().getCanonicalName() + " " + e.getMessage());
				e.printStackTrace();
				// If there's any failure, continue and update the next sheet.
			}
			
		}
	}

	void updateGoogleSheets() {
		synchronized(this) {
			deleteOldData();
			updateBySheet();
		}
	}

	void initSheets() {
		for (Map.Entry<String, ArrayList<RunParams.Dataset>> entry :
						accountMonitor.runParams.sheets.entrySet()) {
			String spreadSheetId = entry.getKey();
			try {
				if (validateSheetId(spreadSheetId)) {
					GSheetsUtility.clear(spreadSheetId, "Sheet1!A1:Z1000");
				}
			} catch (Exception e) {
				Utils.logToConsole("initSheets(): FAILED to update Google Sheet : " +
					spreadSheetId + " : " + e.getClass().getCanonicalName() + " " + e.getMessage());
				e.printStackTrace();
				// If there's any failure, continue and initialize the next sheet.
			}
		}
	}

	public void run() {
		Utils.logToConsole(this.getClass().getName() + ": thread starting.");
		if (accountMonitor.runParams.sheetsWriteIntervalInSeconds > 0) {
			initSheets();
			while (true) {
				try {
					updateGoogleSheets();
					Thread.sleep(accountMonitor.runParams.sheetsWriteIntervalInSeconds * 1000);
				} catch (Exception e) {
					Utils.logToConsole(e.getMessage());
					e.printStackTrace();
					// If there's any failure, continue and write the sheets at next time interval.
				}
			}
		}
		Utils.logToConsole(this.getClass().getName() + ": thread exiting.");
	}
}
