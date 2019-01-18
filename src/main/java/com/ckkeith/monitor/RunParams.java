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

	private static String getDeviceList(Element root) {
		String ret = "";
		NodeList nl = root.getElementsByTagName("deviceNameToSheetId");
		for (int i = 0; i < nl.getLength(); i++) {
			ret += nl.item(i).getTextContent();
			if (i < nl.getLength() - 1) {
				ret += "|";
			}
		}
		return ret;
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
		rp.deviceNameToSheetId = getDeviceList(root);
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
