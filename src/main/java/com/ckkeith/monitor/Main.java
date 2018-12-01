// Please credit chris.keith@gmail.com
package com.ckkeith.monitor;

import java.io.File;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;

public class Main {
	private static ArrayList<AccountMonitor> monitors = new ArrayList<AccountMonitor>();

	private static void displayEnv() {
		ArrayList<String> envVars = new ArrayList<String>();
		for (String key : System.getenv().keySet()) {
			envVars.add(key + "=" + System.getenv(key));
		}
		Collections.sort(envVars);
		for (String e : envVars) {
			Utils.logToConsole(e);
		}
	}

	public static void main(String[] args) {
		try {
			displayEnv();
			String filePath = Utils.getHomeDir() + File.separator + "Documents" + File.separator + "particle-tokens.txt";
			ArrayList<String> accountTokens = Utils.readParameterFile(filePath);
			for (String c : accountTokens) {
				if (c != null && !c.startsWith("#")) {
					AccountMonitor m = new AccountMonitor(c);
					m.start();
					monitors.add(m);
				}
			}
			LocalDateTime bootTime = Utils.getBootTime();
			int bootMinute;
			if (bootTime == null) {
				bootMinute = 0;
			} else {
				bootMinute = bootTime.getMinute();
			}
			int hourIncrement = 1;
			if (bootMinute > LocalDateTime.now().getMinute()) {
				hourIncrement--;
			}
			LocalDateTime then = LocalDateTime.now().plusHours(hourIncrement).withMinute(bootMinute - 3);
							// - 3 to increase the odds that this instance is gone
							// before Task Scheduler tries to start a new one.
			Utils.sleepUntil("MonitorParticle main - waiting to System.exit(0).", then);
			System.exit(0);
		} catch (Exception e) {
			Utils.logToConsole("main() :\t" + e.getClass().getName() + "\t" + e.getMessage());
			e.printStackTrace(new PrintStream(System.out));
		}
	}
}
