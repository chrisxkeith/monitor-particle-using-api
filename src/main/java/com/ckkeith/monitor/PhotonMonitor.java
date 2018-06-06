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
		// TODO : add version identifier if possible.
		if (d.variables == null) {
			return "no variables, unknown";
		}
		if (d.variables.has("gitHubHash")) {
			return d.variables.getString("gitHubHash");
		}
		if (d.variables.has("version")) {
			return d.variables.getString("version");
		}
		return "no fields, unknown";
	}
	
	public void run() {
		try {
			logFileName = Utils.getLogFileName(accountName + "_particle_log.txt");
			logger.info("Logging to " + logFileName);
			Utils.log(this.accountName + " : PhotonMonitor thread starting up.", logFileName);
			Cloud c = new Cloud("Bearer " + accessToken, true, false);
			Utils.log(Utils.padWithSpaces("name", 20) + "\t" + Utils.padWithSpaces("id", 24) + "\t" + "connect" + "\t"
					+ Utils.padWithSpaces("lastHeard", 28) + "\t" + Utils.padWithSpaces("version", 28), logFileName);
			for (Map.Entry<String, Device> entry : c.devices.entrySet()) {
				Device d = entry.getValue();
				Utils.log(toTabbed(d), logFileName);
				if (d.connected && d.name.contains("ermistor")) {
//					ParticleDeviceEvent cb = new ParticleDeviceEvent(accountName, d.name);
//					c.subscribe(cb);
				}
			}
		} catch (Exception e) {
			System.out.println(
					"run() : " + LocalDateTime.now().toString() + "\t" + e.getClass().getName() + " " + e.getMessage());
			e.printStackTrace(new PrintStream(System.out));
		}
	}
}
