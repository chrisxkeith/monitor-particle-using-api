package com.ckkeith.monitor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

import nl.infcomtec.jparticle.Device;

public class Utils {
	public static String getHomeDir() throws Exception {
		String d = System.getProperty("user.home");
		if (d == null || d.length() == 0) {
			throw new Exception("Unable to determine user.home directory from System.getProperty(\"user.home\")");
		}
		return d;
	}

	public static final boolean isDebug = java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments()
			.toString().indexOf("jdwp") >= 0;

	private static final SimpleDateFormat logDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");

	public static String padWithSpaces(String s, int length) {
		StringBuffer ret = new StringBuffer(s);
		int nSpaces = length - s.length();
		while (nSpaces > 0) {
			ret.append(" ");
			nSpaces--;
		}
		return ret.toString();
	}

	private static void logRaw(String s, String logFileName) {
		try {
			System.out.println(s);
			FileWriter fstream = new FileWriter(logFileName, true);
			fstream.write(s + System.getProperty("line.separator"));
			fstream.flush();
			fstream.close();
		} catch (Exception e) {
			System.out.println(s + "\tError writing log file : " + logFileName + "\t" + e.toString());
			e.printStackTrace(new PrintStream(System.out));
		}
	}

	public static void logToConsole(String s) {
		String d = logDateFormat.format(new java.util.Date());
		System.out.println(d + "\t" + s);
	}

	public static void log(String s, String logFileName) {
		String d = logDateFormat.format(new java.util.Date());
		logRaw(d + "\t" + s, logFileName);
	}

	final static private DateTimeFormatter googleSheetsDateFormat =
			DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
	public static void logWithGSheetsDate(LocalDateTime ldt, String s, String logFileName) {
		String d = logDateFormat.format(new java.util.Date());
		String d2 = googleSheetsDateFormat.format(ldt);
		logRaw(d + "\t" + d2 + "\t" + s, logFileName);
	}

	public static String getLogFileName(String accountName, String fn) throws Exception {
		String d = Utils.getHomeDir();
		String safeName = accountName.replaceAll("\\W+", "-");
		String acctId;
		if (safeName.length() > 24) {
			acctId = safeName.substring(0, 11)
					+ "--"
					+ safeName.substring(safeName.length() - 12);
		} else {
			acctId = safeName;
		}
		String path = d + File.separator + "Documents" + File.separator
				+ "tmp" + File.separator + acctId.toLowerCase();
		File dir = new File(path);
		if (!dir.exists()) {
			throw new IllegalArgumentException("No such directory for log files : " + path);
		}
		// Append to existing log file to get better long term data.
		return path + File.separator + fn;
	}

	public static void sleepUntil(String msg, LocalDateTime then) throws Exception {
		Utils.logToConsole(msg + "\tAbout to sleep until\t" + then);
		Thread.sleep(ChronoUnit.MILLIS.between(LocalDateTime.now(), then));
	}

	public static String getVariable(String accessToken, Device d, String variableName) {
		if (d.variables == null) {
			return "unknown (no variables)";
		}
		if (d.variables.has(variableName)) {
			String ret = d.readString(variableName, "Bearer " + accessToken);
			if (ret == null || ret.isEmpty()) {
				return "unknown (null or empty value)";
			}
			return ret;
		}
		return "unknown (no " + variableName + ")";
	}

	public static ArrayList<String> readParameterFile(String fileName) throws Exception {
		String parameterFilePath = getHomeDir() + File.separator + "Documents" + File.separator + fileName;
		File f = new File(parameterFilePath);
		if (!f.exists()) {
			System.out.println("No parameter file : " + parameterFilePath);
			System.exit(-7);
		}
		ArrayList<String> creds = new ArrayList<String>(10);
		BufferedReader br = new BufferedReader(new FileReader(parameterFilePath));
		try {
			String s;
			while ((s = br.readLine()) != null) {
				creds.add(s);
			}
		} finally {
			br.close();
		}
		return creds;
	}

}
