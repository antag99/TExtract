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
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import com.esotericsoftware.minlog.Log;

class Ffmpeg {
	private static final String cmd;
	private static final boolean isWindows = System.getProperty("os.name").toLowerCase().contains("windows");

	static {
		// Non-windows users will have to install ffmpeg
		String tmpCmd = "ffmpeg";

		if (isWindows) {
			File ffmpegExecutable = null;
			try {
				// Try to create a temporary file for the executable
				ffmpegExecutable = File.createTempFile("ffmpeg", ".exe");
			} catch (IOException ex) {
				// Dump the executable in the local directory instead
				ffmpegExecutable = new File("ffmpeg.exe");
			}

			URL ffmpegExecutableResource = Ffmpeg.class.getResource("/ffmpeg.exe");

			if (ffmpegExecutableResource == null) {
				throw new RuntimeException("ffmpeg.exe not found in classpath");
			}

			try {
				FileUtils.copyURLToFile(ffmpegExecutableResource, ffmpegExecutable);

				tmpCmd = ffmpegExecutable.getAbsolutePath();
			} catch (Throwable ex) {
				Log.error("Failed to copy ffmpeg executable!", ex);
				// We can still try, the user might have ffmpeg installed
			}
		}

		cmd = tmpCmd;
	}

	public static void convert(File input, File output) {
		/*
		 * Note: From version 1.4, TExtract uses a special version of the
		 * ffmpeg executable configured with the following options:
		 * --disable-everything --enable-muxer=wav --enable-encoder=pcm_s16le
		 * --enable-demuxer=xwma --enable-decoder=wmav2
		 * --enable-protocol=file --enable-filter=aresample
		 * It can therefore not resample to another format without
		 * recompilation with appropriate options. The reason behind this
		 * is that the original weighted about 27 megabytes, whereas the new
		 * one weights only 1,5 megabytes.
		 */
		List<String> command = new ArrayList<String>();
		command.add(cmd);
		command.add("-i");
		command.add(FilenameUtils.separatorsToSystem(FilenameUtils.normalize(input.getAbsolutePath())));
		command.add("-acodec");
		command.add("pcm_s16le");
		command.add("-nostdin");
		command.add("-ab");
		command.add("128k");
		command.add(FilenameUtils.separatorsToSystem(FilenameUtils.normalize(output.getAbsolutePath())));

		ProcessBuilder builder = new ProcessBuilder(command);

		try {
			Process process = builder.start();
			if (process.waitFor() != 0) {
				Log.error("Ffmpeg exited with abnormal exit code: " + process.exitValue());
			}
			Log.debug(IOUtils.toString(process.getErrorStream()));
			Log.debug(IOUtils.toString(process.getInputStream()));
		} catch (Throwable ex) {
			Log.error("An error occured when executing ffmpeg", ex);
		}
	}
}
