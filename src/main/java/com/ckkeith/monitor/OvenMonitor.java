package com.ckkeith.monitor;

import java.time.LocalDateTime;

import nl.infcomtec.jparticle.Cloud;
import nl.infcomtec.jparticle.Device;

public class OvenMonitor extends Thread {
	private String accessToken;
	private String photonName;
	private int sensorIndex;
	private int pollingInterval;

	public OvenMonitor(String credentials) throws Exception {
		String[] creds = credentials.split("\t");
		if (creds.length > 0) {
			this.accessToken = creds[0];
		} else {
			throw new Exception("No particle token specified.");
		}
		if (creds.length > 1) {
			if (!creds[1].equals("CK")) {
				throw new Exception("Invalid accountName specified : " + creds[1]);
			}
		} else {
			throw new Exception("No accountName specified.");
		}
		// TODO : parameterize these:
		this.photonName = "thermistor-test";
		this.sensorIndex = 0;
		this.pollingInterval = 5;
	}

	public void run() {
		Utils.logToConsole(getClass().getName() + "\tthread started.");
		try {
			Cloud c = new Cloud("Bearer " + accessToken, true, false);
			String sensorName = "sensor" + sensorIndex + "Data";
			while (true) {
				for (Device device : c.devices.values()) {
					if (device.connected && device.name.equals(photonName)) {
						device = Device.getDevice(device.id, "Bearer " + accessToken);
						String val = Utils.getVariable(accessToken, device, sensorName);
						Utils.logToConsole(getClass().getName() + "\t" + photonName + "\t" + sensorName + "\t" + val);
					}
				}
				LocalDateTime then = LocalDateTime.now().plusMinutes(pollingInterval);
				Utils.sleepUntil(getClass().getName() + "\t" + photonName + "\t" + sensorName, then);
			}
		} catch (Throwable t) {
			Utils.logToConsole(getClass().getName() + "\tthread\t" + t.toString());
		}
		Utils.logToConsole(getClass().getName() + "\tthread ended.");
	}
}
