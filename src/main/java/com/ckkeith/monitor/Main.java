// Please credit chris.keith@gmail.com
package com.ckkeith.monitor;

import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.ArrayList;

public class Main {
	private static ArrayList<AccountMonitor> monitors = new ArrayList<AccountMonitor>();
	static RunParams runParams;

	private static void emailMostRecentEvents() {
		for (AccountMonitor m : monitors) {
			m.emailMostRecentEvents();
		}
	}

	public static void main(String[] args) {
		try {
			// TODO : make this per-account, rather than per-executable-instance.
			runParams = RunParams.load("monitor-run-params.txt");
			Utils.logToConsole(runParams.toString());

			ArrayList<String> accountTokens = Utils.readParameterFile("particle-tokens.txt");
			for (String c : accountTokens) {
				if (c != null && !c.startsWith("#")) {
					AccountMonitor m = new AccountMonitor(c);
					m.start();
					monitors.add(m);
				}
			}
			Utils.logToConsole(GMailer.sendSubjectLine(Utils.nowInLogFormat() + " " + "Particle Monitor server started on " + Utils.getHostName()));

			while (true) {
				LocalDateTime then = LocalDateTime.now().withHour(23).withMinute(50).withSecond(0);
				Utils.sleepUntil("MonitorParticle main - waiting to send daily 'most recent events' email.", then);
				emailMostRecentEvents();

				if (runParams.shutDown) {
					// Shut self down at specified time (currently 3 a.m. tomorrow).
					// You should create a Scheduled Task (on Windows) to restart 5 minutes (or so) afterwards.
					// This is a hack to get around an issue where ParticleDeviceEvents randomly stop logging events
					// after a day or two.
					then = LocalDateTime.now().plusDays(1).withHour(3).withMinute(0).withSecond(0);
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
