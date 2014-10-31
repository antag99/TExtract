package com.github.antag99.textract.extract;

import java.io.File;

public final class Steam {
	private Steam() {
	}

	public static File findTerrariaDirectory() {
		// Check the windows registry for steam installation path
		if (System.getProperty("os.name").toLowerCase().contains("windows")) {
			try {
				String steamPath = WinRegistry.readString(WinRegistry.HKEY_CURRENT_USER,
						"Software\\Valve\\Steam", "SteamPath");
				File steamDirectory = new File(steamPath);
				if (isTerrariaDirectory(steamDirectory)) {
					return steamDirectory;
				}
			} catch (Throwable ignored) {
			}
		}

		// Else prompt the user
		return null;
	}

	public static boolean isTerrariaDirectory(File terrariaDirectory) {
		if (!terrariaDirectory.isDirectory()) {
			return false;
		}

		File terrariaExe = new File(terrariaDirectory, "Terraria.exe");
		File contentDirectory = new File(terrariaDirectory, "Content");

		return terrariaExe.exists() && contentDirectory.exists();
	}
}
