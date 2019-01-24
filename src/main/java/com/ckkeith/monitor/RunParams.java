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

	Integer		dataIntervalInMinutes = 10;
	Integer		htmlWriteIntervalInSeconds = 5;
	Integer		expectedEventRateInSeconds = 60 * 2;
	String		devicesToReport = "";
	String		deviceNameToSheetId = "";
	Integer		csvTimeGranularityInSeconds = 60; // E.g. '60' == round to minutes, '30' == round to minutes and half-minutes.
	Integer		sheetsWriteIntervalInSeconds = 10;
	Hashtable<String, ArrayList<Dataset>> sheets =
				new Hashtable<String, ArrayList<Dataset>>();

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
					sensorNames.add(sensors.item(nameIndex).getTextContent());
				}
				microcontrollers.put(microcontrollerName, sensorNames);
				datasets.add(new Dataset(microcontrollers));
			}
		}
		return datasets;
	}

	private void addSheet(Element sheetElement) throws Exception {
		NodeList sheetIdList = sheetElement.getElementsByTagName("sheetId");
		if (sheetIdList.getLength() == 0) {
			throw new Exception("sheetIdList.getLength() == 0");
		}
		String sheetId = sheetIdList.item(0).getTextContent();
		NodeList datasetElems = sheetElement.getElementsByTagName("dataSet");
		if (datasetElems.getLength() == 0) {
			throw new Exception("datasets.getLength() == 0");
		}
		sheets.put(sheetId, buildDatasetList(datasetElems));
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
		rp.dataIntervalInMinutes = getInteger(root, "dataIntervalInMinutes", rp.dataIntervalInMinutes);
		rp.htmlWriteIntervalInSeconds = getInteger(root, "htmlWriteIntervalInSeconds", rp.htmlWriteIntervalInSeconds);
		rp.expectedEventRateInSeconds = getInteger(root, "expectedEventRateInSeconds", rp.expectedEventRateInSeconds);
		rp.csvTimeGranularityInSeconds = getInteger(root, "csvTimeGranularityInSeconds", rp.csvTimeGranularityInSeconds);
		rp.sheetsWriteIntervalInSeconds = getInteger(root, "sheetsWriteIntervalInSeconds", rp.sheetsWriteIntervalInSeconds);
		rp.devicesToReport = getString(root, "devicesToReport", rp.devicesToReport);
		rp.deviceNameToSheetId = getDeviceList(root);
		rp.loadSheets(root);
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
