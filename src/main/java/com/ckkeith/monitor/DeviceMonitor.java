package com.ckkeith.monitor;

import java.io.PrintStream;

import nl.infcomtec.jparticle.Cloud;
import nl.infcomtec.jparticle.Device;

public class DeviceMonitor extends Thread {

	private String accessToken;
	private Device device;
	private Cloud cloud;
	private String logFileName;

	public DeviceMonitor(String accessToken, Device device, Cloud cloud) throws Exception {
		this.accessToken = accessToken;
		this.device = device;
		this.cloud = cloud;
		logFileName = Utils.getLogFileName(device.name + "_particle_log.txt");
	}
	
	public String toTabbedString() {
		StringBuffer sb = new StringBuffer();
		sb.append(Utils.padWithSpaces(device.name, 20)).append("\t");
		sb.append(device.id).append("\t");
		sb.append(device.connected).append("\t");
		sb.append(device.lastHeard).append("\t");
		sb.append(getVersionString(device));
		return sb.toString();
	}

	private String getVersionString(Device d) {		
		if (d.variables == null) {
			return "unknown (no variables)";
		}
		if (d.variables.has("GitHubHash")) {
			return d.readString("GitHubHash", "Bearer " + accessToken);
		}
		return "unknown (no GitHubHash)";
	}

	public void run() {
		Utils.log(Utils.padWithSpaces(device.name, 20) + "\tthread started.", logFileName);
		int retries = 24;
		try {
			while (!device.connected && retries > 0) {
				Utils.log(Utils.padWithSpaces(device.name, 20) + " not connected. Will retry in an hour.", logFileName);
				sleep(60 * 60 * 1000);
				device = Device.getDevice(device.id, "Bearer " + accessToken);
				retries--;
			}
			if (device.connected) {
				ParticleDeviceEvent cb = new ParticleDeviceEvent(device);
				cloud.subscribe(cb);
				Utils.log(Utils.padWithSpaces(device.name, 20) + "\tsubscribed.", logFileName);
			} else {
				Utils.log(Utils.padWithSpaces(device.name, 20) + "\tnot connected after 24 hours. Giving up.", logFileName);
			}
		} catch (Exception e) {
			Utils.logToConsole("run() :\t" + e.getClass().getName() + "\t" + e.getMessage());
			e.printStackTrace(new PrintStream(System.out));			
		}
	}
	
}
