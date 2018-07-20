package com.ckkeith.monitor;

import java.io.PrintStream;
import java.time.Duration;
import java.time.LocalDateTime;

import nl.infcomtec.jparticle.Cloud;
import nl.infcomtec.jparticle.Device;

public class DeviceMonitor extends Thread {

	private String accessToken;
	private Device device;
	private Cloud cloud;
	private String accountName;
	private String logFileName;
	private LocalDateTime lastEventTime;

	public DeviceMonitor(String accountName, String accessToken, Device device, Cloud cloud) throws Exception {
		this.accessToken = accessToken;
		this.device = device;
		this.cloud = cloud;
		this.accountName = accountName;
		logFileName = Utils.getLogFileName(accountName, device.name + "_particle_log.txt");
	}
	
	public String toTabbedString() {
		StringBuffer sb = new StringBuffer();
		sb.append(Utils.padWithSpaces(accountName, 20)).append("\t");
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
		Utils.logWithGSheetsDate(LocalDateTime.now(),
				Utils.padWithSpaces(device.name, 20) + "\t" + s + "\t" + Utils.getHostName(),
				logFileName);
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

    public synchronized void setLastEventTime(LocalDateTime d) {
        this.lastEventTime = d;
    }

    private synchronized LocalDateTime getLastEventTime() {
        return lastEventTime;
    }

    private boolean shouldConnect() {
		if (getLastEventTime() == null) {
			return true;
		}
		// Sometimes the Event subscribers stop recording events,
		// for no reason that I can determine.
		// If no event has come in the last hour, create another Event subscriber.
		return Duration.between(getLastEventTime(),
				LocalDateTime.now()).toMinutes() > 60;
    }

    private void subscribe() throws Exception {
		ParticleDeviceEvent cb;
		if (device.name.contains("thermistor")) {
			cb = new StoveThermistorEvent(accountName, device, this);
		} else {
			cb = new ParticleDeviceEvent(accountName, device, this);
		}
		cloud.subscribe(cb);
    }

	public void run() {
		log("DeviceMonitor thread started.");
		try {
			Integer connectionCount = 0;
			while (true) {
				if (shouldConnect() && ableToConnect()) {
					subscribe();
					if (connectionCount == 0) {
						log(connectionCount++ + " : subscribed.");
					} else {
						log(connectionCount++ + " : resubscribed.");
					}
					sleep(60 * 60 * 1000);
				}
			}
		} catch (Exception e) {
			Utils.logToConsole("run() :\t" + e.getClass().getName() + "\t" + e.getMessage());
			e.printStackTrace(new PrintStream(System.out));			
		}
		log("DeviceMonitor thread exiting.");
	}
	
}
