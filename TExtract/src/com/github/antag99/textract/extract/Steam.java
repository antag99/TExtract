package com.github.antag99.textract.extract;

import java.io.File;
import java.util.Map.Entry;

public final class Steam {
	private Steam() {
	}

	public static File findTerrariaDirectory() {
		try {
			if (System.getProperty("os.name").toLowerCase().contains("windows")) {
				// Check the windows registry for steam installation path
				try {
					String steamPath = WinRegistry.readString(WinRegistry.HKEY_CURRENT_USER,
							"Software\\Valve\\Steam", "SteamPath");
					File result = seekSteamDirectory(new File(steamPath));
					if (result != null) {
						return result;
					}
				} catch (Throwable ignored) {
				}
			}

			// Try to find steam parent directories
			for (File root : File.listRoots()) {
				// Search inside program & 'game' directories
				for (File rootChild : root.listFiles()) {
					if (rootChild.getName().toLowerCase().contains("program") ||
							rootChild.getName().toLowerCase().contains("game")) {
						File result = seekSteamParent(rootChild);
						if (result != null) {
							return result;
						}
					}
				}

				// Try to find steam directory inside root
				File result = seekSteamParent(root);
				if (result != null) {
					return result;
				}
			}

			// Try to find relevant environment variables
			for (Entry<String, String> environmentVariable : System.getenv().entrySet()) {
				if (environmentVariable.getKey().toLowerCase().contains("terraria") |
						environmentVariable.getKey().toLowerCase().contains("tapi")) {

					File result = seekTerrariaDirectory(new File(environmentVariable.getValue()));
					if (result != null) {
						return result;
					}
				} else if (environmentVariable.getKey().toLowerCase().contains("steam")) {
					File result = seekSteamDirectory(new File(environmentVariable.getValue()));
					if (result != null) {
						return result;
					}
				}
			}
		} catch (Exception ex) {
			// Do not fail because of an exception, but prompt the user and log the error
			ex.printStackTrace();
		}

		// If nothing other works, then prompt the user
		return null;
	}

	private static File seekSteamParent(File parent) {
		if (parent == null || !parent.isDirectory()) {
			return null;
		}

		File[] parentFiles = parent.listFiles();

		if (parentFiles == null) {
			return null;
		}

		for (File child : parentFiles) {
			if (child.getName().toLowerCase().contains("steam")) {
				File result = seekSteamDirectory(child);
				if (result != null) {
					return result;
				}
			}
		}

		return null;
	}

	private static File seekSteamDirectory(File steamDirectory) {
		if (steamDirectory == null || !steamDirectory.isDirectory()) {
			return null;
		}

		File steamApps = new File(steamDirectory, "SteamApps");
		File common = new File(steamApps, "Common");

		// We might've ended up inside a SteamApps directory
		if (!steamApps.exists()) {
			common = new File(steamDirectory, "Common");
		}

		File terraria = new File(common, "Terraria");

		return seekTerrariaDirectory(terraria);
	}

	private static File seekTerrariaDirectory(File terrariaDirectory) {
		if (terrariaDirectory == null || !terrariaDirectory.isDirectory()) {
			return null;
		}

		File contentDirectory = new File(terrariaDirectory, "Content");

		if (contentDirectory.exists()) {
			return terrariaDirectory;
		}

		return null;
	}
}
