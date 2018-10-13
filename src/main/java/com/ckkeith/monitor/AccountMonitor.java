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
	private HtmlFileDataWriter htmlFileDataWriter;

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
				for (DeviceMonitor dm : newDevices) {
					dm.start();
				}
				if (htmlFileDataWriter == null && accountName.equals("chris.keith@gmail.com")) {
					htmlFileDataWriter = new HtmlFileDataWriter(this);
					htmlFileDataWriter.start();
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
		String headers[] = {"Name", "PublishedAt", "Event"};
		Integer columns[] = {0, 1, 2};
		ArrayList<String> lines = new ArrayList<String>(eventSubscribers.size());
		for (Entry<String, ParticleDeviceEvent> e : eventSubscribers.entrySet()) {
			String s = new String(e.getKey());
			s.concat("\t").concat(e.getValue().getMostRecentEventDateTime())
			 .concat("\t").concat(e.getValue().getMostRecentEvent());
			lines.add(s);
		}
		Utils.writeTable(sb, headers, columns, lines);
		sb.append("</body></html>");
		String subject = accountName + " : Particle device most recent entries from " + Utils.getHostName();
		GMailer.sendMessageX("chris.keith@gmail.com", "chris.keith@gmail.com", subject, sb.toString());
	}

	public void addEventSubscriber(String name, ParticleDeviceEvent cb) {
		synchronized(eventSubscribers) {
			eventSubscribers.put(name, cb);
		}
	}

	public void addDataPoint(LocalDateTime ldt, String event, String data) {
		if ((htmlFileDataWriter != null) && event.contains("ensor")) {
			this.htmlFileDataWriter.addData(new SensorDataPoint(ldt, event, data));
		}
		synchronized(eventCount) {
			eventCount++;
		}
	}
}
