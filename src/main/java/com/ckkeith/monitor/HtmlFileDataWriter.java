package com.ckkeith.monitor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
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
			sensorNames.put(sensorDataPoint.sensorName, sensorDataPoint.sensorName);
			ConcurrentSkipListMap<String, String> sensorValues = sensorData.get(sensorDataPoint.timestamp);
			if (sensorValues == null) {
				sensorValues = new ConcurrentSkipListMap<String, String>();
			}
			sensorValues.put(sensorDataPoint.sensorName, sensorDataPoint.sensorData);
			sensorData.put(sensorDataPoint.timestamp, sensorValues);
		}
	}

	private void writeln(FileWriter fw, String s) throws Exception {
		fw.write(s + System.getProperty("line.separator"));
	}

	private int colorIndex = 0;

	private String getNextColor() {
		final String colors[] = { "255,99,132,1", "255, 206, 86, 1", "75, 192, 192, 1", "153, 102, 255, 1",
				"255, 159, 64, 1", "54, 162, 235, 1" };
		String nextColor = colors[colorIndex++];
		if (colorIndex >= colors.length) {
			colorIndex = 0;
		}
		return nextColor;
	}

	private int writeJson(FileWriter jsonStream) throws Exception {
		int nDataPoints = 0;
		try {
			colorIndex = 0;
			writeln(jsonStream, "\t{ \"datasets\" : ");
			writeln(jsonStream, "\t\t[");

			Iterator<String> sensorIt = sensorNames.keySet().iterator();
			boolean firstSensor = true;
			while (sensorIt.hasNext()) {
				String sensorName = sensorIt.next();
				StringBuilder sb1 = new StringBuilder("\t\t\t");
				if (firstSensor) {
					firstSensor = false;
				} else {
					sb1.append(" , ");
				}
				sb1.append("{ \"label\" : \"");
				sb1.append(sensorName).append("\",");
				writeln(jsonStream, sb1.toString());
				writeln(jsonStream, "\t\t\t\"borderColor\" : \"rgba(" + getNextColor() + ")\",");
				writeln(jsonStream, "\t\t\t\"backgroundColor\" : \"rgba(0, 0, 0, 0.0)\",");
				writeln(jsonStream, "\t\t\t\t\"data\" : [");

				Set<LocalDateTime> keys = sensorData.keySet();
				Iterator<LocalDateTime> itr = keys.iterator();
				boolean first = true;
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
				writeln(jsonStream, "\t\t\t}");
			}
		} finally {
			writeln(jsonStream, "\t\t]");
			writeln(jsonStream, "\t}");
		}
		return nDataPoints;
	}

	private void appendFromFileToFile(FileWriter htmlStream, String fromFile, String nextFileNumber) throws Exception {
		try (BufferedReader br = new BufferedReader(new FileReader(new File(fromFile)))) {
			String line;
			while ((line = br.readLine()) != null) {
				if (line.contains("_fileIndexGoesHere_.html")) {
					line = line.replace("_fileIndexGoesHere_", nextFileNumber)
							.replace("_homePathGoesHere_", Utils.getHomeURLPath())
							.replace("_writeIntervalGoesHere_",
									new Integer(Main.runParams.htmlWriteIntervalInSeconds).toString());
				}
				writeln(htmlStream, line);
			}
		}
	}

	int nextFileNumber = 0;

	String getNextFileNumber() {
		nextFileNumber++;
		if (nextFileNumber > Main.runParams.nHtmlFiles) {
			nextFileNumber = 0;
		}
		return String.format("%03d", nextFileNumber);
	}

	void deleteOldData() {
		LocalDateTime start = LocalDateTime.now().minusMinutes(Main.runParams.dataIntervalInMinutes);
		Set<LocalDateTime> keys = sensorData.keySet();
		Iterator<LocalDateTime> itr = keys.iterator();
		while (itr.hasNext()) {
			LocalDateTime timestamp = itr.next();
			if (timestamp.isBefore(start)) {
				sensorData.remove(timestamp);
			}
		}
	}
	
	// If/when move to Java 9, could use ProcessHandle to remove some Windows-specific code:
	// https://stackoverflow.com/questions/54686/how-to-get-a-list-of-current-open-windows-process-with-java
	boolean chomeIsRunning() {
		try {
			String p[] = { "tasklist.exe", "/v"};
			String output = Utils.runCommandForOutput(Arrays.asList(p));
			String outputLines[] = output.split(System.getProperty("line.separator"));
			for (String s : outputLines) {
				if (s.contains("chrome")) {
					Utils.logToConsole("String from tasklist : " + s);
					if (s.contains("all") && s.contains("html")) {
						return true;
					}
				}
			}
		} catch (Exception e) {
			Utils.logToConsole("Running tasklist failed, currently only works on Windows. " + e.getMessage());
			e.printStackTrace();
			return true; // Don't run extra instances of Chrome.
		}
		return false;
	}

	void startChromeIfNecessary(String fn) {
		if (! chomeIsRunning()) {
		    try {
				List<String> params = new ArrayList<String>();
				params.add("c:/Program Files (x86)/Google/Chrome/Application/chrome.exe");
				params.add(fn);
			    ProcessBuilder pb = new ProcessBuilder(params);
				pb.start();
			} catch (Exception e) {
				Utils.logToConsole("Running chrome failed, currently only works on Windows. " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	void writeHtml() throws Exception {
		synchronized (this) {
			String thisFileName = "not yet specified";
			int nDataPoints = 0;
			try {
				deleteOldData();
				String fileName = Utils.getLogFileName(accountMonitor.accountName, "allNNN.html");
				thisFileName = fileName.replace("NNN", String.format("%03d", nextFileNumber));
				String dir = new File(fileName).getParent();
				File tempFile = File.createTempFile("tmp", ".html", new File(dir));
				FileWriter htmlStream = new FileWriter(tempFile.getCanonicalPath(), false);
				try {
					appendFromFileToFile(htmlStream, "src/main/resources/prefix.html", getNextFileNumber());
					nDataPoints = writeJson(htmlStream);
					appendFromFileToFile(htmlStream, "src/main/resources/suffix.html", "junk");
				} finally {
					htmlStream.close();
				}
				File thisFile = new File(thisFileName);
				try {
					thisFile.delete();
				} catch (Exception ex) {
					Utils.logToConsole(
							"thisFile.delete() : " + ex.getMessage() + " " + ex.getClass().getName() + ", continuing");
				}
				Files.move(tempFile.toPath(), thisFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
				Utils.logToConsole(
						"Wrote " + thisFileName + " : # data points : " + new Integer(nDataPoints).toString());
// TODO :				startChromeIfNecessary(thisFileName);
			} catch (Exception e) {
				Utils.logToConsole("FAILED to write : " + thisFileName + " : # data points : "
						+ new Integer(nDataPoints).toString());
				// If there's any failure, continue and write the next file at the appropriate time.
			}
		}
	}
	
	public void run() {
		Utils.logToConsole("HtmlFileDataWriter thread starting.");
		try {
			while (true) {
				Thread.sleep(Main.runParams.htmlWriteIntervalInSeconds * 1000);
				writeHtml();
			}
		} catch (Exception e) {
			Utils.logToConsole(e.getMessage());
			e.printStackTrace();
		}
	}
}
