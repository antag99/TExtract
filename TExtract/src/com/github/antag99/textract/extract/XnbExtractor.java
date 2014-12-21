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
import java.io.FileFilter;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.ImageLineByte;
import ar.com.hjg.pngj.PngWriter;

import com.github.antag99.textract.StatusReporter;

public class XnbExtractor {
	private static final Logger logger = LogManager.getLogger(XnbExtractor.class);

	private static final int SURFACEFORMAT_COLOR = 0;
	private static final int HEADER_SIZE = 14;

	// WAV Encoding
	private static final byte[] RIFF = "RIFF".getBytes(Charset.forName("UTF-8"));
	private static final byte[] WAVE = "WAVE".getBytes(Charset.forName("UTF-8"));
	// Note the space after fmt.
	private static final byte[] fmt = "fmt ".getBytes(Charset.forName("UTF-8"));
	private static final byte[] data = "data".getBytes(Charset.forName("UTF-8"));
	private static final int wavHeaderSize = RIFF.length + 4 + WAVE.length + fmt.length + 4 + 2 + 2 + 4 + 4 + 2 + 2 + data.length + 4;

	private ByteBuffer writeBuffer = ByteBuffer.allocate(wavHeaderSize);

	private File inputDirectory;
	private File outputDirectory;

	private LzxDecoder lzxDecoder;
	private StatusReporter statusReporter = StatusReporter.mutedReporter;

	public XnbExtractor() {
		lzxDecoder = new LzxDecoder();
		writeBuffer.order(ByteOrder.LITTLE_ENDIAN);
	}

	public StatusReporter getStatusReporter() {
		return statusReporter;
	}

	public void setStatusReporter(StatusReporter statusReporter) {
		this.statusReporter = statusReporter;
	}

	public File getInputDirectory() {
		return inputDirectory;
	}

	public void setInputDirectory(File inputDirectory) {
		this.inputDirectory = inputDirectory;
	}

	public File getOutputDirectory() {
		return outputDirectory;
	}

