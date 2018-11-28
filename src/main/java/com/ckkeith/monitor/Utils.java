// Please credit chris.keith@gmail.com
package com.ckkeith.monitor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.SystemUtils;

public class Utils {
	public static String getHomeDir() throws Exception {
		String d = System.getProperty("user.home");
		if (d == null || d.isEmpty()) {
			d = System.getProperty("HOMEDRIVE") + System.getProperty("HOMEPATH");
		}
		if (d == null || d.isEmpty()) {
			throw new Exception("Unable to determine home directory from environment variables.");
		}
		return d;
	}

	public static String getHomeURLPath(String accountPath) throws Exception {
		String d = getHomeDir() + File.separator + accountPath;
		if (d.charAt(1) == ':') {
			d = d.replaceAll("\\\\", "/");
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
			FileWriter fstream = new FileWriter(logFileName, true);
			fstream.write(s + System.getProperty("line.separator"));
			fstream.flush();
			fstream.close();
		} catch (Exception e) {
			System.out.println(s + "\tError writing log file : " + logFileName + "\t" + e.toString());
			e.printStackTrace(new PrintStream(System.out));
		}
	}

	public static String nowInLogFormat() {
		return logDateFormat.format(new java.util.Date());
	}

	public static void logToConsole(String s) {
		String d = logDateFormat.format(new java.util.Date());
		System.out.println(d + "\t" + s + "\t");
	}

	public static void log(String s, String logFileName) {
		String d = logDateFormat.format(new java.util.Date());
		System.out.println(d + "\t" + s);
		logRaw(d + "\t" + s, logFileName);
	}

	final static public DateTimeFormatter googleSheetsDateFormat =
			DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
	public static String logWithGSheetsDate(LocalDateTime ldt, String s, String logFileName) {
		String d = logDateFormat.format(new java.util.Date());
		String d2 = googleSheetsDateFormat.format(ldt);
		String logString = d + "\t" + d2 + "\t" + s;
		logRaw(logString, logFileName);
		return logString;
	}

	static String getSafeName(String s) {
		String safeName = s.replaceAll("\\W+", "-");
		if (safeName.length() > 24) {
			safeName = safeName.substring(0, 11)
					+ "--"
					+ safeName.substring(safeName.length() - 12);
		}
		return safeName.toLowerCase();
	}

	private static void mkdir(String path) {
		File dir = new File(path);
		if (!dir.exists()) {
			try {
				dir.mkdir();
			} catch (Exception e) {
				System.out.println("Unable to create directory : " + path);
				throw e;
			}
		}
	}

	public static String getLogFileName(String accountName, String fn) throws Exception {
		String d = Utils.getHomeDir();
		String acctId = getSafeName(accountName);
		String machineName = getSafeName(getHostName());
		String path = d + File.separator + "Documents" + File.separator
				+ "tmp" + File.separator + machineName;
		mkdir(path);	// looks like mkdir will only create a directory at one level.
		path += File.separator + acctId;
		mkdir(path);
		// Append to existing log file to get better long term data.
		return path + File.separator + fn;
	}

	public static void sleepUntil(String msg, LocalDateTime then) throws Exception {
		Utils.logToConsole(msg + "\tAbout to sleep until\t" + then);
		Thread.sleep(ChronoUnit.MILLIS.between(LocalDateTime.now(), then));
		Utils.logToConsole(msg + "\tFinished sleeping, started at\t" + then);
	}

	public static ArrayList<String> readParameterFile(String parameterFilePath) throws Exception {
		File f = new File(parameterFilePath);
		if (!f.exists()) {
			System.out.println("No parameter file : " + parameterFilePath);
			System.exit(-7);
		}
		return readTextFileIntoArray(parameterFilePath);
	}

	public static ArrayList<String> readTextFileIntoArray(String parameterFilePath) throws Exception {
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

	public static String getHostName() {
		String serverName = System.getenv("COMPUTERNAME");
		if (serverName == null || serverName.isEmpty()) {
			serverName = System.getenv("HOSTNAME");
		}
		if (serverName == null || serverName.isEmpty()) {
			try {
				serverName = InetAddress.getLocalHost().getHostName();
			} catch (UnknownHostException e) {
				System.out.println("Error on InetAddress.getLocalHost().getHostName()\t" + e.toString());
				e.printStackTrace(new PrintStream(System.out));
			}
		}
		if (serverName == null || serverName.isEmpty()) {
			serverName = "unknown-host";
		}
		return serverName;
	}

	public static List<String> runCommandForOutput(List<String> params) {
	    ProcessBuilder pb = new ProcessBuilder(params);
	    Process p;
	    ArrayList<String> result = new ArrayList<String>();
	    try {
	        p = pb.start();
	        final BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
	        reader.lines().iterator().forEachRemaining(result::add);
	        p.waitFor();
	        p.destroy();
	    } catch (Exception e) {
	        e.printStackTrace();
	        result.add(e.getMessage());
	    }
	    return result;
	}

	public static LocalDateTime getBootTime() {
		if (SystemUtils.IS_OS_WINDOWS) {
			ArrayList<String> cmd = new ArrayList<String>();
			cmd.add("systeminfo");
			List<String> output = runCommandForOutput(cmd);
			for (String s : output) {
				if (s.contains("System Boot Time:")) {
					s = s.replace("System Boot Time:", "");
					s = s.trim();
					// 11/14/2018, 11:48:32 AM
					s = s.replace(", ", "T");
					try {
						SimpleDateFormat f = new SimpleDateFormat("MM'/'dd'/'yyyy'T'hh:mm:ss' 'a");
						Date d = f.parse(s);
						return LocalDateTime.ofInstant(d.toInstant(), ZoneId.systemDefault());
					} catch(Exception e) {
						Utils.logToConsole("Unable to parse datetime: " + s);
						return null;
					}
				}
			}
		}
		return null;
	}

	private static void writeTableHeader(StringBuilder sb, String[] headers) {
		sb.append("<table border=\"1\">");
		sb.append("<tr>");
		for (int i = 0; i < headers.length; i++) {
			sb.append("<th style=\"text-align:left\">").append(headers[i]).append("</th>");
		}
		sb.append("</tr>");
	}

	public static void writeTable(StringBuilder sb, String[] headers, Integer[] columns, ArrayList<String> bodyLines) {
		writeTableHeader(sb, headers);
		for (String s : bodyLines) {
			sb.append("<tr>");
			String[] f = s.split("\t");
			for (int i = 0; i < columns.length; i++) {
				String str = "Bad data in line : '" + s + "'";
				try {
					str = f[columns[i]];
				} catch (Throwable t) {
					// show error, don't throw.
				}
				sb.append("<td style=\"text-align:left\">").append(str).append("</td>");
			}
			sb.append("</tr>");
		}
		sb.append("</table>");
	}
}
