// Please credit chris.keith@gmail.com
package com.ckkeith.monitor;

import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.ArrayList;

public class Main {
	private static ArrayList<PhotonMonitor> monitors = new ArrayList<PhotonMonitor>();

	private static void emailMostRecentEvents() {
		for (PhotonMonitor m : monitors) {
			m.emailMostRecentEvents();
		}
	}

	public static void main(String[] args) {
		try {
			ArrayList<String> params = Utils.readParameterFile("particle-tokens.txt");
			for (String c : params) {
				if (c != null && !c.startsWith("#")) {
					PhotonMonitor m = new PhotonMonitor(c);
					m.start();
					monitors.add(m);
				}
			}
			Utils.logToConsole(GMailer.sendSubjectLine(Utils.nowInLogFormat() + " " + "Particle Monitor server started on " + Utils.getHostName()));

			// At midnight (local time), shut down. Task Scheduler (on Windows) will start a new instance at 12:05.
			// This is to get around the issue where PhotonMonitors randomly stop logging events after a few days.
//			LocalDateTime then = LocalDateTime.now().plusMinutes(6); // for testing self-shutdown only.

			LocalDateTime then = LocalDateTime.now().withHour(23).withMinute(59).withSecond(0);
			Utils.sleepUntil("MonitorParticle main", then);
			emailMostRecentEvents();
			Utils.logToConsole("main() :\t" + "About to System.exit(0)");
			System.exit(0);
		} catch (Exception e) {
			Utils.logToConsole("main() :\t" + e.getClass().getName() + "\t" + e.getMessage());
			e.printStackTrace(new PrintStream(System.out));
		}
	}
}
