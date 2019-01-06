// Please credit chris.keith@gmail.com
package com.ckkeith.monitor;

import java.io.File;
import java.util.ArrayList;

public class RunParams {
	int		dataIntervalInMinutes = 10;
	int		htmlWriteIntervalInSeconds = 5;
	int		expectedEventRateInSeconds = 60 * 2;
	String	devicesToReport = "";
	String	deviceNameToSheetId = "";
	int		csvTimeGranularityInSeconds = 60;	// E.g. '60' == round to minutes, '30' == round to minutes and half-minutes.
	public int sheetsWriteIntervalInSeconds = 10;
	
	static RunParams load(String filePath) {
		RunParams rp = new RunParams();
		try {
			File f = new File(filePath);
			if (!f.exists()) {
				Utils.logToConsole("No param file (" + filePath + ") using defaults");
			} else {
				Utils.logToConsole("Reading param file : " + filePath);
				ArrayList<String> params = Utils.readParameterFile(filePath);
				for (String s : params) {
					String p[] = s.split("=");
					if (p.length > 1) {
						String key = p[0].trim();
						String val = p[1].trim();
						if (key.equalsIgnoreCase("dataIntervalInMinutes")) {
							rp.dataIntervalInMinutes = Integer.valueOf(val);
						} else if (key.equalsIgnoreCase("htmlWriteIntervalInSeconds")) {
							rp.htmlWriteIntervalInSeconds = Integer.valueOf(val);
						} else if (key.equalsIgnoreCase("expectedEventRateInSeconds")) {
							rp.expectedEventRateInSeconds = Integer.valueOf(val);
						} else if (key.equalsIgnoreCase("devicesToReport")) {
							rp.devicesToReport = val;
						} else if (key.equalsIgnoreCase("deviceNameToSheetId")) {
							rp.deviceNameToSheetId = val;
						} else if (key.equalsIgnoreCase("csvTimeGranularityInSeconds")) {
							rp.csvTimeGranularityInSeconds = Integer.valueOf(val);
						} else if (key.equalsIgnoreCase("sheetsWriteIntervalInSeconds")) {
							rp.sheetsWriteIntervalInSeconds = Integer.valueOf(val);
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
				+ "dataIntervalInMinutes = " + dataIntervalInMinutes
				+ ", htmlWriteIntervalInSeconds = " + htmlWriteIntervalInSeconds
				+ ", sheetsWriteIntervalInSeconds = " + sheetsWriteIntervalInSeconds
				+ ", expectedEventRateInSeconds = " + expectedEventRateInSeconds
				+ ", devicesToReport = " + devicesToReport
				+ ", deviceNameToSheetId = " + deviceNameToSheetId
				+ ", csvTimeGranularityInSeconds = " + csvTimeGranularityInSeconds
				+ ", sheetsWriteIntervalInSeconds = " + sheetsWriteIntervalInSeconds
				;
	}
}
