// Please credit chris.keith@gmail.com
package com.ckkeith.monitor;

import java.io.File;
import java.util.ArrayList;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class RunParams {
	Integer		dataIntervalInMinutes = 10;
	Integer		htmlWriteIntervalInSeconds = 5;
	Integer		expectedEventRateInSeconds = 60 * 2;
	String		devicesToReport = "";
	String		deviceNameToSheetId = "";
	Integer		csvTimeGranularityInSeconds = 60;	// E.g. '60' == round to minutes, '30' == round to minutes and half-minutes.
	Integer		sheetsWriteIntervalInSeconds = 10;
	
	private static Integer getInteger(Element root, String name, Integer defaultValue) {
		NodeList nl = root.getElementsByTagName(name);
		if (nl.getLength() > 0) {
			return Integer.parseInt(nl.item(0).getTextContent());
		}
		return defaultValue;
	}

	private static String getString(Element root, String name, String defaultValue) {
		NodeList nl = root.getElementsByTagName(name);
		if (nl.getLength() > 0) {
			return nl.item(0).getTextContent();
		}
		return defaultValue;
	}

	static RunParams loadFromXML(String filePath) throws Exception {
		RunParams rp = new RunParams();
		Element root = Utils.readTextFileIntoDOM(filePath).getDocumentElement();
		rp.dataIntervalInMinutes = getInteger(root, "dataIntervalInMinutes", rp.dataIntervalInMinutes);
		rp.htmlWriteIntervalInSeconds = getInteger(root, "htmlWriteIntervalInSeconds", rp.htmlWriteIntervalInSeconds);
		rp.expectedEventRateInSeconds = getInteger(root, "expectedEventRateInSeconds", rp.expectedEventRateInSeconds);
		rp.csvTimeGranularityInSeconds = getInteger(root, "csvTimeGranularityInSeconds", rp.csvTimeGranularityInSeconds);
		rp.sheetsWriteIntervalInSeconds = getInteger(root, "sheetsWriteIntervalInSeconds", rp.sheetsWriteIntervalInSeconds);
		rp.devicesToReport = getString(root, "devicesToReport", rp.devicesToReport);
		rp.deviceNameToSheetId = getString(root, "deviceNameToSheetId", rp.deviceNameToSheetId);
		return rp;
	}

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
