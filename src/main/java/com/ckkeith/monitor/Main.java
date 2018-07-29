// Please credit chris.keith@gmail.com
package com.ckkeith.monitor;

import java.io.PrintStream;
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
		} catch (Exception e) {
			Utils.logToConsole("main() :\t" + e.getClass().getName() + "\t" + e.getMessage());
			e.printStackTrace(new PrintStream(System.out));
		}
	}
}
