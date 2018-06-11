package com.ckkeith.monitor;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Map;

import nl.infcomtec.jparticle.Cloud;
import nl.infcomtec.jparticle.Device;

public class PhotonMonitor extends Thread {

	private String accessToken = null;
	private String accountName = null;
	private String deviceName = null;
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
		if (creds.length > 2) {
			this.deviceName = creds[2];
		}
		logFileName = Utils.getLogFileName(accountName + "-devices-overview.txt");
	}

	public void run() {
		try {
			Utils.logToConsole(Utils.padWithSpaces(this.accountName, 20) + "\tPhotonMonitor thread starting up.");
			Cloud c = new Cloud("Bearer " + accessToken, true, false);
			ArrayList<Device> devices = new ArrayList<Device>();
			if (this.deviceName != null && !this.deviceName.isEmpty()) {
				Device d = c.devices.get(this.deviceName);
				if (d != null) {
					devices.add(c.devices.get(this.deviceName));
				}
			} else {
				for (Map.Entry<String, Device> entry : c.devices.entrySet()) {
					devices.add(entry.getValue());
				}
			}
			
			// Print overview first, then start threads.
			ArrayList<DeviceMonitor> deviceMonitors = new ArrayList<DeviceMonitor>();
			for (Device device : devices) {
				// Get device variables and functions
				if (device.connected) {
					device = Device.getDevice(device.id, "Bearer " + accessToken);
				}
				DeviceMonitor dm = new DeviceMonitor(accessToken, device, c);
				Utils.log(dm.toTabbedString(), logFileName);
				if (device.connected) {
					deviceMonitors.add(dm);
				}
			}
			for (DeviceMonitor dm: deviceMonitors) {
				dm.start();
			}
		} catch (Exception e) {
			Utils.logToConsole("run() :\t" + e.getClass().getName() + "\t" + e.getMessage());
			e.printStackTrace(new PrintStream(System.out));
		}
	}
}
