package com.ckkeith.monitor;

import java.io.PrintStream;
import java.time.LocalDateTime;

import nl.infcomtec.jparticle.Cloud;
import nl.infcomtec.jparticle.Device;

public class DeviceMonitor extends Thread {

	private String accessToken;
	private Device device;
	private Cloud cloud;
	private String logFileName;
	private AccountMonitor accountMonitor;

	public DeviceMonitor(AccountMonitor accountMonitor, Device device, Cloud cloud) throws Exception {
		this.device = device;
		this.cloud = cloud;
		this.accountMonitor = accountMonitor;
		logFileName = Utils.getLogFileName(accountMonitor.accountName, device.name + "_particle_log.txt");
	}

	public String toTabbedString() {
		StringBuffer sb = new StringBuffer();
		sb.append(Utils.padWithSpaces(accountMonitor.accountName, 20)).append("\t");
		sb.append(Utils.padWithSpaces(device.name, 20)).append("\t");
		sb.append(device.id).append("\t");
		sb.append(device.connected).append("\t");
		sb.append(device.lastHeard).append("\t");
		sb.append(getVersionString(device)).append("\t");
		sb.append(Utils.getHostName());
		return sb.toString();
	}

	private String getVersionString(Device d) {
		return Utils.getVariable(accessToken, d, "GitHubHash");
	}

	private void log(String s) {
		String msg = Utils.padWithSpaces(device.name, 20) + "\t" + s + "\t" + Utils.getHostName();
		Utils.logWithGSheetsDate(LocalDateTime.now(), msg, logFileName);
		Utils.logToConsole(msg);
	}

	private boolean ableToConnect() throws Exception {
		int retries = 24;
		while (!device.connected && retries > 0) {
			log("not connected. Will retry in an hour.");
			sleep(60 * 60 * 1000);
			device = Device.getDevice(device.id, "Bearer " + accessToken);
			retries--;
		}
		if (!device.connected) {
			log("not connected after 24 hours.");
		}
		return device.connected;
	}

	private void subscribe() throws Exception {
		ParticleDeviceEvent cb;
		if (device.name.contains("thermistor") && "chris.keith@gmail.com".equals(accountMonitor.accountName)) {
			cb = new StoveThermistorEvent(accountMonitor, device);
		} else {
			cb = new ParticleDeviceEvent(accountMonitor, device);
		}
		cloud.subscribe(cb);
		accountMonitor.addEventSubscriber(device.name, cb);
	}

	public void run() {
		log("DeviceMonitor thread started.");
		try {
			if (ableToConnect()) {
				subscribe();
				log("subscribed.");
			}
		} catch (Exception e) {
			Utils.logToConsole("run() :\t" + e.getClass().getName() + "\t" + e.getMessage());
			e.printStackTrace(new PrintStream(System.out));
		}
		log("DeviceMonitor thread exiting.");
	}

}
