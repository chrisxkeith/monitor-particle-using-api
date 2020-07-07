package com.ckkeith.monitor;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ckkeith.monitor.RunParams.Dataset;

public class GoogleSheetsWriter extends Thread {

	private ConcurrentSkipListMap<LocalDateTime, ConcurrentSkipListMap<String, String>> sensorData =
		new ConcurrentSkipListMap<LocalDateTime, ConcurrentSkipListMap<String, String>>();
	private ConcurrentSkipListMap<String, String> sensorNames = 
		new ConcurrentSkipListMap<String, String>();

	public Map.Entry<String, RunParams.SheetConfig> entry;
	
	public GoogleSheetsWriter(Entry<String, RunParams.SheetConfig> entry) {
		this.entry = entry;
	}

	public void addData(EventData eventData) {
		synchronized (this) {
			LocalDateTime start = LocalDateTime.now().minusMinutes(entry.getValue().dataIntervalInMinutes);
			Map.Entry<String, String> sensorDataEntry = eventData.getNextSensorData();
			while (sensorDataEntry != null) {
				// Don't need time granularity finer than reporting granularity.
				long seconds = eventData.timestamp.toEpochSecond(OffsetDateTime.now().getOffset());
				LocalDateTime truncatedTime = eventData.timestamp
						.minusSeconds(seconds % entry.getValue().writeIntervalInSeconds).withNano(0);
				if (start.isBefore(truncatedTime)) {
					String fullSensorName = eventData.deviceName + "\t" + sensorDataEntry.getKey();
					sensorNames.put(fullSensorName, sensorDataEntry.getKey());
	
					ConcurrentSkipListMap<String, String> sensorValues = sensorData.get(truncatedTime);
					if (sensorValues == null) {
						sensorValues = new ConcurrentSkipListMap<String, String>();
					}
					sensorValues.put(fullSensorName, sensorDataEntry.getValue());
					sensorData.put(truncatedTime, sensorValues);
				}
				sensorDataEntry = eventData.getNextSensorData();
			}
		}
	}

	void deleteOldData() {
		LocalDateTime start = LocalDateTime.now().minusMinutes(entry.getValue().dataIntervalInMinutes);
		Set<LocalDateTime> keys = sensorData.keySet();
		Iterator<LocalDateTime> itr = keys.iterator();
		while (itr.hasNext()) {
			LocalDateTime timestamp = itr.next();
			if (timestamp.isBefore(start)) {
				sensorData.remove(timestamp);
			}
		}
	}

