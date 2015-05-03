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

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.TeeOutputStream;

import com.github.antag99.textract.extract.XactExtractor;
import com.github.antag99.textract.extract.XnbExtractor;

public class Extractor {
	private XnbExtractor xnbExtractor;
	private XactExtractor xactExtractor;

	private File inputDirectory;
	private File outputDirectory;

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
			try {
				outputDirectory.mkdirs();
				logFile = new FileOutputStream(new File(outputDirectory, "TExtract.log"));
				System.setOut(new PrintStream(new TeeOutputStream(stdOut, logFile)));
				System.setErr(new PrintStream(new TeeOutputStream(stdErr, logFile)));
			} catch (IOException ex) {
				ex.printStackTrace();
				return;
			}

			totalBytes = count(inputDirectory);

			System.out.println("Input Directory: " + inputDirectory.getAbsolutePath());
			System.out.println("Output Directory: " + outputDirectory.getAbsolutePath());
			System.out.println("Total Bytes: " + totalBytes);

			traverse(inputDirectory, outputDirectory);
		} finally {
			System.setOut(stdOut);
			System.setErr(stdErr);
			IOUtils.closeQuietly(logFile);
		}
	}

	/**
	 * @param directory The directory to count files inside
	 */
	private int count(File directory) {
		File[] files = directory.listFiles();
		int bytes = 0;
		for (File file : files)
			if (file.isFile())
				bytes += file.length();
			else
				bytes += count(file);
		return bytes;
	}

	/**
	 * @param input The input directory to traverse
	 * @param output The output directory corresponding to the input directory
	 */
	private void traverse(File input, File output) {
		String relativePath = input.getAbsolutePath().substring(inputDirectory.getAbsolutePath().length());
		if (relativePath.length() > 0)
			relativePath = relativePath.substring(1);
		statusReporter.reportOverallStatus("Extracting files from " + relativePath + "/");

		File[] files = input.listFiles();

		for (int i = 0; i < files.length; ++i) {
			File file = files[i];

			statusReporter.reportTaskPercentage((float) i / (float) files.length);
			statusReporter.reportTaskStatus(file.getName());

			statusReporter.reportOverallPercentage((float) ((double) processedBytes / (double) totalBytes));
			processedBytes += file.length();

			if (file.getName().endsWith(".xnb")) {
				output.mkdirs();
				try {
					xnbExtractor.extract(file, output);
				} catch (IOException ex) {
					throw new RuntimeException("An unexpected I/O error has occured", ex);
				}
			} else if (file.getName().endsWith(".xwb")) {
				statusReporter.reportOverallStatus("Extracting files from " +
						(relativePath.length() > 0 ? relativePath + "/" : "") + file.getName());
				try {
					String directoryName = file.getName().substring(0, file.getName().lastIndexOf('.'));
					File directory = new File(output, directoryName);
					directory.mkdirs();
					xactExtractor.extract(file, directory);
				} catch (IOException ex) {
					throw new RuntimeException("An unexpected I/O error has occured", ex);
				}

				// Restore status message
				statusReporter.reportOverallStatus("Extracting files from " + relativePath);
			} else if (file.isDirectory()) {
				traverse(file, new File(output, file.getName()));

				// Restore status message
				statusReporter.reportOverallStatus("Extracting files from " + relativePath);
			}
		}
	}

	public StatusReporter getStatusReporter() {
		return statusReporter;
	}

	public void setStatusReporter(StatusReporter statusReporter) {
		this.statusReporter = statusReporter;
	}

	public void setInputDirectory(File inputDirectory) {
		this.inputDirectory = inputDirectory;
	}

	public File getInputDirectory() {
		return inputDirectory;
	}

	public void setOutputDirectory(File outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	public File getOutputDirectory() {
		return outputDirectory;
	}
}
