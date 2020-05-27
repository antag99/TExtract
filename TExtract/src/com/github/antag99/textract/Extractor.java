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

import com.github.antag99.textract.extract.XactExtractor;
import com.github.antag99.textract.extract.XnbExtractor;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.TeeOutputStream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Extractor {
    private final XnbExtractor xnbExtractor;
    private final XactExtractor xactExtractor;
    private final List<File> inputFiles = new ArrayList<File>();
    private final List<File> filesToExtract = new LinkedList<>();
    private boolean logFileEnabled;
    private File outputDirectory;
    private StatusReporter statusReporter = StatusReporter.mutedReporter;

    // Counters used for percentage bars
    private long processedFiles = 0;
    private long totalFiles = 0;

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

    private void extractFiles() {
        statusReporter.reportOverallStatus("Extracting files...");

        final int cores = Runtime.getRuntime().availableProcessors();
        final int[] threadsRunning = {0};    // One element array such that it can be changed in a thread

        System.out.println("DEBUG: There are " + cores + " available threads for extraction.");

        // Extract all files in list
        while (!this.filesToExtract.isEmpty()) {

            // If cores are available then extract
            if (threadsRunning[0] < cores) {
                // Remove file from list
                final File assetFile = this.filesToExtract.remove(0);
                // Inc threads counter
                threadsRunning[0]++;

                // Start a thread with the extraction of the file
                (new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            statusReporter.reportTaskStatus("Extracting " + assetFile.getName());

                            if (assetFile.getName().endsWith(".xnb")) {
                                xnbExtractor.extract(assetFile, outputDirectory);
                            } else if (assetFile.getName().endsWith(".xwb")) {
                                String directoryName = assetFile.getName().substring(0, assetFile.getName().lastIndexOf('.'));
                                File directory = new File(outputDirectory, directoryName);
                                directory.mkdirs();
                                xactExtractor.extract(assetFile, directory);
                            }
                        } catch (Exception e) {
                            System.err.println("ERROR: Failed to extract " + assetFile.getName());
                            e.printStackTrace();
                        }

                        threadsRunning[0]--;    // Decrement thread counter, (used to calculate threads which are left running)
                        processedFiles++;

                        float percentage = (float) ((double) processedFiles / (double) totalFiles);

                        statusReporter.reportOverallPercentage(percentage);
                        statusReporter.reportTaskPercentage(percentage);
                        System.out.println("INFO: Finished extracting " + assetFile.getName() + " " + percentage + "% complete " + threadsRunning[0] + " threads left running "
                                + filesToExtract.size() + " items left to process.");
                    }
                }, "Extracting " + assetFile.getName())).start();
            }
        }

        // Wait for threads to finish - any hanging threads will cause this code to hang
        while (threadsRunning[0] > 0) {
            statusReporter.reportOverallStatus("DEBUG: Waiting on " + threadsRunning[0] + " threads to terminate");
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        statusReporter.reportTaskStatus("Finished");
        statusReporter.reportOverallStatus("Finished");

        statusReporter.reportOverallPercentage(1);
        statusReporter.reportTaskPercentage(1);

        statusReporter.reportOverallStatus("Finished extraction");
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
                traverse(inputFile, inputFile.getParentFile(), outputDirectory);
            // Gets all files to extract and adds them to the list

            this.totalFiles = this.filesToExtract.size();

            extractFiles(); // Extract the files
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
        int files = 0;
        if (file.isDirectory()) {
            for (File child : file.listFiles())
                files += count(child);
        } else {
            files++;
        }
        return files;
    }

    /**
     * @param inputFile       The input file to traverse
     * @param inputRoot       The root of the input file, for better status messages
     * @param outputDirectory The output directory to put extracted files in
     */
    private void traverse(File inputFile, File inputRoot, File outputDirectory) {
        String relativePath = inputFile.getAbsolutePath().substring(inputRoot.getAbsolutePath().length());
        if (relativePath.length() > 0)
            relativePath = relativePath.substring(1);

        if (inputFile.isDirectory()) {
            outputDirectory = new File(outputDirectory, inputFile.getName());
            File[] files = inputFile.listFiles();
            for (int i = 0; i < files.length; i++) {
                statusReporter.reportOverallStatus("Discovering files in " + relativePath + "/");
                File child = files[i];

                statusReporter.reportTaskPercentage((float) i / (float) files.length);
                statusReporter.reportTaskStatus("Found " + child.getName());

                traverse(child, inputRoot, outputDirectory);
            }
        } else if (inputFile.canRead() && (inputFile.getName().endsWith(".xnb") || inputFile.getName().endsWith(".xwb"))) {
            this.filesToExtract.add(inputFile);
        }
    }

    public StatusReporter getStatusReporter() {
        return statusReporter;
    }

    public void setStatusReporter(StatusReporter statusReporter) {
        this.statusReporter = statusReporter;
    }

    public File getOutputDirectory() {
        return outputDirectory;
    }

    public void setOutputDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
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
