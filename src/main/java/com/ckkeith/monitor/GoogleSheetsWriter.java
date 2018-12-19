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
// Implement when it's asked for.
//			pivotDataApp.fillInData(this);
		}
// Parameterize sheet ids when necessary.
		if (accountMonitor.accountName.equals("chris.keith@gmail.com")) {
			deviceNameToSheetId.put("thermistor2-test", "1Tq1lGWuO4kipyKz_zIaXOQhVN5B4as0N0VEG_PTgP9w");
		} else if (accountMonitor.accountName.equals("ara@verdical.io")) {
			deviceNameToSheetId.put("US Foods", "1qCHfRDno-Lp-fzIc_xUbq7kjU0lkxLrjGb9dVqtWAuE");
//			deviceNameToSheetId.put("verdical_tester_usfoods", "1xWj_bFERM0tvtJYdak0Ujba3h9dPJjpn2-YK4Ibji2I");
//			deviceNameToSheetId.put("verdical_tester_5", "1fA8T5qodNa48EQro1B5FIwHMeAd3kciOdx6dgAAOWFI");
		}
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
				List<Object> mostRecentDataRow = new ArrayList<Object>();
				sensorNameRow.add(""); // time stamp column
				mostRecentDataRow.add(Utils.googleSheetsDateFormat.format(LocalDateTime.now().withYear(1980)));
				Iterator<String> sensorIt = sensorNames.keySet().iterator();
				while (sensorIt.hasNext()) {
					String sensorName = sensorIt.next();
					if ((sensorName.startsWith(deviceName))) {
						sensorNameRow.add(sensorName);
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
					String spreadSheetId = deviceNameToSheetId.get(deviceName);
					if (spreadSheetId != null) {
						// TODO : If there is any way to get 'existing data range',
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
