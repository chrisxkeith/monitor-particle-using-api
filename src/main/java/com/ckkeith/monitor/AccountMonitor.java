// Please credit chris.keith@gmail.com
package com.ckkeith.monitor;

import java.io.File;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AccountMonitor extends Thread {

	String accessToken = null;
	String accountName = null;
	private String logFileName;
	private Map<String, ParticleDeviceEvent> eventSubscribers = new HashMap<String, ParticleDeviceEvent>();
	private HtmlFileDataWriter htmlFileDataWriter;
	private GoogleSheetsWriter googleSheetsWriter;
	RunParams runParams;
	Map<String, DeviceMonitor> deviceMonitors = new HashMap<String, DeviceMonitor>();
	Map<String, String> deviceNameToSheetId = new HashMap<String, String>();

	public AccountMonitor(String credentials) throws Exception {
		String[] creds = credentials.split("\t");
		if (creds.length > 0) {
			this.accessToken = creds[0];
		} else {
			throw new Exception("No particle token specified.");
		}
		if (creds.length > 1) {
			this.accountName = creds[1];
		} else {
			throw new Exception("No account name specified.");
		}
		logFileName = Utils.getLogFileName(accountName, "devices-overview.txt");
		runParams = RunParams.load(getParamFilePath());
		Utils.logToConsole(accountName + ": " + runParams.toString());

		// Parameterize sheet ids when necessary.
		if (accountName.equals("chris.keith@gmail.com")) {
			deviceNameToSheetId.put("thermistor2-test", "1Tq1lGWuO4kipyKz_zIaXOQhVN5B4as0N0VEG_PTgP9w");
		} else if (accountName.equals("ara@verdical.io")) {
			deviceNameToSheetId.put("US Foods", "1qCHfRDno-Lp-fzIc_xUbq7kjU0lkxLrjGb9dVqtWAuE");
			deviceNameToSheetId.put("verdical_tester_usfoods2", "1xWj_bFERM0tvtJYdak0Ujba3h9dPJjpn2-YK4Ibji2I");
	//			deviceNameToSheetId.put("verdical_tester_5", "1fA8T5qodNa48EQro1B5FIwHMeAd3kciOdx6dgAAOWFI");
		}
	}

	private String getParamFilePath() throws Exception {
		return Utils.getHomeDir() + File.separator + "Documents" + File.separator + "tmp" + File.separator
				+ Utils.getHostName() + File.separator + Utils.getSafeName(accountName) + File.separator
				+ "runparams.txt";
	}

	void startDeviceMonitors(PivotDataApp pivotDataApp) {
		ParticleCloud c = new ParticleCloud("Bearer " + accessToken, true, false);
		ArrayList<DeviceMonitor> newDevices = new ArrayList<DeviceMonitor>();
		for (ParticleDevice device : c.getDevices()) {
			try {
				if (!device.getConnected()) {
					Utils.logToConsole("Skipping disconnected device : " + device.getName());
				} else {
					// Get device variables and functions
					device = device.getDevice("Bearer " + accessToken);
					DeviceMonitor dm = new DeviceMonitor(this, device, c);
					Utils.logWithGSheetsDate(LocalDateTime.now(), dm.toTabbedString(), logFileName);
					if (deviceNameToSheetId.get(device.getName()) != null && deviceMonitors.get(device.getName()) == null) {
						deviceMonitors.put(device.getName(), dm);
						newDevices.add(dm);
					}
					// Server returned HTTP response code: 502 for URL: https://api.particle.io/v1/devices/4b0050001151373331333230
					LocalDateTime then = LocalDateTime.now().plusSeconds(3);
					Utils.sleepUntil(
						"AccountMonitor.startDeviceMonitors() sleeping to try to avoid \"Too many requests\" (http 502) error for: "
								+ device.getName(),
						then);
				}
			} catch (Exception e) {
				String err = "run() :\t" + device.getName() + "\t" + e.getClass().getName() + "\t" + e.getMessage();
				Utils.logToConsole(err);
				e.printStackTrace(new PrintStream(System.out));
			}
		}
		for (DeviceMonitor dm : newDevices) {
			dm.start();
		}
		// if (htmlFileDataWriter == null && runParams.htmlWriteIntervalInSeconds > 0) {
		// 	htmlFileDataWriter = new HtmlFileDataWriter(this);
		// 	htmlFileDataWriter.start();
		// }
		if (googleSheetsWriter == null && runParams.sheetsWriteIntervalInSeconds > 0) {
			googleSheetsWriter = new GoogleSheetsWriter(this, pivotDataApp);
			googleSheetsWriter.start();
		}
	}

	public void run() {
		Utils.logToConsole(Utils.padWithSpaces(this.accountName, 20) + "\tPhotonMonitor thread starting.");
		PivotDataApp	pivotDataApp = null;
		try {
			pivotDataApp = new PivotDataApp(Utils.getLogFileDir(accountName), runParams);
		} catch (Exception e1) {
			Utils.logToConsole("Unable to construct a PivotDataApp");
			e1.printStackTrace();
		}
		startDeviceMonitors(pivotDataApp);
// Eventually put while(true) loop here to write longer term data to Google Sheets
		Utils.logToConsole(Utils.padWithSpaces(this.accountName, 20) + "\tPhotonMonitor thread exiting.");
	}

	public void addEventSubscriber(String name, ParticleDeviceEvent cb) {
		synchronized(eventSubscribers) {
			eventSubscribers.put(name, cb);
		}
	}

	public void addDataPoint(LocalDateTime ldt, String deviceName, String event, String data) {
		if (event.contains("ensor") || event.contains("ontroller")) {
			if ((htmlFileDataWriter != null)) {
				this.htmlFileDataWriter.addData(new EventData(ldt, deviceName, event, data));
			}
			if ((googleSheetsWriter != null)) {
				this.googleSheetsWriter.addData(new EventData(ldt, deviceName, event, data));
			}
		}
	}
}
