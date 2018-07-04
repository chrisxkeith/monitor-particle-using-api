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
		logFileName = Utils.getLogFileName(accountName + "-devices-overview.txt");
	}

	public void run() {
		try {
			Utils.logToConsole(Utils.padWithSpaces(this.accountName, 20) + "\tPhotonMonitor thread starting.");
			Cloud c = new Cloud("Bearer " + accessToken, true, false);
			Map<String, DeviceMonitor> deviceMonitors = new HashMap<String, DeviceMonitor>();
			while (true) {
				ArrayList<DeviceMonitor> newDevices = new ArrayList<DeviceMonitor>();
				for (Device device : c.devices.values()) {
					if (device.connected) {
						try {
							// Get device variables and functions
							device = Device.getDevice(device.id, "Bearer " + accessToken);
							DeviceMonitor dm = new DeviceMonitor(accessToken, device, c);
							Utils.logWithGSheetsDate(LocalDateTime.now(), dm.toTabbedString(), logFileName);
							if (device.connected && (deviceMonitors.get(device.name) == null)) {
								deviceMonitors.put(device.name, dm);
								newDevices.add(dm);
							}
						} catch (Exception e) {
							Utils.logToConsole("run() :\t" + device.name + "\t" + e.getClass().getName() + "\t" + e.getMessage());
							e.printStackTrace(new PrintStream(System.out));
						}
					}
				}
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
