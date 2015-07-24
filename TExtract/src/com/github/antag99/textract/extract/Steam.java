/*******************************************************************************
 * Copyright (C) 2014-2015 Anton Gustafsson
 *
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/
package com.github.antag99.textract.extract;

import java.io.File;
import java.util.Map.Entry;

public final class Steam {
	private Steam() {
	}

	public static File findTerrariaDirectory() {
		try {
			String os = System.getProperty("os.name");
			String home = System.getProperty("user.home");
			if (os.contains("Windows")) {
				// Check the windows registry for steam installation path
				try {
					String steamPath = WinRegistry.readString(WinRegistry.HKEY_CURRENT_USER,
							"Software\\Valve\\Steam", "SteamPath");
					File result = seekDirectory(new File(steamPath));
					if (result != null) {
						return result;
					}
				} catch (Throwable ex) {
					// This might fail in the future, as WinRegistry uses internal API
					ex.printStackTrace();
				}
			} else if (os.contains("Linux")) {
				// Steam directory should be ~/.local/share/Steam
				File result = seekDirectory(new File(home, "/.local/share/Steam"));
				if (result != null) {
					return result;
				}
			} else if (os.contains("Mac")) {
				// Steam directory should be ~/Library/Application Support/Steam
				File result = seekDirectory(new File(home, "/Library/Application Support/Steam"));
				if (result != null) {
					return result;
				}
			}

			// Try to find relevant environment variables
			for (Entry<String, String> environmentVariable : System.getenv().entrySet()) {
				if (environmentVariable.getKey().toLowerCase().contains("terraria") ||
						environmentVariable.getKey().toLowerCase().contains("tapi")) {
					File result = seekDirectory(new File(environmentVariable.getValue()));
					if (result != null) {
						return result;
					}
				} else if (environmentVariable.getKey().toLowerCase().contains("steam")) {
					File result = seekDirectory(new File(environmentVariable.getValue()));
					if (result != null) {
						return result;
					}
				}
			}
		} catch (Throwable ex) {
			// Do not fail because of an exception, but log the error
			ex.printStackTrace();
		}

		// If nothing other works, then prompt the user
		return null;
	}

	private static File seekDirectory(File steamDirectory) {
		if (steamDirectory == null || !steamDirectory.isDirectory()) {
			return null;
		}

		File steamApps = new File(steamDirectory, "SteamApps");
		if (!steamApps.exists()) {
			steamApps = new File(steamDirectory, "steamapps");
		}
		if (!steamApps.exists()) {
			steamApps = steamDirectory;
		}
		File common = new File(steamApps, "Common");
		if (!common.exists()) {
			common = new File(steamApps, "common");
		}
		if (!common.exists()) {
			common = steamDirectory;
		}
		File terraria = new File(common, "Terraria");
		if (!terraria.exists()) {
			terraria = steamDirectory;
		}
		File content = new File(terraria, "Content");
		if (!content.exists()) {
			return null;
		}
		return terraria;
	}
}
