package com.ckkeith.monitor;

import java.io.File;
import java.util.ArrayList;

public class RunParams {
	boolean	shutDown = true;
	
	static RunParams load(String filename) {
		RunParams rp = new RunParams();
		try {
			File f = new File(Utils.getParamFileDirectory() + filename);
			if (f.exists()) {
				ArrayList<String> params = Utils.readParameterFile(filename);
				for (String s : params) {
					String p[] = s.split("=");
					if (p.length > 1) {
						p[0] = p[0].trim();
						p[1] = p[1].trim();
						if (p[0].equals("shutdown")) {
							rp.shutDown = Boolean.valueOf(p[1]);
						}
					}
				}				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return rp;
	}
}
