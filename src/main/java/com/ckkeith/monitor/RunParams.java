package com.ckkeith.monitor;

import java.io.File;
import java.util.ArrayList;

public class RunParams {
	boolean	shutDown = true;
	int		nHtmlFiles = 3;
	int		dataIntervalInMinutes = 10;
	int		htmlWriteIntervalInSeconds = 5;
	
	static RunParams load(String filename) {
		RunParams rp = new RunParams();
		try {
			File f = new File(Utils.getParamFileDirectory() + filename);
			if (f.exists()) {
				ArrayList<String> params = Utils.readParameterFile(filename);
				for (String s : params) {
					String p[] = s.split("=");
					if (p.length > 1) {
						String key = p[0].trim();
						String val = p[1].trim();
						if (key.toLowerCase().equals("shutdown")) {
							rp.shutDown = Boolean.valueOf(val);
						} else if (key.equals("nhtmlfiles")) {
							rp.nHtmlFiles = Integer.valueOf(val);
						} else if (key.equals("dataintervalinminutes")) {
							rp.dataIntervalInMinutes = Integer.valueOf(val);
						} else if (key.equals("htmlwriteintervalinseconds")) {
							rp.htmlWriteIntervalInSeconds = Integer.valueOf(val);
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
		return "RunParams : shutdown = " + shutDown;
	}
}
