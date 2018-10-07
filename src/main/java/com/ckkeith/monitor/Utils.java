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
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import nl.infcomtec.jparticle.Device;

public class Utils {
	public static String getHomeDir() throws Exception {
		String d = System.getProperty("user.home");
		if (d == null || d.isEmpty()) {
			d = System.getProperty("HOMEDRIVE") + System.getProperty("HOMEPATH");
		}
		if (d == null || d.isEmpty()) {
			throw new Exception("Unable to determine home directory from System.getProperty(\"user.home\")");
		}
		return d;
	}

	public static String getHomeURLPath() throws Exception {
		String d = getHomeDir();
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
		System.out.println(d + "\t" + s + "\t" + getHostName());
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

	public static String getParamFileDirectory() throws Exception {
		return getHomeDir() + File.separator + "Documents" + File.separator;
	}

	public static ArrayList<String> readParameterFile(String fileName) throws Exception {
		String parameterFilePath =  getParamFileDirectory() + fileName;
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

	public static String runCommandForOutput(List<String> params) {
	    ProcessBuilder pb = new ProcessBuilder(params);
	    Process p;
	    String result = "";
	    try {
	        p = pb.start();
	        final BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

	        StringJoiner sj = new StringJoiner(System.getProperty("line.separator"));
	        reader.lines().iterator().forEachRemaining(sj::add);
	        result = sj.toString();

	        p.waitFor();
	        p.destroy();
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    return result;
	}
}
