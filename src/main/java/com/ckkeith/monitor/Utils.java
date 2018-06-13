package com.ckkeith.monitor;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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

	public static String getLogFileName(String fn) throws Exception {
		String d = Utils.getHomeDir();
		String path = d + File.separator + "Documents" + File.separator + "tmp";
		File dir = new File(path);
		if (!dir.exists()) {
			throw new IllegalArgumentException("No such directory for log files : " + path);
		}
		// Append to existing log file to get better long term data.
		return path + File.separator + fn;
	}

}
