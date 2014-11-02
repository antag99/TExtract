package com.github.antag99.textract.extract;

import java.io.File;

public final class Steam {
	private Steam() {
	}

	public static File findTerrariaDirectory() {
		// Check the windows registry for steam installation path
		if (System.getProperty("os.name").toLowerCase().contains("windows")) {
			try {
				String terrariaPath = WinRegistry.readString(WinRegistry.HKEY_LOCAL_MACHINE,
						"SOFTWARE\\Re-Logic\\Terraria", "Install_Path");
				File terrariaDirectory = new File(terrariaPath);
				if (isTerrariaDirectory(terrariaDirectory)) {
					return terrariaDirectory;
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
