// Please credit chris.keith@gmail.com
package com.ckkeith.monitor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

public class HtmlFileDataWriter extends Thread {

	private AccountMonitor accountMonitor;
	
	private ConcurrentSkipListMap<LocalDateTime, ConcurrentSkipListMap<String, String>> sensorData = new ConcurrentSkipListMap<LocalDateTime, ConcurrentSkipListMap<String, String>>();
	private ConcurrentSkipListMap<String, String> sensorNames = new ConcurrentSkipListMap<String, String>();

	public HtmlFileDataWriter(AccountMonitor accountMonitor) {
		this.accountMonitor = accountMonitor;
	}
	
	public void addData(SensorDataPoint sensorDataPoint) {
		synchronized (this) {
			String fullSensorName = sensorDataPoint.deviceName + " " + sensorDataPoint.sensorName;
			sensorNames.put(fullSensorName, sensorDataPoint.sensorName);
			ConcurrentSkipListMap<String, String> sensorValues = sensorData.get(sensorDataPoint.timestamp);
			if (sensorValues == null) {
				sensorValues = new ConcurrentSkipListMap<String, String>();
			}
			sensorValues.put(fullSensorName, sensorDataPoint.sensorData);
			sensorData.put(sensorDataPoint.timestamp, sensorValues);
		}
	}

	private void writeln(FileWriter fw, String s) throws Exception {
		fw.write(s + System.getProperty("line.separator"));
	}

	private int colorIndex = 0;

	private String getNextColor() {
		final String colors[] = { "255,99,132,1", "75, 192, 192, 1", "153, 102, 255, 1",
				"255, 159, 64, 1", "54, 162, 235, 1" };
		String nextColor = colors[colorIndex++];
		if (colorIndex >= colors.length) {
			colorIndex = 0;
		}
		return nextColor;
	}

	private int addDataForSensor(FileWriter jsonStream, String sensorName) throws Exception {
		writeln(jsonStream, "\t\t\t\t\"data\" : [");
		Set<LocalDateTime> keys = sensorData.keySet();
		Iterator<LocalDateTime> itr = keys.iterator();
		boolean first = true;
		int nDataPoints = 0;
		while (itr.hasNext()) {
			LocalDateTime timestamp = itr.next();
			ConcurrentSkipListMap<String, String> entries = sensorData.get(timestamp);

			String val = entries.get(sensorName);
			if (val != null && !val.isEmpty()) {
				StringBuilder sb2 = new StringBuilder();
				if (!first) {
					sb2.append(",");
				} else {
					first = false;
				}
				sb2.append("{ \"t\" : \"").append(timestamp).append("\", \"y\" : " + val + "}");
				writeln(jsonStream, sb2.toString());
				nDataPoints++;
			}
		}
		writeln(jsonStream, "\t\t\t\t]");
		return nDataPoints;
	}

	private Integer writeJson(FileWriter jsonStream, String deviceName) throws Exception {
		Integer nDataPoints = 0;
		try {
			colorIndex = 0;
			writeln(jsonStream, "\t{ \"datasets\" : ");
			writeln(jsonStream, "\t\t[");

			Iterator<String> sensorIt = sensorNames.keySet().iterator();
			Boolean firstSensor = true;
			while (sensorIt.hasNext()) {
				String sensorName = sensorIt.next();
				if (((deviceName == null) || sensorName.startsWith(deviceName))
						&& ((accountMonitor.runParams.devicesToReport.isEmpty()
								|| accountMonitor.runParams.devicesToReport.contains(sensorName)))) {
					StringBuilder sb1 = new StringBuilder("\t\t\t");
					if (firstSensor) {
						firstSensor = false;
					} else {
						sb1.append(" , ");
					}
					sb1.append("{ \"label\" : \"");
					sb1.append(sensorName).append("\",");
					writeln(jsonStream, sb1.toString());
					writeln(jsonStream, "\"lineTension\" : 0,");
					writeln(jsonStream, "\t\t\t\"borderColor\" : \"rgba(" + getNextColor() + ")\",");
					writeln(jsonStream, "\t\t\t\"backgroundColor\" : \"rgba(0, 0, 0, 0.0)\",");
					nDataPoints += addDataForSensor(jsonStream, sensorName);
					writeln(jsonStream, "\t\t\t}");
				}
			}
		} finally {
			writeln(jsonStream, "\t\t]");
			writeln(jsonStream, "\t}");
		}
		return nDataPoints;
	}

