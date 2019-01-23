package com.ckkeith.monitor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

public class GoogleSheetsWriter extends Thread {

	private AccountMonitor accountMonitor;
	private ConcurrentSkipListMap<LocalDateTime, ConcurrentSkipListMap<String, String>> sensorData = new ConcurrentSkipListMap<LocalDateTime, ConcurrentSkipListMap<String, String>>();
	private ConcurrentSkipListMap<String, String> sensorNames = new ConcurrentSkipListMap<String, String>();
	private HashMap<String, Integer> mostRecentRowCount = new HashMap<String, Integer>();

	public GoogleSheetsWriter(AccountMonitor accountMonitor, PivotDataApp pivotDataApp) {
		this.accountMonitor = accountMonitor;
		if (pivotDataApp != null) {
//			pivotDataApp.fillInData(this);
		}
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
		LocalDateTime start = LocalDateTime.now().minusMinutes(accountMonitor.runParams.dataIntervalInMinutes);
		Set<LocalDateTime> keys = sensorData.keySet();
		Iterator<LocalDateTime> itr = keys.iterator();
		while (itr.hasNext()) {
			LocalDateTime timestamp = itr.next();
			if (timestamp.isBefore(start)) {
				sensorData.remove(timestamp);
			}
		}
	}

	// In case there are fewer rows than before,
	// fill in with blanks to avoid junk data at right end of graph.
	private void addBlankRows(List<List<Object>> listOfRows, String deviceName) {
		int blankRowsToAdd = this.mostRecentRowCount.get(deviceName) - listOfRows.size();
		List<Object> blankRow = new ArrayList<Object>();
		int numColumns = sensorNames.keySet().size();
		while (numColumns > 0) {
			blankRow.add("");
			numColumns--;
		}
		this.mostRecentRowCount.put(deviceName, listOfRows.size());
		while (blankRowsToAdd > 0) {
			listOfRows.add(blankRow);
			blankRowsToAdd--;
		}
	}

	private void updateSheet(String deviceName) throws Exception {
		try {
			String sheetId = accountMonitor.deviceNameToSheetId.get(deviceName);
			List<List<Object>> listOfRows = new ArrayList<List<Object>>();
			List<Object> sensorNameRow = new ArrayList<Object>();

			// Keep most recent data values around to fill out potential 'holes' in the graph.
			List<Object> mostRecentDataRow = new ArrayList<Object>();
			sensorNameRow.add(""); // time stamp column
			mostRecentDataRow.add(Utils.googleSheetsDateFormat.format(LocalDateTime.now().withYear(1980)));
			Iterator<String> sensorIt = sensorNames.keySet().iterator();
			while (sensorIt.hasNext()) {
				String fullSensorName = sensorIt.next();
				if (fullSensorName.startsWith(deviceName)) {
					sensorNameRow.add(sensorNames.get(fullSensorName));
				}
				mostRecentDataRow.add("");
			}
			listOfRows.add(sensorNameRow);

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
					int i = 1;
					while (sensorNameIt.hasNext()) {
						String fullSensorName = sensorNameIt.next();
						if (fullSensorName.startsWith(deviceName)) {
							String val = entries.get(fullSensorName);
							if (i < sensorNameRow.size() && val != null && !val.isEmpty()) {
								sensorDataRow.set(i++, val);
							}
						}
					}
					listOfRows.add(sensorDataRow);
					mostRecentDataRow.clear();
					mostRecentDataRow.addAll(sensorDataRow);
				}
			}
			addBlankRows(listOfRows, deviceName);
			if (listOfRows.size() > 1) {
				GSheetsUtility.updateData(accountMonitor.accountName, sheetId, "A1", listOfRows);
				Utils.logToConsole("Updated Google Sheet " + sheetId + " for : " + deviceName + " : rows : " + listOfRows.size()
					+ ", columns : " + listOfRows.get(0).size());
			}
		} catch (Exception e) {
			Utils.logToConsole("updateSheet(): FAILED to update Google Sheet for : " + deviceName + " : " + e.getClass().getCanonicalName() + " " + e.getMessage());
			e.printStackTrace();
			throw e;
		}
	}

	void updateGoogleSheets() {
		synchronized(this) {
			deleteOldData();
			for (String deviceName : accountMonitor.deviceMonitors.keySet()) {
				try {
					String spreadSheetId = accountMonitor.deviceNameToSheetId.get(deviceName);
					if (spreadSheetId != null && !spreadSheetId.isEmpty()) {
						updateSheet(deviceName);
					}
				} catch (Exception e) {
					Utils.logToConsole("updateGoogleSheets(): FAILED to update Google Sheet for : " + deviceName + " : " + e.getClass().getCanonicalName() + " " + e.getMessage());
					e.printStackTrace();
					// If there's any failure, continue and update the next sheet.
				}
			}
		}
	}

	void initSheets() {
		for (String deviceName : accountMonitor.deviceMonitors.keySet()) {
			try {
				String spreadSheetId = accountMonitor.deviceNameToSheetId.get(deviceName);
				if (spreadSheetId != null) {
						GSheetsUtility.clear(spreadSheetId, "Sheet1!A1:Z1000");
						this.mostRecentRowCount.put(deviceName, 0);
				}
			} catch (Exception e) {
				Utils.logToConsole("initSheets(): FAILED to update Google Sheet for : " + deviceName + " : " + e.getClass().getCanonicalName() + " " + e.getMessage());
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
