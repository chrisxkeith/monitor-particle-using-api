package com.ckkeith.monitor;

import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.Map;

import nl.infcomtec.jparticle.Cloud;
import nl.infcomtec.jparticle.Device;

public class PhotonMonitor extends Thread {
	private String accessToken = null;

	private String padWithSpaces(String s, int length) {
		StringBuffer ret = new StringBuffer(s);
		int nSpaces = length - s.length();
		while (nSpaces > 0) {
			ret.append(" ");
			nSpaces--;
		}
		return ret.toString();
	}

	private String toTabbed(Device d) {
		StringBuffer sb = new StringBuffer();
		sb.append(padWithSpaces(d.name, 20)).append("\t");
		sb.append(d.id).append("\t");
		sb.append(d.connected).append("\t");
		sb.append(d.lastHeard);
		return sb.toString();
	}

	public PhotonMonitor(String credentials) {
		String[] creds = credentials.split("\t");
		if (creds.length > 0) {
			this.accessToken = creds[0];
		}
	}

	public void run() {
		if (accessToken == null) {
			System.out.println("No access token.");
			return;
		}
		try {
			Cloud c = new Cloud("Bearer " + accessToken, true, false);
			System.out.println(padWithSpaces("name", 20) + "\t" + 
					padWithSpaces("id", 24) + "\t" +
					"connect" + "\t" + 
					padWithSpaces("lastHeard", 28));
			for (Map.Entry<String, Device> entry : c.devices.entrySet()) {
				Device d = entry.getValue();
				System.out.println(toTabbed(d));
			}
		} catch (Exception e) {
			System.out.println("main() : " + LocalDateTime.now().toString() + "\t" + e.getClass().getName() + " " + e.getMessage());
		    e.printStackTrace(new PrintStream(System.out));
		}
	}
}
