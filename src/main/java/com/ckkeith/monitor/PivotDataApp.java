// Please credit chris.keith@gmail.com
package com.ckkeith.monitor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
import org.apache.commons.io.input.ReversedLinesFileReader;

public class PivotDataApp extends Thread {
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
	String directory;
	AccountMonitor accountMonitor;
	
	class ReverseReader {
		ReversedLinesFileReader fr;
		String					filePath;
		LocalDateTime			startTime;
		String					previousLine;

		@SuppressWarnings("deprecation")
		ReverseReader(String filePath, LocalDateTime startTime) throws Exception {
			this.filePath = filePath;
			this.startTime = startTime;
			this.previousLine = "no previous line";
			fr = new ReversedLinesFileReader(new File(filePath));
		}

		private String readLine() throws Exception {
			while (true) {
				String ch = fr.readLine();
				if (ch == null) {
					fr.close();
					Utils.logToConsole("Reached start of file: " + filePath + ". previous line: " + previousLine);
					return null;
				}
				String[] chunks = ch.split("\t");
				if (chunks.length == 0) {
					continue;
				}
				LocalDateTime ldt;
				try {
					ldt = LocalDateTime.parse(chunks[0], logDateFormat);
				} catch(Exception e ) {
					if (Utils.isDebug) {
						Utils.logToConsole("Does not start with a timestamp : " + ch);
					}
					continue;
				}
				if (startTime == null || startTime.isBefore(ldt)) {
					previousLine = ch;
					return ch;
				}
				String ts = ldt.format(Utils.googleSheetsDateFormat);
				String lim = "no startTime";
				if (startTime != null) {
					lim = startTime.format(Utils.googleSheetsDateFormat);
				}
				Utils.logToConsole("Read lines from " + filePath + " back up to " + ts +
						". startTime: " + lim + ". previous line: " + previousLine);
				return null;
			}
		}

		void close() {
			if (fr != null) {
				try {
					fr.close();
				} catch (IOException e) {
					Utils.logToConsole("close() : catch : " + e.getMessage());
					e.printStackTrace();
				}
			}
		}
	}

