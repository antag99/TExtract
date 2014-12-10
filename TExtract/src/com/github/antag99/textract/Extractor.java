/*******************************************************************************
 * Copyright (c) 2014, Anton Gustafsson
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * 
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * 
 * * Neither the name of TExtract nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package com.github.antag99.textract;

import java.io.File;

import org.apache.commons.io.FileUtils;

import com.github.antag99.textract.extract.XactExtractor;
import com.github.antag99.textract.extract.XnbExtractor;

public class Extractor {
	// private static final Logger logger = LogManager.getLogger(Extractor.class);

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
		statusReporter.reportPercentage(1f / 4);
		statusReporter.reportTask("Extracting Images");
		xnbExtractor.setInputDirectory(new File(contentDirectory, "Images"));
		xnbExtractor.setOutputDirectory(new File(outputDirectory, "Images"));
		xnbExtractor.extract();
		statusReporter.reportPercentage(1f / 2);

		statusReporter.reportTask("Extracting Sounds");
		xnbExtractor.setInputDirectory(new File(contentDirectory, "Sounds"));
		xnbExtractor.setOutputDirectory(new File(outputDirectory, "Sounds"));
		xnbExtractor.extract();
		statusReporter.reportPercentage(1f / 2 + (1f / 2) / 3);

		statusReporter.reportTask("Extracting Fonts");
		xnbExtractor.setInputDirectory(new File(contentDirectory, "Fonts"));
		xnbExtractor.setOutputDirectory(new File(outputDirectory, "Fonts"));
		xnbExtractor.extract();
		statusReporter.reportPercentage(1f / 2 + ((1f / 2) / 3) * 2);

		statusReporter.reportTask("Extracting Music");
		xactExtractor.setInputFile(new File(contentDirectory, "Wave Bank.xwb"));
		xactExtractor.setOutputDirectory(new File(outputDirectory, "Music"));
		xactExtractor.extract();
		statusReporter.reportPercentage(1f);

		try {
			FileUtils.copyInputStreamToFile(ExtractorGUI.class.getResourceAsStream("/fontNotice.txt"), new File(outputDirectory, "Fonts/README.txt"));
		} catch (Throwable t) {
			t.printStackTrace();
		}
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
