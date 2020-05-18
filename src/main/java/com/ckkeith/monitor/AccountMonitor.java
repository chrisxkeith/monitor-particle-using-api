// Please credit chris.keith@gmail.com
package com.ckkeith.monitor;

import java.io.File;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AccountMonitor extends Thread {

	String accessToken = null;
	String accountName = null;
	private String logFileName;
	private Map<String, ParticleDeviceEvent> eventSubscribers = new HashMap<String, ParticleDeviceEvent>();
	private List<GoogleSheetsWriter> googleSheetsWriters = new ArrayList<GoogleSheetsWriter>();
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
			throw new Exception("No account name specified.");
		}
		logFileName = Utils.getLogFileName(accountName, "devices-overview.txt");
		String paramFileName = getParamFilePath();
		Utils.logToConsole(accountName + ": loading params from: " + paramFileName);
		runParams = RunParams.loadFromXML(paramFileName);
		Utils.logToConsole(accountName + ": " + runParams.toString());
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

	private String getParamFilePath() throws Exception {
		return Utils.getHomeDir() + File.separator + "Documents" + File.separator + "tmp" + File.separator
				+ Utils.getHostName() + File.separator + Utils.getSafeName(accountName) + File.separator
				+ "runparams.xml";
	}

	private void startSheetsWriter() {
		for (Map.Entry<String, RunParams.SheetConfig> entry : this.runParams.sheets.entrySet()) {
			GoogleSheetsWriter googleSheetsWriter = new GoogleSheetsWriter(this, entry);
			googleSheetsWriter.initSheets();
			new PivotDataApp(this).loadSheetsWriter(googleSheetsWriter);
			googleSheetsWriter.start();
			googleSheetsWriters.add(googleSheetsWriter);
		}
	}

	void startDeviceMonitors() {
		startSheetsWriter();
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
			for (GoogleSheetsWriter googleSheetsWriter : googleSheetsWriters) {
				Iterator<RunParams.Dataset> datasetIt = googleSheetsWriter.entry.getValue().dataSets.iterator();
				while (datasetIt.hasNext()) {
					RunParams.Dataset d = datasetIt.next();
					for (Map.Entry<String, HashSet<String>> mc : d.microcontrollers.entrySet()) {
						if (mc.getKey().equals(deviceName)) {
							googleSheetsWriter.addData(new EventData(ldt, deviceName, event, data));
						}
					}
				}
			}
		}
	}

	// TODO: Add this to XML configuration file.
	public String getMappedSensorName(String photonSensorName) {
		Map<String, String> sensorNameMap = new HashMap<String, String>();
		sensorNameMap.put("photon-07 Faire 7 IR heat sensor", "Office");
		sensorNameMap.put("photon-08 Stove heat sensor", "Stove");
		sensorNameMap.put("photon-10 Outdoor Thermistor sensor 10", "Back Porch");

		// SheetsWriter doesn't use photon name.
		sensorNameMap.put("Faire 7 IR heat sensor", "Office");
		sensorNameMap.put("Stove heat sensor", "Stove");
		sensorNameMap.put("Outdoor Thermistor sensor 10", "Back Porch");
		if (sensorNameMap.get(photonSensorName) != null) {
			return sensorNameMap.get(photonSensorName);
		}
		return photonSensorName;
	}
	

}
