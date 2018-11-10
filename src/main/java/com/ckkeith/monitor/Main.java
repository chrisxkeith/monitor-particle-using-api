// Please credit chris.keith@gmail.com
package com.ckkeith.monitor;

import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.ArrayList;

public class Main {
	private static ArrayList<AccountMonitor> monitors = new ArrayList<AccountMonitor>();
	static RunParams runParams;

	public static void main(String[] args) {
		try {
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
			LocalDateTime then = LocalDateTime.now().plusHours(1).withMinute(0).withSecond(0);
			Utils.sleepUntil("MonitorParticle main - waiting for shutdown.", then);
			System.exit(0);
		} catch (Exception e) {
			Utils.logToConsole("main() :\t" + e.getClass().getName() + "\t" + e.getMessage());
			e.printStackTrace(new PrintStream(System.out));
		}
	}
}
