// Please credit chris.keith@gmail.com
package com.ckkeith.monitor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class PivotDataApp {

	class SensorData {
		SensorData(LocalDateTime localDateTime, String deviceName, String sensorName, String sensorValue) {
			this.localDateTime = localDateTime;
			this.deviceName = deviceName;
			this.sensorName = sensorName;
			this.sensorValue = sensorValue;
		}
		LocalDateTime	localDateTime;
		String			deviceName;
		String			sensorName;
		String			sensorValue;
	}

	final private DateTimeFormatter googleSheetsDateFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
	final static String expectedInputData =
			"logTimestamp<tab>GSheetsTimestamp<tab>sensorName<tab>sensorValue<tab>photonName";
	/*
	 * Output: file containing:
	 * <space><tab>sensor1Name<tab>sensor2Name...
	 * timestamp<tab>sensor1Value<tab>sensor2Value...
	 * ...
	 */
	int totalInputLines = 0;
	int linesSkipped = 0;
	int totalCsvLinesOutput = 0;
	int linesReadForSensorData = 0;
	AccountMonitor accountMonitor;
	
	private void writeCsv(String fileName, ConcurrentSkipListMap<String, String> firstSensorValues,
			ConcurrentSkipListMap<LocalDateTime, ConcurrentSkipListMap<String, String>> outputRows) throws Exception {
		Set<String> sensorNames = firstSensorValues.keySet();
		StringBuilder sb = new StringBuilder(" ");
		Iterator<String> sensorIt = sensorNames.iterator();
		while (sensorIt.hasNext()) {
			sb.append("\t").append(sensorIt.next());
		}
		String			sensorNameString = sb.toString();
		FileWriter		csvStream = new FileWriter(fileName, false);
		try {
			csvStream.write(sensorNameString + System.getProperty("line.separator"));
			Set<LocalDateTime> keys = outputRows.keySet();
			Iterator<LocalDateTime> itr = keys.iterator();
			while (itr.hasNext()) {
				LocalDateTime timestamp = itr.next();
					sb = new StringBuilder(googleSheetsDateFormat.format(timestamp));
					ConcurrentSkipListMap<String, String> entries = outputRows.get(timestamp);
	
					sensorIt = sensorNames.iterator();
					while (sensorIt.hasNext()) {
						String sensorName = sensorIt.next();
						String val = entries.get(sensorName);
						if (val == null) {
							val = "";
						}
						entries.put(sensorName, val);
						sb.append("\t").append(val);
					}
					csvStream.write(sb.append(System.getProperty("line.separator")).toString());
					totalCsvLinesOutput++;
			}
		} finally {
			if (csvStream != null) {
				csvStream.close();
			}
		}
	}

	private void addEvent(ConcurrentSkipListMap<LocalDateTime, ConcurrentSkipListMap<String, String>> outputRows,
						LocalDateTime ldt,
						String sensorName,
						String sensorValue) {
		ConcurrentSkipListMap<String, String> sensorValues = outputRows.get(ldt);
		if (sensorValues == null) {
			sensorValues = new ConcurrentSkipListMap<String, String>();
		}
		sensorValues.put(sensorName, sensorValue);
		outputRows.put(ldt, sensorValues);
	}

	private void createGapEvents(ConcurrentSkipListMap<String, String> firstSensorValues,
			ConcurrentSkipListMap<LocalDateTime, ConcurrentSkipListMap<String, String>> outputRows,
			ConcurrentSkipListMap<LocalDateTime, ConcurrentSkipListMap<String, String>> inputRows) throws Exception {
		String sensorName = "gap";
		firstSensorValues.put(sensorName, "3");
		LocalDateTime lastSampleTime = null;
		Set<LocalDateTime> keys = inputRows.keySet();
		Iterator<LocalDateTime> itr = keys.iterator();
		while (itr.hasNext()) {
			LocalDateTime timestamp = setTimeGranularity(itr.next());
			if (lastSampleTime == null) {
				addEvent(outputRows, timestamp, sensorName, "3");
			} else {
				Integer lastMinute = lastSampleTime.toLocalTime().toSecondOfDay() / 60;
				Integer thisMinute = timestamp.toLocalTime().toSecondOfDay() / 60;
				Integer gap = thisMinute - lastMinute;
				if (gap > accountMonitor.runParams.gapTriggerInMinutes) {
					addEvent(outputRows, lastSampleTime.minusSeconds(1), sensorName, "3");
					addEvent(outputRows, lastSampleTime, sensorName, "0");
					addEvent(outputRows, timestamp.minusSeconds(1), sensorName, "0");
					addEvent(outputRows, timestamp, sensorName, "3");
				}
			}
			lastSampleTime = timestamp;
		}
	}

	final private DateTimeFormatter logDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX");

	private String checkInputData(String s) {
		String[] vals = s.split("\t");

		// Need at least log line timestamp, Google sheets timestamp and event name.
		if (vals.length < 3 || vals[0].isEmpty() || vals[1].isEmpty() || vals[2].isEmpty()) {
			return "Not enough input data";
		}
		if (!(vals[2].contains("ensor") || vals[2].contains("ontroller"))) {
			return "unknown event : " + vals[2];
		}
		return null;
	}

	private SensorData mungeValues(String vals[]) throws Exception {
		try {
			if (vals.length < 4) {
				String newVals[] = new String[4];
				newVals[0] = vals[0];
				newVals[1] = vals[1];
				if (vals.length == 3) {
					newVals[2] = vals[2];
				} else {
					newVals[2] = "unknown event";
				}
				newVals[3] = new Integer(Integer.MIN_VALUE).toString();
				vals = newVals;
			}
			if (vals[3].startsWith("|")) {
				String c[] = vals[3].split("\\|");
				ZonedDateTime zdt = ZonedDateTime.parse(c[1], DateTimeFormatter.ISO_ZONED_DATE_TIME);
				ZoneId caZone = ZoneId.of("America/Los_Angeles");
				ZonedDateTime caZoned = zdt.withZoneSameInstant(caZone);
				vals[1] = googleSheetsDateFormat.format(caZoned.toLocalDateTime());
				vals[3] = c[2];
			}
			LocalDateTime.parse(vals[1], googleSheetsDateFormat);
		} catch (Exception e) {
			// Try to use log date for event date.
			LocalDateTime ldt = LocalDateTime.parse(vals[0], logDateFormat);
			String newVals[] = new String[vals.length + 1];
			if (vals.length > 2) {
				newVals[3] = vals[2];
			}
			newVals[2] = vals[1];
			newVals[1] = googleSheetsDateFormat.format(ldt);
			newVals[0] = vals[0];
			vals = newVals;
		}

		if (vals.length < 4) {
			// Extend array.
			String newVals[] = new String[4];
			newVals[0] = vals[0];
			newVals[1] = vals[1];
			newVals[2] = vals[2];
			vals = newVals;
		}
		if (vals[3] == null) {
			vals[3] = "";
		} else {
			if (vals[3].equalsIgnoreCase("on")) {
				vals[3] = "1";
			} else if (vals[3].equalsIgnoreCase("off")) {
				vals[3] = "0";
			} else {
				try {
					Integer.parseInt(vals[3]);
				} catch (Exception e) {
					try {
						Double.parseDouble(vals[3]);
					} catch (Exception e2){
						vals[3] = "";
					}
				}
			}
		}
		return new SensorData(LocalDateTime.parse(vals[1], googleSheetsDateFormat), vals[0], vals[2], vals[3]);
	}

	private void readSensorNames(String fn, ConcurrentSkipListMap<String, String> firstSensorValues,
			ConcurrentSkipListMap<LocalDateTime, ConcurrentSkipListMap<String, String>> outputRows) throws Exception {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(fn));
			String s;
			while ((s = br.readLine()) != null) {
				totalInputLines++;
				String err = checkInputData(s);
				if (err == null) {
					SensorData sensorData = getVals(s);
					firstSensorValues.put(sensorData.sensorName, sensorData.sensorValue);
				} else {
					linesSkipped++;
				}
			}
		} catch (Exception e) {
			if (br != null) {
				br.close();
			}
			Utils.logToConsole("readSensorNames() : " + e.getMessage());
			e.printStackTrace();
		}
	}

	private SensorData getVals(String s) throws Exception {
		String vals[] = s.split("\t");
		String deviceName = "unknown device";
		if (vals.length > 4) {
			deviceName = vals[4];
		}
		SensorData sensorData = mungeValues(vals);
		setTimeGranularity(sensorData);
		sensorData.sensorName = deviceName + " " + sensorData.sensorName;
		return sensorData;
	}

	private LocalDateTime setTimeGranularity(LocalDateTime ldt) {
		int seconds = ldt.toLocalTime().toSecondOfDay();
		int roundedSeconds = ((seconds + (accountMonitor.runParams.csvTimeGranularityInSeconds / 2))
								/ accountMonitor.runParams.csvTimeGranularityInSeconds)
								* accountMonitor.runParams.csvTimeGranularityInSeconds;
		int dayFactor;
		if (roundedSeconds < 86400) {
			dayFactor = 0;
		} else {  // we rounded up to the next day
			roundedSeconds = 0;
			dayFactor = 1;
		}
		LocalTime lt = LocalTime.ofSecondOfDay(roundedSeconds);
		return LocalDateTime.of(ldt.toLocalDate(), lt).plusDays(dayFactor);
	}

	private void setTimeGranularity(SensorData sensorData) {
		sensorData.localDateTime = setTimeGranularity(sensorData.localDateTime);
	}

	private void readSensorValues(String fn, ConcurrentSkipListMap<String, String> firstSensorValues,
			ConcurrentSkipListMap<LocalDateTime, ConcurrentSkipListMap<String, String>> outputRows) throws Exception {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(fn));
			String s;
			while ((s = br.readLine()) != null) {
				String err = checkInputData(s);
				if (err == null) {
					linesReadForSensorData++;
					SensorData sensorData = getVals(s);
					ConcurrentSkipListMap<String, String> sensorValues = outputRows.get(sensorData.localDateTime);
					if (sensorValues == null) {
						sensorValues = new ConcurrentSkipListMap<String, String>();
					}
					sensorValues.put(sensorData.sensorName, sensorData.sensorValue);
					outputRows.put(sensorData.localDateTime, sensorValues);
				}
			}
		} catch (Exception e) {
			if (br != null) {
				br.close();
			}
			Utils.logToConsole("readSensorValues() : " + e.getMessage());
			e.printStackTrace();
		}
	}

	private void readData(String fn, ConcurrentSkipListMap<String, String> firstSensorValues,
			ConcurrentSkipListMap<LocalDateTime, ConcurrentSkipListMap<String, String>> outputRows) throws Exception {
		File f = new File(fn);
		if (!f.exists()) {
			System.out.println("No inputFile file : " + f.getCanonicalPath());
			System.exit(-7);
		}
		readSensorNames(fn, firstSensorValues, outputRows);
		readSensorValues(fn, firstSensorValues, outputRows);
	}

	void processFile(String fn, ConcurrentSkipListMap<String, String> firstSensorValues,
			ConcurrentSkipListMap<LocalDateTime, ConcurrentSkipListMap<String, String>> outputRows) throws Exception {
		totalInputLines = 0;
		linesSkipped = 0;
		totalCsvLinesOutput = 0;
		linesReadForSensorData = 0;

		readData(fn, firstSensorValues, outputRows);
		writeCsv(fn.replace(".txt", ".csv"), firstSensorValues, outputRows);
		processMasterLog(outputRows);
		Utils.logToConsole(Utils.padWithSpaces(fn.replace(".txt", ".csv"), 100)
				+ "\t" + totalCsvLinesOutput);
	}

	private void processPath(Path p) {
		try {
			ConcurrentSkipListMap<LocalDateTime, ConcurrentSkipListMap<String, String>> outputRows =
					new ConcurrentSkipListMap<LocalDateTime, ConcurrentSkipListMap<String, String>>();
			ConcurrentSkipListMap<String, String> firstSensorValues =
					new ConcurrentSkipListMap<String, String>();

			processFile(p.toFile().getCanonicalPath(), firstSensorValues,
					outputRows);
		} catch (Exception e) {
			System.out.println("processPath() : " + e.toString());
			e.printStackTrace();
		}
	}

	static Boolean checkPath(Path p) {
		return p.toString().endsWith("_particle_log.txt");
	}

	private void processMasterLog(
			ConcurrentSkipListMap<LocalDateTime, ConcurrentSkipListMap<String, String>> inputRows) throws Exception {
		ConcurrentSkipListMap<String, String> firstSensorValues =
			new ConcurrentSkipListMap<String, String>();
		ConcurrentSkipListMap<LocalDateTime, ConcurrentSkipListMap<String, String>> outputRows =
			new ConcurrentSkipListMap<LocalDateTime, ConcurrentSkipListMap<String, String>>();

		accountMonitor.runParams.csvTimeGranularityInSeconds = 10 * 60;
		createGapEvents(firstSensorValues, outputRows, inputRows);
		firstSensorValues.put("exception", "0");
		firstSensorValues.put("startup", "0");

		String fullPath = Utils.getMasterLogFileDir() + File.separator + "monitor.log";
		BufferedReader	br = new BufferedReader(new FileReader(fullPath));
		try {
			String s;
			while ((s = br.readLine()) != null) {
				String[] vals = s.split("\t");
				if (vals.length > 1) {
					if (s.contains("xception") || s.contains("Running from")) {
						LocalDateTime ldt = setTimeGranularity(LocalDateTime.parse(vals[0], logDateFormat));
						if (s.contains("xception")) {
							addEvent(outputRows, ldt, "exception", "1");
						} else {
							addEvent(outputRows, ldt, "startup", "2");
						}
					}
				}
			}
			writeCsv(fullPath + ".tsv", firstSensorValues, outputRows);
		} finally {
			if (br != null) {
				br.close();
			}
		}
	}

	public void writeLongTermData() {
		Utils.logToConsole(accountMonitor.accountName + "PivotDataApp.writeLongTermData() started.");
		try {
			Predicate<Path> isParticleFile = i -> (checkPath(i));
			Consumer<Path> processPath = i -> processPath(i);
			String logFileDir = Utils.getLogFileDir(accountMonitor.accountName);
			Files.walk(Paths.get(logFileDir)).
						filter(isParticleFile).forEach(processPath);
		} catch (Exception e) {
			System.out.println("writeLongTermData() : " + e.toString());
			e.printStackTrace();
		}
		Utils.logToConsole(accountMonitor.accountName + "PivotDataApp.writeLongTermData() finished.");
	}

	public PivotDataApp(AccountMonitor accountMonitor) {
		this.accountMonitor = accountMonitor;
	}
}
