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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.TeeOutputStream;

import com.github.antag99.textract.extract.XactExtractor;
import com.github.antag99.textract.extract.XnbExtractor;

public class Extractor {
	private XnbExtractor xnbExtractor;
	private XactExtractor xactExtractor;
	private boolean logFileEnabled;
	private File outputDirectory;
	private List<File> inputFiles = new ArrayList<File>();

	private StatusReporter statusReporter = StatusReporter.mutedReporter;

	// Counters used for percentage bars
	private long processedBytes = 0;
	private long totalBytes = 0;

	public Extractor() {
		xnbExtractor = new XnbExtractor();
		xactExtractor = new XactExtractor() {
			@Override
			protected void status(String status) {
				statusReporter.reportTaskStatus(status);
			}

			@Override
			protected void percentage(float percentage) {
				statusReporter.reportTaskPercentage(percentage);
			}
		};
	}

	public void extract() {
		PrintStream stdOut = System.out;
		PrintStream stdErr = System.err;
		FileOutputStream logFile = null;
		try {
			File outputDirectory = this.outputDirectory;
			// If output directory is not specified, default to working directory
			if (outputDirectory == null) {
				outputDirectory = new File(".");
			}

			if (logFileEnabled) {
				try {
					outputDirectory.mkdirs();
					logFile = new FileOutputStream(new File(outputDirectory, "TExtract.log"));
					System.setOut(new PrintStream(new TeeOutputStream(stdOut, logFile)));
					System.setErr(new PrintStream(new TeeOutputStream(stdErr, logFile)));
				} catch (IOException ex) {
					ex.printStackTrace();
					return;
				}
			}

			for (File inputFile : inputFiles)
				totalBytes += count(inputFile);

			for (File inputFile : inputFiles)
				traverse(inputFile, inputFile.getParentFile(), outputDirectory);
		} finally {
			if (logFileEnabled) {
				System.setOut(stdOut);
				System.setErr(stdErr);
				IOUtils.closeQuietly(logFile);
			}
		}
	}

	/**
	 * @param file The file to compute the size of
	 */
	private int count(File file) {
		int bytes = 0;
		if (file.isDirectory()) {
			for (File child : file.listFiles())
				bytes += child.length();
		} else {
			bytes += file.length();
		}
		return bytes;
	}

	/**
	 * @param inputFile The input file to traverse
	 * @param inputRoot The root of the input file, for better status messages
	 * @param outputDirectory The output directory to put extracted files in
	 */
	private void traverse(File inputFile, File inputRoot, File outputDirectory) {
		String relativePath = inputFile.getAbsolutePath().substring(inputRoot.getAbsolutePath().length());
		if (relativePath.length() > 0)
			relativePath = relativePath.substring(1);

		if (inputFile.isDirectory()) {
			statusReporter.reportOverallStatus("Extracting files from " + relativePath + "/*");
			outputDirectory = new File(outputDirectory, inputFile.getName());
			File[] files = inputFile.listFiles();
			for (int i = 0; i < files.length; i++) {
				File child = files[i];

				statusReporter.reportTaskPercentage((float) i / (float) files.length);
				statusReporter.reportTaskStatus(child.getName());

				statusReporter.reportOverallPercentage((float) ((double) processedBytes / (double) totalBytes));
				processedBytes += child.length();

				traverse(child, inputRoot, outputDirectory);
			}
		} else {
			if (inputFile.getName().endsWith(".xnb")) {
				outputDirectory.mkdirs();
				try {
					xnbExtractor.extract(inputFile, outputDirectory);
				} catch (IOException ex) {
					throw new RuntimeException("An unexpected I/O error has occured", ex);
				}
			} else if (inputFile.getName().endsWith(".xwb")) {
				statusReporter.reportOverallStatus("Extracting files from " +
						(relativePath.length() > 0 ? relativePath + "/" : ""));
				try {
					String directoryName = inputFile.getName().substring(0, inputFile.getName().lastIndexOf('.'));
					File directory = new File(outputDirectory, directoryName);
					directory.mkdirs();
					xactExtractor.extract(inputFile, directory);
				} catch (IOException ex) {
					throw new RuntimeException("An unexpected I/O error has occured", ex);
				}
			}
		}
	}

	public StatusReporter getStatusReporter() {
		return statusReporter;
	}

	public void setStatusReporter(StatusReporter statusReporter) {
		this.statusReporter = statusReporter;
	}

	public void setOutputDirectory(File outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	public File getOutputDirectory() {
		return outputDirectory;
	}

	public List<File> getInputFiles() {
		return inputFiles;
	}

	public boolean isLogFileEnabled() {
		return logFileEnabled;
	}

	public void setLogFileEnabled(boolean logFileEnabled) {
		this.logFileEnabled = logFileEnabled;
	}
}
