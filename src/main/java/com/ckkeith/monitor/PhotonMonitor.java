package com.ckkeith.monitor;

import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import nl.infcomtec.jparticle.Cloud;
import nl.infcomtec.jparticle.Device;

public class PhotonMonitor extends Thread {

	private String accessToken = null;
	private String accountName = null;
	private String logFileName;

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
		logFileName = Utils.getLogFileName(accountName, "devices-overview.txt");
	}

	private void writeTable(StringBuilder sb, String[] headers, Integer[] columns, ArrayList<String> bodyLines) throws Exception {
		sb.append("<table border=\"1\">");
		sb.append("<tr>");
		for (int i = 0; i < headers.length; i++) {
			sb.append("<th style=\"text-align:left\">").append(headers[i]).append("</th>");
		}
		sb.append("</tr>");
		for (String s : bodyLines) {
			sb.append("<tr>");
			String[] f = s.split("\t");
			for (int i = 0; i < columns.length; i++) {
				sb.append("<td style=\"text-align:left\">").append(f[columns[i]]).append("</td>");
			}
			sb.append("</tr>");
		}
		sb.append("</table>");
	}

	private void sendEmail(ArrayList<String> bodyLines) throws Exception {
		StringBuilder sb = new StringBuilder("<!DOCTYPE HTML><html><body>");
		String[] headers = {"photon name", "connected", "lastheard"};
		Integer[] columns = {1, 3, 4};
		writeTable(sb, headers, columns, bodyLines);
		sb.append("</body></html>");
		String subject = accountName + " : Particle device status from " + Utils.getHostName();
		GMailer.sendMessageX("chris.keith@gmail.com", "chris.keith@gmail.com", subject, sb.toString());
	}

	public void run() {
		try {
			Utils.logToConsole(Utils.padWithSpaces(this.accountName, 20) + "\tPhotonMonitor thread starting.");
			Map<String, DeviceMonitor> deviceMonitors = new HashMap<String, DeviceMonitor>();
			while (true) {
				Cloud c = new Cloud("Bearer " + accessToken, true, false);
				ArrayList<String> statuses = new ArrayList<String>();
				ArrayList<DeviceMonitor> newDevices = new ArrayList<DeviceMonitor>();
				for (Device device : c.devices.values()) {
					if (device.connected) {
						try {
							// Get device variables and functions
							device = Device.getDevice(device.id, "Bearer " + accessToken);
							DeviceMonitor dm = new DeviceMonitor(accountName, accessToken, device, c);
							Utils.logWithGSheetsDate(LocalDateTime.now(), dm.toTabbedString(), logFileName);
							statuses.add(dm.toTabbedString());
							if (device.connected && (deviceMonitors.get(device.name) == null)) {
								deviceMonitors.put(device.name, dm);
								newDevices.add(dm);
							}
						} catch (Exception e) {
							String err = "run() :\t" + device.name + "\t" + e.getClass().getName() + "\t" + e.getMessage();
							Utils.logToConsole(err);
							statuses.add(err);
							e.printStackTrace(new PrintStream(System.out));
						}
					}
				}
				sendEmail(statuses);
				for (DeviceMonitor dm : newDevices) {
					dm.start();
				}
				// At midnight (local time), check for changes in devices-per-cloud and their statuses.
				LocalDateTime then = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS).plusDays(1);
				Utils.sleepUntil("PhotonMonitor\t" + accountName, then);
			}
		} catch (Exception e) {
			Utils.logToConsole("run() :\t" + e.getClass().getName() + "\t" + e.getMessage());
			e.printStackTrace(new PrintStream(System.out));
		}
		Utils.logToConsole(Utils.padWithSpaces(this.accountName, 20) + "\tPhotonMonitor thread exiting.");
	}
}
