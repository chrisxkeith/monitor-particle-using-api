package com.ckkeith.monitor;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.logging.Logger;

import nl.infcomtec.jparticle.Cloud;
import nl.infcomtec.jparticle.Device;

public class PhotonMonitor extends Thread {

	private static final Logger logger = Logger.getLogger(PhotonMonitor.class.getName());

	private static final SimpleDateFormat logDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
	private String logFileName;

	private String padWithSpaces(String s, int length) {
		StringBuffer ret = new StringBuffer(s);
		int nSpaces = length - s.length();
		while (nSpaces > 0) {
			ret.append(" ");
			nSpaces--;
		}
		return ret.toString();
	}

	private void logRaw(String s) {
		logger.info(s);
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

	private void log(String s) {
		String d = logDateFormat.format(new java.util.Date());
		logRaw(d + "\t" + s);
	}

	private String getLogFileName() throws Exception {
		String d = Utils.getHomeDir();
		String path = d + File.separator + "Documents" + File.separator + "tmp";
		File dir = new File(path);
		if (!dir.exists()) {
			throw new IllegalArgumentException("No such directory for log files : " + path);
		}
		// Append to existing log file to get better long term data.
		return path + File.separator + this.accountName + "_particle_log.txt";
	}

	private String accessToken = null;
	private String accountName = null;

	public PhotonMonitor(String credentials) throws Exception {
		String[] creds = credentials.split("\t");
		if (creds.length > 0) {
			this.accessToken = creds[0];
		} else {
			throw new Exception("No particle token specified.");
		}
		if (creds.length > 1) {
			this.accountName = creds[1];
		} else {
			this.accountName = "unknown_account";
		}
	}

	private String toTabbed(Device d) {
		StringBuffer sb = new StringBuffer();
		sb.append(padWithSpaces(d.name, 20)).append("\t");
		sb.append(d.id).append("\t");
		sb.append(d.connected).append("\t");
		sb.append(d.lastHeard);
		return sb.toString();
	}

	public void run() {
		try {
			logFileName = getLogFileName();
			logger.info("Logging to " + logFileName);
			log(this.accountName + " : PhotonMonitor thread starting up.");
			Cloud c = new Cloud("Bearer " + accessToken, true, false);
			log(padWithSpaces("name", 20) + "\t" + padWithSpaces("id", 24) + "\t" + "connect" + "\t"
					+ padWithSpaces("lastHeard", 28));
			for (Map.Entry<String, Device> entry : c.devices.entrySet()) {
				Device d = entry.getValue();
				log(toTabbed(d));
			}
		} catch (Exception e) {
			System.out.println(
					"run() : " + LocalDateTime.now().toString() + "\t" + e.getClass().getName() + " " + e.getMessage());
			e.printStackTrace(new PrintStream(System.out));
		}
	}
}