	private void writeCsv(String fileName, ConcurrentSkipListMap<String, String> firstSensorValues,
			ConcurrentSkipListMap<String, ConcurrentSkipListMap<String, String>> outputRows) throws Exception {
		Set<String> sensorNames = firstSensorValues.keySet();
		StringBuilder sb = new StringBuilder(" ");
		Iterator<String> sensorIt = sensorNames.iterator();
		while (sensorIt.hasNext()) {
			sb.append("\t").append(sensorIt.next());
		}
		FileWriter csvStream = new FileWriter(fileName, false);
		try {
			String sensorNameString = sb.toString();
			csvStream.write(sensorNameString + System.getProperty("line.separator"));

			Set<String> keys = outputRows.keySet();
			Iterator<String> itr = keys.iterator();
			while (itr.hasNext()) {
				String timestamp = itr.next();
				sb = new StringBuilder(timestamp);
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
			csvStream.close();
		}
	}

	private void writeData(String fileName, ConcurrentSkipListMap<String, String> firstSensorValues,
			ConcurrentSkipListMap<String, ConcurrentSkipListMap<String, String>> outputRows) throws Exception {
		writeCsv(fileName, firstSensorValues, outputRows);
	}

	final private DateTimeFormatter googleSheetsDateFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
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

	private String[] mungeValues(String vals[]) throws Exception {
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
		return vals;
	}

	LocalDateTime getTimeLimit() {
		return LocalDateTime.now().minusDays(14); // two weeks.
	}

	private void readSensorNames(String fn, ConcurrentSkipListMap<String, String> firstSensorValues,
			ConcurrentSkipListMap<String, ConcurrentSkipListMap<String, String>> outputRows) throws Exception {
		ReverseReader br = null;
		try {
			br = new ReverseReader(fn, getTimeLimit());
			String s;
			while ((s = br.readLine()) != null) {
				totalInputLines++;
				String err = checkInputData(s);
				if (err == null) {
					String[] vals = getVals(s);
					if (accountMonitor.runParams.devicesToReport.length() == 0 || accountMonitor.runParams.devicesToReport.contains(vals[2])) {
						firstSensorValues.put(vals[2], vals[3]);
					}
				} else {
					linesSkipped++;
				}
			}
		} catch (Exception e) {
			Utils.logToConsole("readSensorNames() : " + e.getMessage());
			e.printStackTrace();
		}
	}

	private String[] getVals(String s) throws Exception {
		String vals[] = s.split("\t");
		String deviceName = "unknown device";
		if (vals.length > 4) {
			deviceName = vals[4];
		}
		String newVals[] = setTimeGranularity(mungeValues(vals));
		newVals[2] = deviceName + " " + newVals[2];
		return newVals;
	}

	private String[] setTimeGranularity(String[] vals) {
		LocalDateTime ldt = LocalDateTime.parse(vals[1], googleSheetsDateFormat);
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
		ldt = LocalDateTime.of(ldt.toLocalDate(), lt).plusDays(dayFactor);
		vals[1] = ldt.format(googleSheetsDateFormat);
		return vals;
	}

	private void readSensorValues(String fn, ConcurrentSkipListMap<String, String> firstSensorValues,
			ConcurrentSkipListMap<String, ConcurrentSkipListMap<String, String>> outputRows) throws Exception {
		ReverseReader br = null;
		try {
			br = new ReverseReader(fn, getTimeLimit());
			String currentDay = "junk";
			String s;
			while ((s = br.readLine()) != null) {
				String err = checkInputData(s);
				if (err == null) {
					linesReadForSensorData++;
					String[] vals = getVals(s);
					String thisDay = vals[1].substring(0, 11);
					if (Utils.isDebug) {
						if (!thisDay.equals(currentDay)) {
							currentDay = thisDay;
							System.out.println("Starting to process\t" + currentDay);
						}
					}
					if (accountMonitor.runParams.devicesToReport.length() == 0 || accountMonitor.runParams.devicesToReport.contains(vals[2])) {
						ConcurrentSkipListMap<String, String> sensorValues = outputRows.get(vals[1]);
						if (sensorValues == null) {
							sensorValues = new ConcurrentSkipListMap<String, String>();
						}
						sensorValues.put(vals[2], vals[3]);
						outputRows.put(vals[1], sensorValues);
					}
				}
			}
		} catch (Exception e) {
			Utils.logToConsole("readSensorValues() : " + e.getMessage());
			e.printStackTrace();
		}
	}

	private void readData(String fn, ConcurrentSkipListMap<String, String> firstSensorValues,
			ConcurrentSkipListMap<String, ConcurrentSkipListMap<String, String>> outputRows) throws Exception {
		File f = new File(fn);
		if (!f.exists()) {
			System.out.println("No inputFile file : " + f.getCanonicalPath());
			System.exit(-7);
		}
		readSensorNames(fn, firstSensorValues, outputRows);
		readSensorValues(fn, firstSensorValues, outputRows);
	}

	String getTimeLimits(ConcurrentSkipListMap<String, ConcurrentSkipListMap<String, String>> outputRows) {
		String firstTimeStamp = "none";
		String lastTimeStamp = "none";
		Set<String> keys = outputRows.keySet();
		Iterator<String> itr = keys.iterator();
		while (itr.hasNext()) {
			lastTimeStamp = itr.next();
			if (firstTimeStamp.equals("none")) {
				firstTimeStamp = lastTimeStamp;
			}
		}
		return firstTimeStamp + "\t" + lastTimeStamp;
	}

	String processFile(String fn, ConcurrentSkipListMap<String, String> firstSensorValues,
			ConcurrentSkipListMap<String, ConcurrentSkipListMap<String, String>> outputRows) throws Exception {
		totalInputLines = 0;
		linesSkipped = 0;
		totalCsvLinesOutput = 0;
		linesReadForSensorData = 0;

		readData(fn, firstSensorValues, outputRows);
		String timeLimits = getTimeLimits(outputRows);
		if (timeLimits.contains("none")) {
			return null;
		}
		writeData(fn.replace(".txt", ".csv"), firstSensorValues, outputRows);
		return Utils.padWithSpaces(fn.replace(".txt", ".csv"), 100)
				+ "\t" + timeLimits + "\t" + totalCsvLinesOutput;
	}

	private void processPath(Path p) {
		try {
			ConcurrentSkipListMap<String, ConcurrentSkipListMap<String, String>> outputRows =
					new ConcurrentSkipListMap<String, ConcurrentSkipListMap<String, String>>();
			ConcurrentSkipListMap<String, String> firstSensorValues =
					new ConcurrentSkipListMap<String, String>();

			String summary = processFile(p.toFile().getCanonicalPath(), firstSensorValues,
					outputRows);
			if (summary != null) {
				System.out.println(summary);
			}
		} catch (Exception e) {
			System.out.println("processPath() : " + e.toString());
			e.printStackTrace();
		}
	}

	static Boolean checkPath(Path p) {
		return p.toString().endsWith("_particle_log.txt");
	}

	void processDirectory() {
		synchronized(this) {
			String titles = Utils.padWithSpaces("photon", 100) + "\t" + Utils.padWithSpaces("first", 23) + "\t"
					+ Utils.padWithSpaces("last", 23) + "\tdatapoints";
			System.out.println(titles);
			try {
				Predicate<Path> isParticleFile = i -> (checkPath(i));
				Consumer<Path> processPath = i -> processPath(i);
				Files.walk(Paths.get(directory)).filter(isParticleFile).forEach(processPath);
			} catch (Exception e) {
				System.out.println("processDirectory() : " + e.toString());
				e.printStackTrace();
			}
		}
	}

	public void run() {
		Utils.logToConsole(Utils.padWithSpaces(accountMonitor.accountName, 20) + "\tPivotDataApp thread starting.");
		while (true) {
			try {
//				processDirectory();
				Thread.sleep(60 * 60 * 1000); // one hour
			} catch (InterruptedException e) {
				Utils.logToConsole(Utils.padWithSpaces(accountMonitor.accountName, 20) + "\tFailure in PivotDataApp.run().");
				e.printStackTrace();
			}
		}
	}

	public PivotDataApp(AccountMonitor accountMonitor) {
		this.accountMonitor = accountMonitor;
	}
}
