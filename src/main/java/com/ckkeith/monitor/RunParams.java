// Please credit chris.keith@gmail.com
package com.ckkeith.monitor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

public class RunParams {

	public class Dataset {
		// microcontrollerName -> list of sensor names
		Hashtable<String, HashSet<String>> microcontrollers;
		public Dataset(Hashtable<String, HashSet<String>> microcontrollers) {
			this.microcontrollers = microcontrollers;
		}
	};

	public class SheetConfig {
		public Integer				dataIntervalInMinutes = 20;
		public Integer				writeIntervalInSeconds = 10;
		public ArrayList<Dataset>	dataSets;
	};

	Integer		htmlWriteIntervalInSeconds = 5;
	String		devicesToReport = "";
	Integer		csvTimeGranularityInSeconds = 300; // E.g. '60' == round to minutes, '30' == round to minutes and half-minutes.
	Boolean		writeLongTermData = false;
	Hashtable<String, SheetConfig> sheets = new Hashtable<String, SheetConfig>();
	int			temperatureLimit = 90; // degrees F
	int			timeLimit = 60; // minutes before alert is logged.
	String		emailTo = "chris.keith@gmail.com";
	Integer		gapTriggerInMinutes = 10; // Display gaps in the data if they are longer than this.
	Integer		daysOfGapData = 30; // Go back this many days for gap data.

	 // If temperature doesn't go down (after sending email) in resendIntervalInMinutes,
	 // keep resending emails until it does.
	 Integer	resendIntervalInMinutes = 10;
			
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

	private ArrayList<Dataset> buildDatasetList(NodeList datasetElems) throws Exception {
		ArrayList<Dataset> datasets = new ArrayList<Dataset>();
		for (int i = 0; i < datasetElems.getLength(); i++) {
			Node datasetNode = datasetElems.item(i);
			if (datasetNode.getNodeType() == Node.ELEMENT_NODE) {
				Hashtable<String, HashSet<String>> microcontrollers = new Hashtable<String, HashSet<String>>();
				Element elem = (Element)datasetNode;
				NodeList microcontrollerNameElems = elem.getElementsByTagName("microcontrollerName");
				if (microcontrollerNameElems.getLength() == 0) {
					throw new Exception("microcontrollerName.getLength() == 0");
				}
				String microcontrollerName = microcontrollerNameElems.item(0).getTextContent();
				NodeList sensors = elem.getElementsByTagName("sensors");
				if (sensors.getLength() == 0) {
					throw new Exception("sensors.getLength() == 0");
				}
				HashSet<String> sensorNames = new HashSet<String>();
				for (int nameIndex = 0; nameIndex < sensors.getLength(); nameIndex++) {
					// TO (eventually) DO : find correct Node to get text from instead of using trim().
					sensorNames.add(sensors.item(nameIndex).getTextContent().trim());
				}
				microcontrollers.put(microcontrollerName, sensorNames);
				datasets.add(new Dataset(microcontrollers));
			}
		}
		return datasets;
	}

	NodeList getNodeList( Element element, String id) throws Exception {
		NodeList nodeList = element.getElementsByTagName(id);
		if (nodeList.getLength() == 0) {
			throw new Exception(id + ": getLength() == 0");
		}
		return nodeList;
	}

	private SheetConfig loadSheetConfig(Element sheetElement) throws Exception {
		SheetConfig sheetConfig = new SheetConfig();
		NodeList datasetElems = getNodeList(sheetElement, "dataSet");
		sheetConfig.dataSets = buildDatasetList(datasetElems);
		sheetConfig.dataIntervalInMinutes = Integer.valueOf(getNodeList(sheetElement, "dataIntervalInMinutes").item(0).getTextContent());
		sheetConfig.writeIntervalInSeconds = Integer.valueOf(getNodeList(sheetElement, "writeIntervalInSeconds").item(0).getTextContent());
		return sheetConfig;
	}

	private void addSheet(Element sheetElement) throws Exception {
		NodeList sheetIdList = getNodeList(sheetElement, "sheetId");
		String sheetId = sheetIdList.item(0).getTextContent();
		sheets.put(sheetId, loadSheetConfig(sheetElement));
	}

	private void loadSheets(Element root) throws Exception {
		NodeList sheetsRoot = root.getElementsByTagName("sheets");
		if (sheetsRoot.getLength() == 0) {
			throw new Exception("sheetsRoot.getLength() == 0");
		}
		Node sheets = sheetsRoot.item(0);
		if (sheets.getNodeType() != Node.ELEMENT_NODE) {
			throw new Exception("sheets.getNodeType() != Node.ELEMENT_NODE");
		}
		NodeList sheetsList = ((Element)sheets).getElementsByTagName("sheet");
		for (int i = 0; i < sheetsList.getLength(); i++) {
			if (sheetsList.item(i).getNodeType() == Node.ELEMENT_NODE) {
				addSheet((Element)sheetsList.item(i));
			}
		}
	}

	static RunParams loadFromXML(String filePath) throws Exception {
		RunParams rp = new RunParams();
		Element root = Utils.readTextFileIntoDOM(filePath).getDocumentElement();
		rp.htmlWriteIntervalInSeconds = getInteger(root, "htmlWriteIntervalInSeconds", rp.htmlWriteIntervalInSeconds);
		rp.csvTimeGranularityInSeconds = getInteger(root, "csvTimeGranularityInSeconds", rp.csvTimeGranularityInSeconds);
		rp.writeLongTermData = getInteger(root, "writeLongTermData", 0) == 0 ? false : true;
		rp.devicesToReport = getString(root, "devicesToReport", rp.devicesToReport);
		rp.temperatureLimit = getInteger(root, "temperatureLimit", 80);
		rp.timeLimit = getInteger(root, "timeLimit", 60);
		rp.emailTo = getString(root, "emailTo", "chris.keith@gmail.com");
		rp.resendIntervalInMinutes = getInteger(root, "resendIntervalInMinutes", 10);
		rp.gapTriggerInMinutes = getInteger(root, "gapTriggerInMinutes", 10);
		rp.daysOfGapData = getInteger(root, "daysOfGapData", 30);
		rp.loadSheets(root);
		return rp;
	}

	public String toString() {
		return "RunParams : "
				+ ", htmlWriteIntervalInSeconds = " + htmlWriteIntervalInSeconds
				+ ", devicesToReport = " + devicesToReport
				+ ", csvTimeGranularityInSeconds = " + csvTimeGranularityInSeconds
				+ ", temperatureLimit = " + temperatureLimit
				+ ", timeLimit = " + timeLimit
				+ ", emailTo = " + emailTo
				+ ", resendIntervalInMinutes = " + resendIntervalInMinutes
				+ ", gapTriggerInMinutes = " + gapTriggerInMinutes
				+ ", daysOfGapData = " + daysOfGapData
				;
	}
}
