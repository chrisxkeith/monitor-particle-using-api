// Please credit chris.keith@gmail.com
package com.ckkeith.monitor;

import java.io.File;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class AccountMonitor extends Thread {

	String accessToken = null;
	String accountName = null;
	private String logFileName;
	private Map<String, ParticleDeviceEvent> eventSubscribers = new HashMap<String, ParticleDeviceEvent>();
	private HtmlFileDataWriter htmlFileDataWriter;
	RunParams runParams;
	Map<String, DeviceMonitor> deviceMonitors = new HashMap<String, DeviceMonitor>();
	Set<String> deviceNames = new HashSet<String>();

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
			Utils.logToConsole(this.accessToken);
			throw new Exception("No account name specified.");
		}
		logFileName = Utils.getLogFileName(accountName, "devices-overview.txt");
		loadParams();
		for (Map.Entry<String, RunParams.SheetConfig> entry : runParams.sheets.entrySet()) {
			for (RunParams.Dataset ds : entry.getValue().dataSets) {
				for (String deviceName : ds.microcontrollers.keySet()) {
					deviceNames.add(deviceName);
				}
			}
		}
		if (deviceNames.size() == 0) {
			Utils.logToConsole("No deviceNames for " + accountName);
			System.exit(-1);
		}
	}

	private void loadParamsFromFile() throws Exception {
		String paramFileName = getParamFilePath();
		Utils.logToConsole(accountName + ": loading params from: " + paramFileName);
		runParams = RunParams.loadFromXML(paramFileName);
	}

	private void loadParams() throws Exception {
		File f = new File(this.getParamCfgFilePath());
		if (f.exists()) {
			Scanner scanner = new Scanner(f);
			String id = "unknown";
			try {
				id = scanner.nextLine();
				Utils.logToConsole(accountName + ": loading params from Google Sheet: " + id);
				runParams = RunParams.loadFromXMLString(GoogleSheetsReader.readData(id, "Sheet1", "A1:A250"));
			} catch (Throwable t) {
				Utils.logToConsole("Error loading params from Google Sheet: " + id);
				Utils.logToConsole(t.toString());
				try {
					loadParamsFromFile();
				} catch (Throwable t1) {
					Utils.logToConsole("Error loading params from file: " + f.getAbsolutePath());
					System.exit(-1);
				}
			} finally {
				scanner.close();
			}
		} else {
			loadParamsFromFile();
		}
		Utils.logToConsole(accountName + ": " + runParams.toString());
	}

	private String getParamFilePath(String fn) throws Exception {
		return Utils.getHomeDir() + File.separator + "Documents" + File.separator + "tmp" + File.separator
				+ Utils.getHostName() + File.separator + Utils.getSafeName(accountName) + File.separator
				+ fn;
	}

	private String getParamFilePath() throws Exception {
		return getParamFilePath("runparams.xml");
	}

	private String getParamCfgFilePath() throws Exception {
		return getParamFilePath("runparams.id");
	}

	void startHtmlWriter() {
		if (htmlFileDataWriter == null && runParams.htmlWriteIntervalInSeconds > 0) {
			htmlFileDataWriter = new HtmlFileDataWriter(this);
			htmlFileDataWriter.start();
		}
	}
	void startDeviceMonitors() {
		startHtmlWriter();
		ParticleCloud c = new ParticleCloud("Bearer " + accessToken, true, false);
		ArrayList<DeviceMonitor> newDevices = new ArrayList<DeviceMonitor>();
		for (ParticleDevice device : c.getDevices()) {
			try {
				if (!device.getConnected()) {
					if (!Utils.isDebug) {
						Utils.logToConsole("Skipping disconnected device : " + device.getName());
					}
				} else {
					// Get device variables and functions
					device = device.getDevice("Bearer " + accessToken);
					DeviceMonitor dm = new DeviceMonitor(this, device, c);
					Utils.logWithGSheetsDate(LocalDateTime.now(), dm.toTabbedString(), logFileName);
					if (deviceNames.contains(device.getName()) && deviceMonitors.get(device.getName()) == null) {
						deviceMonitors.put(device.getName(), dm);
						newDevices.add(dm);
					}
					// Server returned HTTP response code: 502 for URL: https://api.particle.io/v1/devices/4b0050001151373331333230
					if (Utils.isDebug) {
						Thread.sleep(3 * 1000);
					} else {
						LocalDateTime then = LocalDateTime.now().plusSeconds(3);
						Utils.sleepUntil(
							"AccountMonitor.startDeviceMonitors() sleeping to try to avoid \"Too many requests\" (http 502) error for: "
									+ device.getName(),
							then);
						}
				}
			} catch (Exception e) {
				String err = "run() :\t" + device.getName() + "\t" + e.getClass().getName() + "\t" + e.getMessage();
				Utils.logToConsole(err);
				e.printStackTrace(new PrintStream(System.out));
			}
		}
		if (newDevices.size() == 0) {
			Utils.logToConsole("Didn't find any devices for : " + accountName);
			System.exit(-6);
		}
		for (DeviceMonitor dm : newDevices) {
			dm.start();
		}
	}

	public void run() {
		Utils.logToConsole(Utils.padWithSpaces(this.accountName, 20) +
				"\tAccountMonitor thread starting : " + Utils.getCurrentThreadString());
		Utils.logWithGSheetsDate(LocalDateTime.now(), "AccountMonitor thread starting.", logFileName, ",");
		if (runParams.writeLongTermData) {
			Utils.logToConsole("PivotDataApp.writeLongTermData() needs to be rewritten.");
			// (new PivotDataApp(this)).writeLongTermData();
		} else {
			startDeviceMonitors();
		}
		Utils.logToConsole(Utils.padWithSpaces(this.accountName, 20) +
				"\tAccountMonitor thread exiting : " + Utils.getCurrentThreadString());
	}

	public void addEventSubscriber(String name, ParticleDeviceEvent cb) {
		synchronized(eventSubscribers) {
			eventSubscribers.put(name, cb);
		}
	}

	public void addDataPoint(LocalDateTime ldt, String deviceName, String event, String data) {
		if (event.contains("ensor") || event.contains("ontroller")) {
			if ((htmlFileDataWriter != null)) {
				this.htmlFileDataWriter.addData(new SensorDataPoint(ldt, deviceName, event, data));
			}
		}
	}

	public boolean handleServerEvent(String event) throws Exception {
		return true;
	}
}
