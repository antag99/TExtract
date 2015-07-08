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
package com.github.antag99.textract;

import static java.lang.String.format;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.UIManager;

import com.esotericsoftware.minlog.Log;

class TExtract {
	public TExtract() {
	}

	private Integer parseLogLevel(String value) {
		switch (value) {
		case "trace":
			return Log.LEVEL_TRACE;
		case "debug":
			return Log.LEVEL_DEBUG;
		case "info":
			return Log.LEVEL_INFO;
		case "error":
			return Log.LEVEL_ERROR;
		case "none":
			return Log.LEVEL_ERROR;
		default:
			return null;
		}
	}

	public void run(String[] args) {
		List<File> inputs = new ArrayList<File>();
		// null is used for default values
		File outputDirectory = null;
		Integer logLevel = null;
		Boolean logFileEnabled = null;

		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
			case "--help":
			case "-help":
				if (i == 0) {
					Log.info(format("Usage: TExtract [options] [--] [files]"));
					Log.info(format("    --outputDirectory path"));
					Log.info(format("    --logLevel trace|debug|info|error|none"));
					Log.info(format("    --logFile"));
					Log.info(format("    --no-logFile"));
				} else {
					Log.error(format("Invalid option --help"));
				}
				return;
			case "--outputDirectory":
			case "-outputDirectory":
				if (i + 1 == args.length) {
					Log.error(format("Malformed option %s; destination expected", args[i]));
					return;
				} else if (outputDirectory != null) {
					Log.error(format("Duplicate option %s", args[i]));
					return;
				} else {
					outputDirectory = new File(args[++i]);
				}
				break;
			case "--logLevel":
			case "-logLevel":
				if (logLevel != null) {
					Log.error(format("Duplicate option %s", args[i]));
					return;
				}
				if (i + 1 == args.length || (logLevel = parseLogLevel(args[i + 1])) == null) {
					Log.error(format("Malformed option %s; level expected", args[i]));
					return;
				}
				i++;
				break;
			case "--logFile":
			case "-logFile":
				if (logFileEnabled != null) {
					Log.error(format("Duplicate option %s", args[i]));
					return;
				}
				logFileEnabled = true;
				break;
			case "--no-logFile":
			case "-no-logFile":
				if (logFileEnabled != null) {
					Log.error(format("Duplicate option %s", args[i]));
					return;
				}
				logFileEnabled = false;
				break;
			default:
				if ("--".equals(args[i])) {
					i++;
				} else if (args[i].startsWith("-")) {
					Log.error("Unrecognized option " + args[i]);
					return;
				}
				while (i < args.length) {
					inputs.add(new File(args[i++]));
				}
				break;
			}
		}

		Log.set(logLevel != null ? logLevel : Log.LEVEL_TRACE);

		if (outputDirectory == null && inputs.size() == 0) {// GUI
			// Set system-specific look and feel when not on Linux
			if (System.getProperty("os.name").indexOf("Linux") == -1) {
				try {
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				} catch (Throwable ex) {
					ex.printStackTrace();
				}
			}

			ExtractorGUI gui = new ExtractorGUI();
			// Use log file by default
			gui.setLogFileEnabled(logFileEnabled == null || logFileEnabled);
			gui.start();
		} else {// CLI
			Extractor extractor = new Extractor();
			extractor.setStatusReporter(new StatusReporter() {
				@Override
				public void reportTaskStatus(String status) {
					Log.trace(status);
				}

				@Override
				public void reportTaskPercentage(float percentage) {
				}

				@Override
				public void reportOverallStatus(String status) {
					Log.info(status);
				}

				@Override
				public void reportOverallPercentage(float percentage) {
				}
			});
			// Don't use log file by default
			extractor.setLogFileEnabled(logFileEnabled != null && logFileEnabled);
			extractor.getInputFiles().addAll(inputs);
			extractor.setOutputDirectory(outputDirectory);
			extractor.extract();
		}
	}

	public static void main(String[] args) {
		new TExtract().run(args);
	}
}
