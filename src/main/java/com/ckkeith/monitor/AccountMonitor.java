package com.ckkeith.monitor;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintStream;
import java.time.LocalDateTime;
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

	void startDeviceMonitors(Map<String, DeviceMonitor> deviceMonitors) {
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
				// Server returned HTTP response code: 502 for URL: https://api.particle.io/v1/devices/4b0050001151373331333230
				LocalDateTime then = LocalDateTime.now().plusSeconds(2);
				Utils.sleepUntil("AccountMonitor.startDeviceMonitors() sleeping to try to avoid \"Too many requests\" (http 502) error", then);
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
//			htmlFileDataWriter = new HtmlFileDataWriter(this);
//			htmlFileDataWriter.start();
		}

	}

	public void executeRestartScript() throws Exception {
		// Write restart script to disk and execute it.
		// This will (I hope) kludge around the mysterious data drop issue.
		String parentDirName = (new File(logFileName)).getParent();
		String scriptName = parentDirName + File.separator + "restart.cmd";
		String cd = "cd " + Utils.getHomeDir()
				+ File.separator + "Documents"
				+ File.separator + "Github"
				+ File.separator + "monitor-particle-using-api";
		String mvn = "mvn clean install exec:java -D maven.test.skip=true "
				+ "-D exec.mainClass=\"com.ckkeith.monitor.Main\" 2>&1 >> "
				+ Utils.getHomeDir()
				+ File.separator + "Documents"
				+ File.separator + "tmp"
				+ File.separator + "monitor.log";
		FileWriter fstream = new FileWriter(scriptName, false);
		fstream.write(cd + System.getProperty("line.separator"));
		fstream.write(mvn + System.getProperty("line.separator"));
		fstream.flush();
		fstream.close();
		String exec = "cmd /c " + scriptName;
		Utils.log("exec()ing : " + exec, logFileName);
		Runtime.getRuntime().exec(exec);
		Thread.sleep(10 * 1000);
		Utils.log("About to System.exit(0)", logFileName);
		System.exit(0);
	}

	public void run() {
		Utils.logToConsole(Utils.padWithSpaces(this.accountName, 20) + "\tPhotonMonitor thread starting.");
		Map<String, DeviceMonitor> deviceMonitors = new HashMap<String, DeviceMonitor>();
		startDeviceMonitors(deviceMonitors);
		while (true) {
			try {
				int previousEventCount = eventCount;
				LocalDateTime then = LocalDateTime.now().plusSeconds(Main.runParams.expectedEventRateInSeconds);
				Utils.sleepUntil("AccountMonitor sleeping until event count check", then);
				if (previousEventCount == eventCount) {
					// Got no new events in the expected interval.
					executeRestartScript();
				} else if (previousEventCount == 0) {
					emailMostRecentEvents();
				}
			} catch (Exception e) {
				Utils.logToConsole("run() :\t" + e.getClass().getName() + "\t" + e.getMessage());
				e.printStackTrace(new PrintStream(System.out));
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

	public void addDataPoint(LocalDateTime ldt, String event, String data) {
		if ((htmlFileDataWriter != null) && event.contains("ensor")) {
			this.htmlFileDataWriter.addData(new SensorDataPoint(ldt, event, data));
		}
		synchronized(eventCount) {
			eventCount++;
		}
	}
}
