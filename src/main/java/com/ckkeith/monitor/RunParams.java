package com.ckkeith.monitor;

import java.io.File;
import java.util.ArrayList;

public class RunParams {
	int		nHtmlFiles = 2;
	int		dataIntervalInMinutes = 10;
	int		htmlWriteIntervalInSeconds = 5;
	int		expectedEventRateInSeconds = 60 * 2;
	int		shutDownHours = 1;
	
	static RunParams load(String filename) {
		RunParams rp = new RunParams();
		try {
			String fn = Utils.getParamFileDirectory() + filename;
			File f = new File(fn);
			if (!f.exists()) {
				Utils.logToConsole("No param file (" + fn + ") using defaults");
			} else {
				Utils.logToConsole("Reading param file : " + fn);
				ArrayList<String> params = Utils.readParameterFile(filename);
				for (String s : params) {
					String p[] = s.split("=");
					if (p.length > 1) {
						String key = p[0].trim();
						String val = p[1].trim();
						if (key.equalsIgnoreCase("nHtmlFiles")) {
							rp.nHtmlFiles = Integer.valueOf(val);
						} else if (key.equalsIgnoreCase("dataIntervalInMinutes")) {
							rp.dataIntervalInMinutes = Integer.valueOf(val);
						} else if (key.equalsIgnoreCase("htmlWriteIntervalInSeconds")) {
							rp.htmlWriteIntervalInSeconds = Integer.valueOf(val);
						} else if (key.equalsIgnoreCase("expectedEventRateInSeconds")) {
							rp.expectedEventRateInSeconds = Integer.valueOf(val);
						} else if (key.equalsIgnoreCase("shutDownHours")) {
							rp.shutDownHours = Integer.valueOf(val);
						}
					}
				}				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return rp;
	}

	public String toString() {
		return "RunParams : "
				+ "nHtmlFiles = " + nHtmlFiles
				+ ", dataIntervalInMinutes = " + dataIntervalInMinutes
				+ ", htmlWriteIntervalInSeconds = " + htmlWriteIntervalInSeconds
				+ ", expectedEventRateInSeconds = " + expectedEventRateInSeconds
				+ ", shutDownHours = " + shutDownHours
				;
	}
}
