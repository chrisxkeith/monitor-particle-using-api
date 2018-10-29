package com.ckkeith.monitor;

import java.io.File;
import java.util.ArrayList;

public class RunParams {
	boolean	shutDown = true;
	int		nHtmlFiles = 2;
	int		dataIntervalInMinutes = 10;
	int		htmlWriteIntervalInSeconds = 5;
	int		expectedEventRateInSeconds = 60 * 2;
	
	static RunParams load(String filename) {
		RunParams rp = new RunParams();
		try {
			String fn = Utils.getParamFileDirectory() + filename;
			File f = new File(fn);
			if (!f.exists()) {
				Utils.logToConsole("No param file, using defaults : " + fn);
			} else {
				Utils.logToConsole("Reading param file : " + fn);
				ArrayList<String> params = Utils.readParameterFile(filename);
				for (String s : params) {
					String p[] = s.split("=");
					if (p.length > 1) {
						String key = p[0].trim().toLowerCase();
						String val = p[1].trim();
						if (key.equals("shutdown")) {
							rp.shutDown = Boolean.valueOf(val);
						} else if (key.equals("nhtmlfiles")) {
							rp.nHtmlFiles = Integer.valueOf(val);
						} else if (key.equals("dataintervalinminutes")) {
							rp.dataIntervalInMinutes = Integer.valueOf(val);
						} else if (key.equals("htmlwriteintervalinseconds")) {
							rp.htmlWriteIntervalInSeconds = Integer.valueOf(val);
						} else if (key.equals("expectedeventrateinseconds")) {
							rp.expectedEventRateInSeconds = Integer.valueOf(val);
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
		return "RunParams : shutdown = " + shutDown
				+ ", nHtmlFiles = " + nHtmlFiles
				+ ", dataIntervalInMinutes = " + dataIntervalInMinutes
				+ ", htmlWriteIntervalInSeconds = " + htmlWriteIntervalInSeconds
				+ ", expectedEventRateInSeconds = " + expectedEventRateInSeconds;
	}
}
