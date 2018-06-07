package com.ckkeith.monitor;

import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Logger;

import nl.infcomtec.jparticle.Cloud;
import nl.infcomtec.jparticle.Device;

public class PhotonMonitor extends Thread {

	private static final Logger logger = Logger.getLogger(PhotonMonitor.class.getName());

	private String logFileName;

	private String accessToken = null;
	private String accountName = null;
	private String deviceName = null;

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
	}

	public void run() {
		try {
			logFileName = Utils.getLogFileName(accountName + "_particle_log.txt");
			logger.info("Logging to " + logFileName);
			Utils.log(this.accountName + " : PhotonMonitor thread starting up.", logFileName);
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
				deviceMonitors.add(dm);
			}
			for (DeviceMonitor dm: deviceMonitors) {
				dm.start();
			}
		} catch (Exception e) {
			System.out.println(
					"run() : " + LocalDateTime.now().toString() + "\t" + e.getClass().getName() + " " + e.getMessage());
			e.printStackTrace(new PrintStream(System.out));
		}
	}
}
