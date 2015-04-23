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

import com.github.antag99.textract.extract.XactExtractor;
import com.github.antag99.textract.extract.XnbExtractor;

public class Extractor {
	private XnbExtractor xnbExtractor;
	private XactExtractor xactExtractor;

	private File contentDirectory;
	private File outputDirectory;

	protected StatusReporter statusReporter = StatusReporter.mutedReporter;

	public Extractor() {
		xnbExtractor = new XnbExtractor();
		xactExtractor = new XactExtractor();
	}

	public StatusReporter getStatusReporter() {
		return statusReporter;
	}

	public void setStatusReporter(StatusReporter statusReporter) {
		this.statusReporter = statusReporter;
		xnbExtractor.setStatusReporter(statusReporter);
		xactExtractor.setStatusReporter(statusReporter);
	}

	public void extract() {
		int taskCount = 3;
		int currentTask = 1;

		statusReporter.reportPercentage((float) currentTask++ / (float) taskCount);
		statusReporter.reportTask("Extracting Images");
		xnbExtractor.setInputDirectory(new File(contentDirectory, "Images"));
		xnbExtractor.setOutputDirectory(new File(outputDirectory, "Images"));
		xnbExtractor.extract();

		statusReporter.reportPercentage((float) currentTask++ / (float) taskCount);
		statusReporter.reportTask("Extracting Sounds");
		xnbExtractor.setInputDirectory(new File(contentDirectory, "Sounds"));
		xnbExtractor.setOutputDirectory(new File(outputDirectory, "Sounds"));
		xnbExtractor.extract();

		statusReporter.reportPercentage((float) currentTask++ / (float) taskCount);
		statusReporter.reportTask("Extracting Music");
		xactExtractor.setInputFile(new File(contentDirectory, "Wave Bank.xwb"));
		xactExtractor.setOutputDirectory(new File(outputDirectory, "Music"));
		xactExtractor.extract();
	}

	public void setContentDirectory(File contentDirectory) {
		this.contentDirectory = contentDirectory;
	}

	public File getContentDirectory() {
		return contentDirectory;
	}

	public void setOutputDirectory(File outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	public File getOutputDirectory() {
		return outputDirectory;
	}
}
