package com.ckkeith.monitor;

import java.io.PrintStream;
import java.time.LocalDateTime;
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

	private String toTabbed(Device d) {
		StringBuffer sb = new StringBuffer();
		sb.append(Utils.padWithSpaces(d.name, 20)).append("\t");
		sb.append(d.id).append("\t");
		sb.append(d.connected).append("\t");
		sb.append(d.lastHeard).append("\t");
		sb.append(getVersionString(d));
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

	private void addDevice(Device d, Cloud c) throws Exception {
		// Get device variables and functions
		if (d.connected) {
			d = Device.getDevice(d.id, "Bearer " + accessToken);
		}
		Utils.log(toTabbed(d), logFileName);
		if (d.connected) {
			ParticleDeviceEvent cb = new ParticleDeviceEvent(d);
			c.subscribe(cb);
		}
	}
	
	public void run() {
		try {
			logFileName = Utils.getLogFileName(accountName + "_particle_log.txt");
			logger.info("Logging to " + logFileName);
			Utils.log(this.accountName + " : PhotonMonitor thread starting up.", logFileName);
			Cloud c = new Cloud("Bearer " + accessToken, true, false);
			if (this.deviceName != null && !this.deviceName.isEmpty()) {
				addDevice(c.devices.get(this.deviceName), c);
			} else {
				for (Map.Entry<String, Device> entry : c.devices.entrySet()) {
					addDevice(entry.getValue(), c);
				}
			}
		} catch (Exception e) {
			System.out.println(
					"run() : " + LocalDateTime.now().toString() + "\t" + e.getClass().getName() + " " + e.getMessage());
			e.printStackTrace(new PrintStream(System.out));
		}
	}
}