	public void setOutputDirectory(File outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	public void extract() {
		logger.debug("Extracting XNB files");
		logger.debug("Input directory: " + inputDirectory.getAbsolutePath());
		logger.debug("Output directory: " + outputDirectory.getAbsolutePath());

		outputDirectory.mkdirs();

		FileFilter filter = new FileFilter() {
			@Override
			public boolean accept(File file) {
				return file.getName().endsWith(".xnb");
			}
		};

		File[] xnbFiles = inputDirectory.listFiles(filter);

		for (int i = 0; i < xnbFiles.length; ++i) {
			File file = xnbFiles[i];
			statusReporter.reportTaskStatus("Extracting " + file.getName());
			statusReporter.reportTaskPercentage((float) i / (float) xnbFiles.length);

			try {
				ByteBuffer buffer = ByteBuffer.wrap(FileUtils.readFileToByteArray(file));
				buffer.order(ByteOrder.LITTLE_ENDIAN);

				if (buffer.get() != 'X' || buffer.get() != 'N' || buffer.get() != 'B') {
					logger.error(file.getName() + " wasn't a XNB file, skipping!");
					continue;
				}

				// Ignore target platform, it shouldn't matter
				if (buffer.get() != 'w') {
					logger.warn("Unrecognized target platform");
				}

				if (buffer.get() != 5) {
					logger.warn("Unexpected XNA version");
				}

				boolean compressed = (buffer.get() & 0x80) != 0;

				int compressedSize = buffer.getInt();
				int decompressedSize = compressed ? buffer.getInt() : compressedSize;

				if (compressed) {
					ByteBuffer decompressedBuffer = ByteBuffer.allocate(decompressedSize);
					decompressedBuffer.order(ByteOrder.LITTLE_ENDIAN);

					lzxDecoder.decompress(buffer, compressedSize - HEADER_SIZE, decompressedBuffer, decompressedSize);

					decompressedBuffer.position(0);

					buffer = decompressedBuffer;
				} else {
					// Terraria's XNB files are compressed by default
					logger.warn(file.getName() + " is uncompressed");
				}

				int typeReaderCount = Xnb.get7BitEncodedInt(buffer);

				// The first type reader is used for reading the primary asset
				String typeReaderName = Xnb.getCSharpString(buffer);
				// The type reader version - Dosen't matter
				buffer.getInt();

				// Type reader names MIGHT contain assembly information
				int assemblyInformationIndex = typeReaderName.indexOf(',');
				if (assemblyInformationIndex != -1)
					typeReaderName = typeReaderName.substring(0, assemblyInformationIndex);

				// Skip the remaining type readers, as all types are known
				for (int k = 1; k < typeReaderCount; k++) {
					Xnb.getCSharpString(buffer);
					buffer.getInt();
				}

				if (Xnb.get7BitEncodedInt(buffer) != 0) {
					logger.error("Shared resources are not supported");
					continue;
				}

				if (Xnb.get7BitEncodedInt(buffer) != 1) {
					logger.error("Primary asset is invalid");
					continue;
				}

				String fileNameWithoutExtension = file.getName().substring(0, file.getName().indexOf('.'));

				// Switch on the type reader name, excluding assembly information
				switch (typeReaderName) {
				case "Microsoft.Xna.Framework.Content.Texture2DReader": {
					int surfaceFormat = buffer.getInt();

					final int width = buffer.getInt();
					final int height = buffer.getInt();

					// Mip count
					int mipCount = buffer.getInt();
					// Size
					int size = buffer.getInt();

					if (mipCount != 1) {
						logger.warn("Unexpected mipCount: " + mipCount);
					}

					if (size != width * height * 4) {
						logger.warn("Unexpected size: " + size);
					}

					if (surfaceFormat != SURFACEFORMAT_COLOR) {
						logger.error("Unsupported surface format: " + surfaceFormat);
						continue;
					}

					File output = new File(outputDirectory, fileNameWithoutExtension + ".png");

					ImageInfo imageInfo = new ImageInfo(width, height, 8, true);
					PngWriter writer = new PngWriter(output, imageInfo);

					byte[] scanline = new byte[width * 4];
					ImageLineByte imageLine = new ImageLineByte(imageInfo, scanline);

					for (int y = 0; y < height; ++y) {
						buffer.get(scanline);
						writer.writeRow(imageLine);
					}

					writer.close();
					break;
				}
				case "Microsoft.Xna.Framework.Content.SoundEffectReader": {
					if (buffer.getInt() != 18) {
						logger.error("Unimplemented audio format");
						continue;
					}

					if (buffer.getShort() != 1) {
						logger.error("Unimplemented wav codec");
						continue;
					}

					int channels = buffer.getShort() & 0xffff;
					int samplesPerSecond = buffer.getInt();
					int averageBytesPerSecond = buffer.getInt();
					int blockAlign = buffer.getShort() & 0xffff;
					int bitsPerSample = buffer.getShort() & 0xffff;
					buffer.getShort(); // Unknown
					int dataChunkSize = buffer.getInt();

					// Note that the samples are written directly from the source buffer

					writeBuffer.position(0);
					writeBuffer.put(RIFF);
					writeBuffer.putInt(dataChunkSize + 36);
					writeBuffer.put(WAVE);
					writeBuffer.put(fmt);
					writeBuffer.putInt(16);
					writeBuffer.putShort((short) 1);
					writeBuffer.putShort((short) channels);
					writeBuffer.putInt(samplesPerSecond);
					writeBuffer.putInt(averageBytesPerSecond);
					writeBuffer.putShort((short) blockAlign);
					writeBuffer.putShort((short) bitsPerSample);
					writeBuffer.put(data);
					writeBuffer.putInt(dataChunkSize);

					File output = new File(outputDirectory, fileNameWithoutExtension + ".wav");

					OutputStream out = FileUtils.openOutputStream(output);

					// Write header
					out.write(writeBuffer.array(), writeBuffer.arrayOffset(), writeBuffer.position());
					// Write samples
					out.write(buffer.array(), buffer.arrayOffset() + buffer.position(), dataChunkSize);

					out.close();
					break;
				}
				default: {
					logger.error("Unknown primary asset type: " + typeReaderName);
					continue;
				}
				}
			} catch (Throwable ex) {
				logger.error("An error occured when extracting " + file.getName(), ex);
			}
		}
	}
}
