// Please credit chris.keith@gmail.com
package com.ckkeith.monitor;

import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.ArrayList;

public class Main {
	public static void main(String[] args) {
		try {
			ArrayList<String> params = Utils.readParameterFile("particle-tokens.txt");
			for (String c : params) {
				if (c != null && !c.startsWith("#")) {
					(new PhotonMonitor(c)).start();
				}
			}
			Utils.logToConsole(GMailer.sendSubjectLine(Utils.nowInLogFormat() + " " + "Particle Monitor server started on " + Utils.getHostName()));

			// At midnight (local time), shut down. Task Scheduler (on Windows) will start a new instance at 12:05.
//			LocalDateTime then = LocalDateTime.now().plusMinutes(2); // for testing self-shutdown only.
			LocalDateTime then = LocalDateTime.now().withHour(23).withMinute(59).withSecond(0);
			Utils.sleepUntil("MonitorParticle main", then);
			System.exit(0);
		} catch (Exception e) {
			Utils.logToConsole("main() :\t" + e.getClass().getName() + "\t" + e.getMessage());
			e.printStackTrace(new PrintStream(System.out));
		}
	}
}
