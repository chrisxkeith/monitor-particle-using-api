// Please credit chris.keith@gmail.com
package com.ckkeith.monitor;

import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.ArrayList;

public class Main {
	private static ArrayList<PhotonMonitor> monitors = new ArrayList<PhotonMonitor>();
	private static RunParams runParams;

	private static void emailMostRecentEvents() {
		for (PhotonMonitor m : monitors) {
			m.emailMostRecentEvents();
		}
	}

	public static void main(String[] args) {
		try {
			runParams = RunParams.load("monitor-run-params");
			Utils.logToConsole(runParams.toString());

			ArrayList<String> accountTokens = Utils.readParameterFile("particle-tokens.txt");
			for (String c : accountTokens) {
				if (c != null && !c.startsWith("#")) {
					PhotonMonitor m = new PhotonMonitor(c);
					m.start();
					monitors.add(m);
				}
			}
			Utils.logToConsole(GMailer.sendSubjectLine(Utils.nowInLogFormat() + " " + "Particle Monitor server started on " + Utils.getHostName()));

			while (true) {
				LocalDateTime then = LocalDateTime.now().withHour(23).withMinute(50).withSecond(0);
				Utils.sleepUntil("MonitorParticle main - waiting to send daily email.", then);
				emailMostRecentEvents();

				if (runParams.shutDown) {
					// Just before midnight (local time), shut down.
					// Task Scheduler (on Windows) will start a new instance at 12:05.
					// This is to get around the issue where PhotonMonitors randomly stop logging events
					// after a few days.
					then = LocalDateTime.now().withHour(23).withMinute(59).withSecond(0);
					Utils.sleepUntil("MonitorParticle main - waiting to System.exit(0).", then);
					Utils.logToConsole("main() :\t" + "About to System.exit(0)");
					System.exit(0);
				}
			}
		} catch (Exception e) {
			Utils.logToConsole("main() :\t" + e.getClass().getName() + "\t" + e.getMessage());
			e.printStackTrace(new PrintStream(System.out));
		}
	}
}