	private static LocalDateTime noDataMarker = LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC);

	private void initFirstRow(List<Object> sensorNameRow,
			Map.Entry<String, RunParams.SheetConfig> entry,
			List<Object> mostRecentDataRow) {
		sensorNameRow.add("Time");
		// Put a blank row with a timestamp that is sure to be less than any timestamp in the data.
		mostRecentDataRow.add(Utils.googleSheetsDateFormat.format(noDataMarker));
		Iterator<RunParams.Dataset> datasetIt = entry.getValue().dataSets.iterator();
		while (datasetIt.hasNext()) {
			RunParams.Dataset d = datasetIt.next();
			for (Map.Entry<String, HashMap<String, String>> mc :
						d.microcontrollers.entrySet()) {
				for (String sensorName : mc.getValue().keySet()) {
					sensorNameRow.add(sensorName);
					mostRecentDataRow.add(""); // different photons may report at different times. Start with a placeholder.
				}
			}
		}
	}

	int findSensorIndex(List<Object> sensorNameRow, String fullSensorName) {
		String nameComponents[] = fullSensorName.split("\t");
		String sensorName = nameComponents[nameComponents.length - 1];
		int i = 0;
		for (Object n : sensorNameRow.toArray()) {
			if (sensorName.equals((String)n)) {
				return i;
			}
			i++;
		}
		return -1;
	}

    void fillInBlankRows(LocalDateTime nextRowTimestamp,
                            List<Object> mostRecentDataRow,
                            List<List<Object>> listOfRows) {
        if (this.entry.getValue().missingDataShowsBlank) {
            LocalDateTime prev = Utils.getLocalDateTime((String)mostRecentDataRow.get(0));
            if (!prev.equals(noDataMarker)) {
                List<Object> sensorDataRow = new ArrayList<Object>();
                for (int i = 0; i < mostRecentDataRow.size(); i++) {
                    sensorDataRow.add("");
                }
				prev = prev.plusSeconds(this.entry.getValue().writeIntervalInSeconds);
				if (prev.isBefore(nextRowTimestamp)) {
					sensorDataRow.set(0, Utils.googleSheetsDateFormat.format(prev));
					listOfRows.add(sensorDataRow);
				}
            }
        }
    }
    
 	void loadRows(List<Object> sensorNameRow,
					List<Object> mostRecentDataRow,
					List<List<Object>> listOfRows) {
		Iterator<LocalDateTime> itr = sensorData.keySet().iterator();
		while (itr.hasNext()) {
			LocalDateTime timestamp = itr.next();
			fillInBlankRows(timestamp, mostRecentDataRow, listOfRows);
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
		// Erratic bug leaves out-of-order timestamped rows at end of data in Google Sheet.	
		// Current suspicion is that the delete rows call is not happening (message lost or ignored?).	
		// Fill out to the previous number of rows with blank rows.
/*
		List<Object> blankRow = new ArrayList<Object>(mostRecentDataRow.size());	
		for (int i = 0; i < mostRecentDataRow.size(); i++) {
			blankRow.add("");	
			blankRow.add("blank");	
		}
		for (int i = listOfRows.size(); i <= previousRowCount; i++) {	// first time through, previousRowCount == 0
			listOfRows.add(blankRow);	
		}
*/
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
 
	private void updateSheetById(Map.Entry<String, RunParams.SheetConfig>
			entry) throws Exception {
		String sheetId = entry.getKey();
		if (!validateSheetId(sheetId)) {
			return;
		}
		try {
			List<Object> sensorNameRow = new ArrayList<Object>();

			// Keep most recent data values around to fill out potential 'holes' in the graph.
			List<Object> mostRecentDataRow = new ArrayList<Object>();
			initFirstRow(sensorNameRow, entry, mostRecentDataRow);
			List<List<Object>> listOfRows = new ArrayList<List<Object>>();
			listOfRows.add(sensorNameRow);
			loadRows(sensorNameRow, mostRecentDataRow, listOfRows);
			if (listOfRows.size() > 1) {
				renameSensors(sensorNameRow);
				sensorNameRow.add("Last update: " + Utils.googleSheetsDateFormat.format(LocalDateTime.now()));
				sensorNameRow.add("Row count: " + listOfRows.size());
				sensorNameRow.add("Host: " + Utils.getHostName());
				sensorNameRow.add("Booted: " + Utils.googleSheetsDateFormat.format(Utils.getBootTime()));
		
				GSheetsUtility.updateData(sheetId, "A1", listOfRows);
				Utils.logToConsole("Updated Google Sheet : " + sheetId + ", rows : " + listOfRows.size()
					+ ", columns : " + listOfRows.get(0).size());
			}
		} catch (Exception e) {
			Utils.logToConsole("updateSheetById(): FAILED, Google Sheet : " +
				entry.getKey() + " : " + e.getClass().getCanonicalName() + " " + e.getMessage());
			e.printStackTrace();
			throw e;
		}
	}

	public String getMappedSensorName(String name) {
		for (Dataset dataSet : entry.getValue().dataSets) {
			for (HashMap<String, String> sensorMap : dataSet.microcontrollers.values()) {
				String displayName = sensorMap.get(name);
				if (displayName != null) {
					return displayName;
				}
			}
		}
		return name;
	}

	private void renameSensors(List<Object> list) {
		int i = 0;
		for (Object o: list) {
			list.set(i++, getMappedSensorName((String)o));
		}
	}

	void updateBySheet() {
		try {
			updateSheetById(entry);
		} catch (Exception e) {
			Utils.logToConsole("updateBySheet(): FAILED to update Google Sheet : " +
					entry.getKey() + " : " + e.getClass().getCanonicalName() + " " + e.getMessage());
			e.printStackTrace();
		}
	}

	public void updateGoogleSheets() {
		synchronized(this) {
			deleteOldData();
			updateBySheet();
		}
	}

	private void initSheets() {
		String spreadSheetId = entry.getKey();
		try {
			if (validateSheetId(spreadSheetId)) {
				GSheetsUtility.clear(spreadSheetId, "Sheet1");
			}
		} catch (Exception e) {
			Utils.logToConsole("initSheets(): FAILED to update Google Sheet : " +
				spreadSheetId + " : " + e.getClass().getCanonicalName() + " " + e.getMessage());
			e.printStackTrace();
			// If there's any failure, continue and initialize the next sheet.
		}
	}

	public void run() {
		Utils.logToConsole(this.getClass().getName() + ": thread starting : " + Utils.getCurrentThreadString());
		if (entry.getValue().writeIntervalInSeconds > 0) {
			initSheets(); // If I see junk show up in sheets, add a setting to do this in updateGoogleSheets() on a per-sheet basis.
			while (true) {
				try {
					updateGoogleSheets();
					Thread.sleep(entry.getValue().writeIntervalInSeconds * 1000);
				} catch (Exception e) {
					Utils.logToConsole(e.getMessage());
					e.printStackTrace();
					// If there's any failure, continue and write the sheets at next time interval.
				}
			}
		}
		Utils.logToConsole(this.getClass().getName() + ": thread exiting : " + Utils.getCurrentThreadString());
	}
}
