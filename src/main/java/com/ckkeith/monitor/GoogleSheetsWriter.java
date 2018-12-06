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
	Map<String, String> deviceNameToSheetId = new HashMap<String, String>();
	

	public GoogleSheetsWriter(AccountMonitor accountMonitor, PivotDataApp pivotDataApp) {
		this.accountMonitor = accountMonitor;
		if (pivotDataApp != null) {
//			pivotDataApp.fillInData(this);
		}
// TODO : parameterize sheet id.
		deviceNameToSheetId.put("US Foods", "1qCHfRDno-Lp-fzIc_xUbq7kjU0lkxLrjGb9dVqtWAuE");
		deviceNameToSheetId.put("verdical_system_2", "1n4bcbzooUjyah2Hc506FNdy4RwHSAETjBLNo48AFwEI");
//		deviceNameToSheetId.put("trochee_scraper", "12JP_5OPXauD_RiYQChZ0W9dfFP7CHZs6HYhEbsxIyzw");
//		deviceNameToSheetId.put("bobcat_pizza", "1c3xX70MlulYnxcphZnfmbEBtOokTHTBK1Dxi1KfOZE4");
		deviceNameToSheetId.put("CatDev", "1d9oT0tGskhF87KSjRYYBj3svNFl3dzC3hrCrPqn5P1c");		
	}

	public void addData(SensorDataPoint sensorDataPoint) {
		synchronized (this) {
			String fullSensorName = sensorDataPoint.deviceName + " " + sensorDataPoint.sensorName;
			sensorNames.put(fullSensorName, sensorDataPoint.sensorName);

			// Don't need time granularity finer than reporting granularity.
			int seconds = sensorDataPoint.timestamp.getSecond();
			LocalDateTime truncatedTime = sensorDataPoint.timestamp
					.minusSeconds(seconds % accountMonitor.runParams.sheetsWriteIntervalInSeconds).withNano(0);
			ConcurrentSkipListMap<String, String> sensorValues = sensorData.get(truncatedTime);
			if (sensorValues == null) {
				sensorValues = new ConcurrentSkipListMap<String, String>();
			}
			sensorValues.put(fullSensorName, sensorDataPoint.sensorData);
			sensorData.put(truncatedTime, sensorValues);
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
		String sheetId = deviceNameToSheetId.get(deviceName);
		if (sheetId != null && !sheetId.isEmpty()) {
			try {
				List<List<Object>> listOfRows = new ArrayList<List<Object>>();
				List<Object> sensorNameRow = new ArrayList<Object>();
				sensorNameRow.add(""); // time stamp column
				Iterator<String> sensorIt = sensorNames.keySet().iterator();
				while (sensorIt.hasNext()) {
					String sensorName = sensorIt.next();
					if ((sensorName.startsWith(deviceName))) {
						sensorNameRow.add(sensorName);
					}
				}
				listOfRows.add(sensorNameRow);
				Iterator<LocalDateTime> itr = sensorData.keySet().iterator();
				while (itr.hasNext()) {
					LocalDateTime timestamp = itr.next();
					List<Object> sensorDataRow = new ArrayList<Object>();
					sensorDataRow.add(Utils.googleSheetsDateFormat.format(timestamp));
					ConcurrentSkipListMap<String, String> entries = sensorData.get(timestamp);
					sensorIt = entries.keySet().iterator();
					while (sensorIt.hasNext()) {
						String sensorName = sensorIt.next();
						if ((sensorName.startsWith(deviceName))) {
							sensorDataRow.add(entries.get(sensorName));
						}
					}
					if (sensorDataRow.size() > 1) { // must be more than just the time stamp
						listOfRows.add(sensorDataRow);
					}
				}
				GSheetsUtility.updateData(sheetId, "A1", listOfRows);
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
					updateSheet(deviceName);
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
