package com.ckkeith.monitor;

import java.io.PrintStream;
import java.time.LocalDateTime;

import nl.infcomtec.jparticle.Cloud;
import nl.infcomtec.jparticle.Device;

public class DeviceMonitor extends Thread {

	private String accessToken;
	private Device device;
	private Cloud cloud;

	public DeviceMonitor(String accessToken, Device device, Cloud cloud) throws Exception {
		this.accessToken = accessToken;
		this.device = device;
		this.cloud = cloud;
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
		int retries = 24;
		try {
			while (!device.connected && retries > 0) {
				System.out.println(device.name + " not connected. Will retry in an hour.");
				sleep(60 * 60 * 1000);
				device = Device.getDevice(device.id, "Bearer " + accessToken);
				retries--;
			}
			if (device.connected) {
				ParticleDeviceEvent cb = new ParticleDeviceEvent(device);
				cloud.subscribe(cb);
			} else {
				System.out.println(device.name + " not connected after 24 hours. Giving up.");
			}
		} catch (Exception e) {
			System.out.println("run() : " + LocalDateTime.now().toString() + "\t" + e.getClass().getName() + " "
					+ e.getMessage());
			e.printStackTrace(new PrintStream(System.out));			
		}
	}
	
}
