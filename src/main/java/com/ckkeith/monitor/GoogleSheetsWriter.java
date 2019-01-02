package com.ckkeith.monitor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

public class GoogleSheetsWriter extends Thread {

	private AccountMonitor accountMonitor;
	private ConcurrentSkipListMap<LocalDateTime, ConcurrentSkipListMap<String, String>> sensorData = new ConcurrentSkipListMap<LocalDateTime, ConcurrentSkipListMap<String, String>>();
	private ConcurrentSkipListMap<String, String> sensorNames = new ConcurrentSkipListMap<String, String>();

	public GoogleSheetsWriter(AccountMonitor accountMonitor, PivotDataApp pivotDataApp) {
		this.accountMonitor = accountMonitor;
		if (pivotDataApp != null) {
// Implement when it's asked for.
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

	private void updateSheet(String deviceName) throws Exception {
		String sheetId = accountMonitor.deviceNameToSheetId.get(deviceName);
		if (sheetId != null && !sheetId.isEmpty()) {
			try {
				List<List<Object>> listOfRows = new ArrayList<List<Object>>();
				List<Object> sensorNameRow = new ArrayList<Object>();
				List<Object> mostRecentDataRow = new ArrayList<Object>();
				sensorNameRow.add(""); // time stamp column
				mostRecentDataRow.add(Utils.googleSheetsDateFormat.format(LocalDateTime.now().withYear(1980)));
				Iterator<String> sensorIt = sensorNames.keySet().iterator();
				while (sensorIt.hasNext()) {
					String fullSensorName = sensorIt.next();
					if ((fullSensorName.startsWith(deviceName))) {
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
							String sensorName = sensorNameIt.next();
							String val = entries.get(sensorName);
							if (i < sensorNameRow.size() && val != null && !val.isEmpty()) {
								sensorDataRow.set(i++, val);
							}
						}
	                    listOfRows.add(sensorDataRow);
	                    mostRecentDataRow.clear();
	                    mostRecentDataRow.addAll(sensorDataRow);
					}
				}
				if (listOfRows.size() > 1) {
					GSheetsUtility.updateData(sheetId, "A1", listOfRows);
				}
				Utils.logToConsole("Updated Google Sheet for : " + deviceName + " : rows : " + listOfRows.size()
						+ ", columns : " + listOfRows.get(0).size());
			} catch (Exception e) {
				Utils.logToConsole("FAILED to update Google Sheet for : " + deviceName + " : " + e.getMessage());
				e.printStackTrace();
				throw e;
			}
		}
	}

	void updateGoogleSheets() {
		synchronized(this) {
			deleteOldData();
			for (String deviceName : accountMonitor.deviceMonitors.keySet()) {
				try {
					String spreadSheetId = accountMonitor.deviceNameToSheetId.get(deviceName);
					if (spreadSheetId != null) {
						// TO DO : If there is any way to get 'existing data range',
						// use it here instead of hardcoding the range.
//						GSheetsUtility.clear(spreadSheetId, "Sheet1!A1:I400");
						updateSheet(deviceName);
					}
				} catch (Exception e) {
					Utils.logToConsole("FAILED to update Google Sheet for : " + deviceName + " : " + e.getMessage());
					e.printStackTrace();
					// If there's any failure, continue and update the next sheet.
				}
			}
		}
	}

	public void run() {
		Utils.logToConsole(this.getClass().getName() + ": thread starting.");
		if (accountMonitor.runParams.sheetsWriteIntervalInSeconds > 0) {
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
