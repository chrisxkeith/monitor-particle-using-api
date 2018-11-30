// Please credit chris.keith@gmail.com
package com.ckkeith.monitor;

import java.io.File;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class AccountMonitor extends Thread {

	String accessToken = null;
	String accountName = null;
	Integer eventCount = 0;
	private String logFileName;
	private Map<String, ParticleDeviceEvent> eventSubscribers = new HashMap<String, ParticleDeviceEvent>();
	private HtmlFileDataWriter htmlFileDataWriter;
	RunParams runParams;
	Map<String, DeviceMonitor> deviceMonitors = new HashMap<String, DeviceMonitor>();

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
			throw new Exception("No account name specified.");
		}
		logFileName = Utils.getLogFileName(accountName, "devices-overview.txt");
		runParams = RunParams.load(getParamFilePath());
		Utils.logToConsole(accountName + ": " + runParams.toString());
	}

	private String getParamFilePath() throws Exception {
		return Utils.getHomeDir() + File.separator + "Documents" + File.separator + "tmp" + File.separator
				+ Utils.getHostName() + File.separator + Utils.getSafeName(accountName) + File.separator
				+ "runparams.txt";
	}

	void startDeviceMonitors() {
		ParticleCloud c = new ParticleCloud("Bearer " + accessToken, true, false);
		ArrayList<DeviceMonitor> newDevices = new ArrayList<DeviceMonitor>();
		for (ParticleDevice device : c.getDevices()) {
			try {
				if (!device.getConnected()) {
					Utils.logToConsole("Skipping disconnected device : " + device.getName());
				} else {
					// Get device variables and functions
					device = device.getDevice("Bearer " + accessToken);
					DeviceMonitor dm = new DeviceMonitor(this, device, c);
					Utils.logWithGSheetsDate(LocalDateTime.now(), dm.toTabbedString(), logFileName);
					if (deviceMonitors.get(device.getName()) == null) {
						deviceMonitors.put(device.getName(), dm);
						newDevices.add(dm);
					}
					// Server returned HTTP response code: 502 for URL: https://api.particle.io/v1/devices/4b0050001151373331333230
					LocalDateTime then = LocalDateTime.now().plusSeconds(3);
					Utils.sleepUntil(
							"AccountMonitor.startDeviceMonitors() sleeping to try to avoid \"Too many requests\" (http 502) error for: "
									+ device.getName(),
							then);
				}
			} catch (Exception e) {
				String err = "run() :\t" + device.getName() + "\t" + e.getClass().getName() + "\t" + e.getMessage();
				Utils.logToConsole(err);
				e.printStackTrace(new PrintStream(System.out));
			}
		}
		for (DeviceMonitor dm : newDevices) {
			dm.start();
		}
		if (htmlFileDataWriter == null) {
			htmlFileDataWriter = new HtmlFileDataWriter(this);
			htmlFileDataWriter.start();
		}
	}

	public void run() {
		Utils.logToConsole(Utils.padWithSpaces(this.accountName, 20) + "\tPhotonMonitor thread starting.");
		startDeviceMonitors();
		PivotDataApp	pivotDataApp = new PivotDataApp();
		while (true) {
			try {
				LocalDateTime then = LocalDateTime.now().plusHours(1);
				if (Utils.isDebug) {
					then = LocalDateTime.now().plusSeconds(15);
				}
				Utils.sleepUntil("AccountMonitor sleeping until next cvs file generation.", then);
//				pivotDataApp.run(Utils.getLogFileDir(accountName), runParams);
			} catch (Exception e) {
				Utils.logToConsole("run() :\t" + e.getClass().getName() + "\t" + e.getMessage());
				e.printStackTrace();
				// ... and keep thread going ...
			}
		}
	}

	public void emailMostRecentEvents() {
		StringBuilder sb = new StringBuilder("<!DOCTYPE HTML><html><body>");
		String headers[] = {"Device Name", "PublishedAt", "Event Name", "Event Data"};
		Integer columns[] = {0, 1, 2, 3}; // event is returned as tabbed string.
		ArrayList<String> lines = new ArrayList<String>(eventSubscribers.size());
		for (Entry<String, ParticleDeviceEvent> e : eventSubscribers.entrySet()) {
			StringBuilder s = new StringBuilder(e.getKey());
			s.append("\t").append(e.getValue().getMostRecentEventDateTime())
			 .append("\t").append(e.getValue().getMostRecentEvent());
			lines.add(s.toString());
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

	public void addDataPoint(LocalDateTime ldt, String deviceName, String event, String data) {
		if ((htmlFileDataWriter != null) && event.contains("ensor")) {
			this.htmlFileDataWriter.addData(new SensorDataPoint(ldt, deviceName, event, data));
		}
		synchronized(eventCount) {
			eventCount++;
		}
	}
}
