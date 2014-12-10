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
package com.github.antag99.textract.extract;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

class Ffmpeg {
	private static final Logger logger = LogManager.getLogger(Ffmpeg.class);

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
			} catch (IOException ex) {
				logger.error("Failed to copy ffmpeg executable!");
				// We can still try, the user might have ffmpeg installed
			}
		}

		cmd = tmpCmd;
	}

	public static void convert(File input, File output) {
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
				logger.error("Ffmpeg exited with abnormal exit code: " + process.exitValue());
			}
			IOUtils.copy(process.getErrorStream(), System.err);
			IOUtils.copy(process.getInputStream(), System.out);
		} catch (Throwable ex) {
			logger.error("An error has occured when executing ffmpeg", ex);
		}
	}
}
