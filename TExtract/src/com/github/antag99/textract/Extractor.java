package com.github.antag99.textract;

import java.io.File;

import org.apache.commons.io.FileUtils;

import com.github.antag99.textract.extract.XactExtractor;
import com.github.antag99.textract.extract.XnbExtractor;

public class Extractor {
//	private static final Logger logger = LogManager.getLogger(Extractor.class);
	
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
		} catch(Throwable t) {
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