	private void appendFromFileToFile(FileWriter htmlStream, String fromFile, String fileName) throws Exception {
		try (BufferedReader br = new BufferedReader(new FileReader(new File(fromFile)))) {
			String line;
			while ((line = br.readLine()) != null) {
				if (line.contains("_fileNameGoesHere_.html")) {
					String accountPath = Utils.getHomeURLPath(
							"Documents" + File.separator + "tmp" + File.separator + Utils.getHostName() + File.separator
									+ Utils.getSafeName(accountMonitor.accountName));
					line = line.replace("_fileNameGoesHere_", fileName)
							.replace("_homePathGoesHere_", accountPath)
							.replace("_writeIntervalGoesHere_",
									new Integer(accountMonitor.runParams.htmlWriteIntervalInSeconds).toString());
				} else if (line.contains("_suggestedTimeMin_")) {
					LocalDateTime then = LocalDateTime.now().minusMinutes(accountMonitor.runParams.dataIntervalInMinutes);
					line = line.replace("_suggestedTimeMin_", Utils.googleSheetsDateFormat.format(then));
				} else if (line.contains("_suggestedTimeMax_")) {
					line = line.replace("_suggestedTimeMax_", Utils.googleSheetsDateFormat.format(LocalDateTime.now()));
				}
				writeln(htmlStream, line);
			}
		}
	}

	int currentFileNumber = 0;

	int getNextFileNumber(int thisFileNumber) {
		if (thisFileNumber == 0) {
			return 1;
		}
		return 0;
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
	
	private boolean chromeStarted = false;
	@SuppressWarnings("unused")
	private void startChrome(String fn) {
		if (! chromeStarted) {
		    try {
				List<String> params = new ArrayList<String>();
				params.add("c:/Program Files (x86)/Google/Chrome/Application/chrome.exe");
				params.add(fn);
			    ProcessBuilder pb = new ProcessBuilder(params);
				pb.start();
				chromeStarted = true;
			} catch (Exception e) {
				Utils.logToConsole("Running chrome failed, currently only works on Windows. " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	void writeOneHtml(String deviceName, int thisFileNumber) {
		String thisFileName = "not yet specified";
		int nDataPoints = 0;
		try {
			deleteOldData();
			String safeFn;
			if (deviceName == null) {
				safeFn = "all";
			} else {
				safeFn = Utils.getSafeName(deviceName);
			}
			String fileName = Utils.getLogFileName(accountMonitor.accountName, safeFn + "NNN.html");
			thisFileName = fileName.replace("NNN", String.format("%03d", thisFileNumber));
			String dir = new File(fileName).getParent();
			File tempFile = File.createTempFile("tmp", ".html", new File(dir));
			FileWriter htmlStream = new FileWriter(tempFile.getCanonicalPath(), false);
			try {
				appendFromFileToFile(htmlStream, "src/main/resources/prefix.html",
						safeFn + String.format("%03d", getNextFileNumber(thisFileNumber)));
				nDataPoints = writeJson(htmlStream, deviceName);
				appendFromFileToFile(htmlStream, "src/main/resources/suffix.html", "junk");
			} finally {
				htmlStream.close();
			}
			File thisFile = new File(thisFileName);
			try {
				thisFile.delete();
			} catch (Exception ex) {
				Utils.logToConsole(
						"thisFile.delete() failed : " + ex.getMessage() + " " + ex.getClass().getName() + ", continuing");
			}
			try {
				Files.move(tempFile.toPath(), thisFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
				Utils.logToConsole(
						"Wrote " + thisFileName + " : # data points : " + new Integer(nDataPoints).toString());
			} catch (Exception ex) {
				Utils.logToConsole(
						"Files.move() failed : " + ex.getMessage() + " " + ex.getClass().getName() + ", continuing");
			}
//Only use this when NOT running from Task Scheduler
//			startChrome(thisFileName);
		} catch (Exception e) {
			Utils.logToConsole("FAILED to write : " + thisFileName + " : # data points : "
					+ new Integer(nDataPoints).toString() + " : " + e.getMessage());
			e.printStackTrace();
			// If there's any failure, continue and write the next file at the appropriate time.
		}
	}

	void writeHtml() throws Exception {
		synchronized (this) {
			int thisFileNumber = currentFileNumber;
			for (DeviceMonitor dm : accountMonitor.deviceMonitors.values()) {
				writeOneHtml(dm.device.getName(), thisFileNumber);
			}
			writeOneHtml(null, thisFileNumber);
			currentFileNumber = getNextFileNumber(currentFileNumber);
		}
	}
	
	public void run() {
		Utils.logToConsole("HtmlFileDataWriter thread starting.");
		try {
			while (true) {
				writeHtml();
				Thread.sleep(accountMonitor.runParams.htmlWriteIntervalInSeconds * 1000);
			}
		} catch (Exception e) {
			Utils.logToConsole(e.getMessage());
			e.printStackTrace();
		}
	}
}
