package com.ckkeith.monitor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

public class HtmlFileDataWriter extends Thread {

	private AccountMonitor accountMonitor;
	
	private ConcurrentSkipListMap<LocalDateTime, ConcurrentSkipListMap<String, String>> sensorData = new ConcurrentSkipListMap<LocalDateTime, ConcurrentSkipListMap<String, String>>();
	private ConcurrentSkipListMap<String, String> sensorNames = new ConcurrentSkipListMap<String, String>();
	private int dataIntervalInMinutes = 10; // TODO : add to RunParams
	private int htmlWriteIntervalInSeconds = 5; // TODO : add to RunParams

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
									new Integer(htmlWriteIntervalInSeconds).toString());
				}
				writeln(htmlStream, line);
			}
		}
	}

	int nextFileNumber = 0;
	final static int MAX_FILE_NUMBER = 1;

	String getNextFileNumber() {
		nextFileNumber++;
		if (nextFileNumber > MAX_FILE_NUMBER) {
			nextFileNumber = 0;
		}
		return String.format("%03d", nextFileNumber);
	}

	void deleteOldData() {
		LocalDateTime start = LocalDateTime.now().minusMinutes(dataIntervalInMinutes);
		Set<LocalDateTime> keys = sensorData.keySet();
		Iterator<LocalDateTime> itr = keys.iterator();
		while (itr.hasNext()) {
			LocalDateTime timestamp = itr.next();
			if (timestamp.isBefore(start)) {
				sensorData.remove(timestamp);
			}
		}
	}
	
	void writeHtml() throws Exception {
		synchronized (this) {
			deleteOldData();
			String fileName = Utils.getLogFileName(accountMonitor.accountName, "allNNN.html");
			String dir = new File(fileName).getParent();
			File tempFile = File.createTempFile("tmp", ".html", new File(dir));
			FileWriter htmlStream = new FileWriter(tempFile.getCanonicalPath(), false);
			int thisFileNumber = nextFileNumber;
			int nDataPoints = 0;
			try {
				appendFromFileToFile(htmlStream, "src/main/resources/prefix.html", getNextFileNumber());
				nDataPoints = writeJson(htmlStream);
				appendFromFileToFile(htmlStream, "src/main/resources/suffix.html", "junk");
			} finally {
				htmlStream.close();
			}
			String thisFileName = fileName.replace("NNN", String.format("%03d", thisFileNumber));
			File thisFile = new File(thisFileName);
			try {
				try {
					thisFile.delete();
				} catch (Exception ex) {
					Utils.logToConsole(
							"thisFile.delete() : " + ex.getMessage() + " " + ex.getClass().getName() + ", continuing");
				}
				Files.move(tempFile.toPath(), thisFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
				Utils.logToConsole(
						"Wrote " + thisFileName + " : # data points : " + new Integer(nDataPoints).toString());
			} catch (Exception e) {
				Utils.logToConsole("FAILED to write : " + thisFileName + " : # data points : "
						+ new Integer(nDataPoints).toString());
				throw e;
			}
		}
	}
	
	public void run() {
		Utils.logToConsole("HtmlFileDataWriter thread starting.");
		try {
			while (true) {
				Thread.sleep(htmlWriteIntervalInSeconds * 1000);
				writeHtml();
			}
		} catch (Exception e) {
			Utils.logToConsole(e.getMessage());
			e.printStackTrace();
		}
	}
}
