package com.ckkeith.monitor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.ArrayList;

public class Main {

	private static String getCredentialsFileName() throws Exception {
		return Utils.getHomeDir() + File.separator + "Documents" + File.separator + "particle-tokens.txt";
	}

	private static ArrayList<String> readCredentials() throws Exception {
		String credentialsFileName = getCredentialsFileName();
		File f = new File(credentialsFileName);
		if (!f.exists()) {
			System.out.println("No credentials file : " + credentialsFileName);
			System.exit(-7);
		}
		ArrayList<String> creds = new ArrayList<String>(10);
		BufferedReader br = new BufferedReader(new FileReader(credentialsFileName));
		try {
			String s;
			while ((s = br.readLine()) != null) {
				creds.add(s);
			}
		} finally {
			br.close();
		}
		return creds;
	}

	public static void main(String[] args) {
		try {
			ArrayList<String> creds = readCredentials();
			for (String c : creds) {
				if (c == null || c.startsWith("#")) {
					continue;
				}
				(new PhotonMonitor(c)).run();
			}
		} catch (Exception e) {
			System.out.println("main() : " + LocalDateTime.now().toString() + "\t" + e.getClass().getName() + " "
					+ e.getMessage());
			e.printStackTrace(new PrintStream(System.out));
		}
	}
}
