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

	private final String otherMachineName = "2018-ck-nuc";

	private void writeCsv(String fileName, ConcurrentSkipListMap<String, String> firstSensorValues,
			ConcurrentSkipListMap<LocalDateTime, ConcurrentSkipListMap<String, String>> outputRows) throws Exception {
		Set<String> sensorNames = firstSensorValues.keySet();
		StringBuilder sb = new StringBuilder(" ");
		Iterator<String> sensorIt = sensorNames.iterator();
		while (sensorIt.hasNext()) {
			sb.append("\t").append(sensorIt.next());
		}
		LocalDateTime	lastSampleTime = null;
		String			sensorNameString = sb.toString();
		FileWriter		csvStream = new FileWriter(fileName, false);
		try {
			csvStream.write(sensorNameString + System.getProperty("line.separator"));
			Set<LocalDateTime> keys = outputRows.keySet();
			Iterator<LocalDateTime> itr = keys.iterator();
			while (itr.hasNext()) {
				LocalDateTime timestamp = itr.next();
				if (lastSampleTime != null) {
					Integer lastSecond = lastSampleTime.toLocalTime().toSecondOfDay();
					Integer thisSecond = timestamp.toLocalTime().toSecondOfDay();
					while (thisSecond - lastSecond > accountMonitor.runParams.csvTimeGranularityInSeconds) {
						sb = new StringBuilder(googleSheetsDateFormat.format(timestamp));
						csvStream.write(sb.append(System.getProperty("line.separator")).toString());
						totalCsvLinesOutput++;
						lastSecond += accountMonitor.runParams.csvTimeGranularityInSeconds;
					}
				}
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
				lastSampleTime = timestamp;
			}
		} finally {
			if (csvStream != null) {
				csvStream.close();
			}
		}
	}

	private void addLogLine(FileWriter tsvStream, LocalDateTime ldt, String s) throws Exception {
		String logLine = (googleSheetsDateFormat.format(ldt) +"\t" + s + System.getProperty("line.separator"));
		tsvStream.write(logLine);
	}

	private int createGapEvents(FileWriter tsvStream,
			ConcurrentSkipListMap<LocalDateTime, ConcurrentSkipListMap<String, String>> inputRows) throws Exception {
		int dayToStart = LocalDateTime.now().getDayOfYear() - accountMonitor.runParams.daysOfGapData;
		int yearToStart = LocalDateTime.now().getYear();
		if (dayToStart < 1) {
			yearToStart--;
			dayToStart += 365;
		}
		int outputLines = 0;
		LocalDateTime lastSampleTime = null;
		Set<LocalDateTime> keys = inputRows.keySet();
		Iterator<LocalDateTime> itr = keys.iterator();
		while (itr.hasNext()) {
			LocalDateTime timestamp = setTimeGranularity(itr.next());
			if ((timestamp.getYear() < yearToStart) || (timestamp.getDayOfYear() < dayToStart)) {
				continue;
			}
			if (lastSampleTime != null) {
				Integer lastMinute = lastSampleTime.toLocalTime().toSecondOfDay() / 60;
				int lastDay = lastSampleTime.getDayOfYear();
				while (lastDay < timestamp.getDayOfYear()) {
					lastMinute -= 24 * 60;
					lastDay++;
				}
				Integer thisMinute = timestamp.toLocalTime().toSecondOfDay() / 60;
				Integer gap = thisMinute - lastMinute;
				if (gap > accountMonitor.runParams.gapTriggerInMinutes) {
					addLogLine(tsvStream, lastSampleTime, "Gap started");
					addLogLine(tsvStream, timestamp, "Gap ended");
					outputLines += 2;
				}
			}
			lastSampleTime = timestamp;
		}
		return outputLines;
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
			Utils.logToConsole("readSensorValues() : " + e.getMessage());
			e.printStackTrace();
		} finally {
			if (br != null) {
				br.close();
			}
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
		Utils.logToConsole(Utils.padWithSpaces(fn.replace(".txt", ".csv"), 100)
				+ "\t" + totalCsvLinesOutput);
		processMasterLog(outputRows);
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

	@SuppressWarnings("unused")
	private int createExceptionEvents(FileWriter tsvStream, String fullPath) throws Exception {
		int outputLines = 0;
		BufferedReader	br = new BufferedReader(new FileReader(fullPath));
		try {
			int dayToStart = LocalDateTime.now().getDayOfYear() - accountMonitor.runParams.daysOfGapData;
			int yearToStart = LocalDateTime.now().getYear();
			if (dayToStart < 1) {
				yearToStart--;
				dayToStart += 365;
			}
			String previousTime = null;
			String s;
			while ((s = br.readLine()) != null) {
				String[] vals = s.split("\t");
				if (vals.length > 1) {
					try {
						String thisTime = vals[0];
						LocalDateTime timestamp = LocalDateTime.parse(thisTime, logDateFormat);
						if ((timestamp.getYear() < yearToStart) || (timestamp.getDayOfYear() < dayToStart)) {
							continue;
						}
						if (s.contains("xception") && !thisTime.equals(previousTime)) {
							addLogLine(tsvStream, timestamp, s);
							outputLines++;
						}
						previousTime = thisTime;
					} catch (Exception e) {
						// just go on to find a log line starting with a correctly formatted timestamp.
					}
				}
			}
		} finally {
			if (br != null) {
				br.close();
			}
		}
		return outputLines;
	}

	private void processMasterLog(ConcurrentSkipListMap<LocalDateTime, ConcurrentSkipListMap<String, String>> outputRows) throws Exception {
		String		fullPath = getMasterLogFilePath();
		String		outputFile = fullPath.replace("monitor.log", "exeptions-and-gaps.tsv");
		FileWriter	tsvStream = new FileWriter(outputFile, false);

		try {
			Integer	outputLines = createGapEvents(tsvStream, outputRows);
			// outputLines += this.createExceptionEvents(tsvStream, fullPath);
			Utils.logToConsole(Utils.padWithSpaces(outputFile + "\t" + outputLines.toString(), 100));
		} finally {
			if (tsvStream != null) {
				tsvStream.close();
			}
		}
	}

	private String getLogFileDir() throws Exception {
		String logFileDir = Utils.getLogFileDir(accountMonitor.accountName).toLowerCase();
		return logFileDir.replace(Utils.getHostName().toLowerCase(), otherMachineName.toLowerCase());
	}

	private String getMasterLogFilePath() throws Exception {
		String fullPath = Utils.getMasterLogFileDir() + File.separator + "monitor.log";
		return fullPath.replace(Utils.getHostName().toLowerCase(), otherMachineName.toLowerCase());
	}

	public void writeLongTermData() {
		Utils.logToConsole(accountMonitor.accountName + "PivotDataApp.writeLongTermData() started.");
		try {
			Predicate<Path> isParticleFile = i -> (checkPath(i));
			Consumer<Path> processPath = i -> processPath(i);
			String logFileDir = getLogFileDir();
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
