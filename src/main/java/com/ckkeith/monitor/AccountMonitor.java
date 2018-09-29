package com.ckkeith.monitor;

import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import nl.infcomtec.jparticle.Cloud;
import nl.infcomtec.jparticle.Device;

public class AccountMonitor extends Thread {

	String accessToken = null;
	String accountName = null;
	Integer eventCount = 0;
	private String logFileName;
	private Map<String, ParticleDeviceEvent> eventSubscribers = new HashMap<String, ParticleDeviceEvent>();

	public AccountMonitor(String credentials) throws Exception {
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

	private void sendEmail(ArrayList<String> bodyLines, String msg) throws Exception {
		StringBuilder sb = new StringBuilder("<!DOCTYPE HTML><html><body>");
		String[] headers = {"photon name", "connected", "lastheard"};
		Integer[] columns = {1, 3, 4};
		writeTable(sb, headers, columns, bodyLines);
		sb.append(msg);
		sb.append("</body></html>");
		String subject = accountName + " : Particle device status from " + Utils.getHostName();
		GMailer.sendMessageX("chris.keith@gmail.com", "chris.keith@gmail.com", subject, sb.toString());
	}

	public void run() {
		Utils.logToConsole(Utils.padWithSpaces(this.accountName, 20) + "\tPhotonMonitor thread starting.");
		while (true) {
			Map<String, DeviceMonitor> deviceMonitors = new HashMap<String, DeviceMonitor>();
			try {
				Utils.logToConsole(Utils.padWithSpaces(this.accountName, 20) + "\tPhotonMonitor checking devices.");
				Cloud c = new Cloud("Bearer " + accessToken, true, false);
				ArrayList<String> statuses = new ArrayList<String>();
				ArrayList<DeviceMonitor> newDevices = new ArrayList<DeviceMonitor>();
				for (Device device : c.devices.values()) {
					try {
						// Get device variables and functions
						device = Device.getDevice(device.id, "Bearer " + accessToken);
						DeviceMonitor dm = new DeviceMonitor(this, device, c);
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
				sendEmail(statuses, "AccountMontor.eventCount : " + eventCount);
				for (DeviceMonitor dm : newDevices) {
					dm.start();
				}
				// At 1 a.m. (local time), check for changes in devices-per-account.
				LocalDateTime then = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS).plusDays(1).withHour(1);
				Utils.sleepUntil("PhotonMonitor\t" + accountName, then);
				Utils.logToConsole("AccountMontor.eventCount : " + eventCount);
			} catch (Exception e) {
				Utils.logToConsole("run() :\t" + e.getClass().getName() + "\t" + e.getMessage());
				e.printStackTrace(new PrintStream(System.out));
				// ... and keep thread going ...
			}
		}
	}

	public void emailMostRecentEvents() {
		StringBuilder sb = new StringBuilder("<!DOCTYPE HTML><html><body>");
		sb.append("<table border=\"1\">");
		sb.append("<tr>");
		sb.append("<th style=\"text-align:left\">").append("Name").append("</th>");
		sb.append("<th style=\"text-align:left\">").append("PublishedAt").append("</th>");
		sb.append("<th style=\"text-align:left\">").append("Event").append("</th>");
		sb.append("</tr>");
		for (Entry<String, ParticleDeviceEvent> e : eventSubscribers.entrySet()) {
			sb.append("<tr>");
			sb.append("<td>").append(e.getKey()).append("</td>");
			sb.append("<td>").append(e.getValue().getMostRecentEventDateTime()).append("</td>");
			sb.append("<td>").append(e.getValue().getMostRecentEvent()).append("</td>");
			sb.append("</tr>");
		}
		sb.append("</table>");
		sb.append("</body></html>");
		String subject = accountName + " : Particle device most recent entries from " + Utils.getHostName();
		GMailer.sendMessageX("chris.keith@gmail.com", "chris.keith@gmail.com", subject, sb.toString());
	}

	public void addEventSubscriber(String name, ParticleDeviceEvent cb) {
		synchronized(eventSubscribers) {
			eventSubscribers.put(name, cb);
		}
	}

	public void incrementEventCount() {
		synchronized(eventCount) {
			eventCount++;
		}
	}
}
