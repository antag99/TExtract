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
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.ImageLineByte;
import ar.com.hjg.pngj.PngWriter;

/**
 * Extracts XNB files to a modifiable format. Currently,
 * only certain textures and sound effects are supported,
 * font and shader effects are simply ignored. Other types
 * raise an error.
 */
public class XnbExtractor {
	private static final int SURFACEFORMAT_COLOR = 0;
	private static final int HEADER_SIZE = 14;

	// WAV Encoding
	private static final byte[] RIFF = "RIFF".getBytes(Charset.forName("UTF-8"));
	private static final byte[] WAVE = "WAVE".getBytes(Charset.forName("UTF-8"));
	// Note the space after fmt.
	private static final byte[] fmt = "fmt ".getBytes(Charset.forName("UTF-8"));
	private static final byte[] data = "data".getBytes(Charset.forName("UTF-8"));
	private static final int wavHeaderSize = RIFF.length + 4 + WAVE.length + fmt.length + 4 + 2 + 2 + 4 + 4 + 2 + 2 + data.length + 4;

	private LzxDecoder lzxDecoder;

	public XnbExtractor() {
		lzxDecoder = new LzxDecoder();
	}

	/**
	 * @param inputFile The XNB file to extract
	 * @param outputDirectory The output directory to put the extracted file(s) into
	 * @throws XnbException If the input file was malformed or used unsupported features.
	 * @throws IOException If an I/O error occurs
	 */
	public void extract(File inputFile, File outputDirectory) throws XnbException, IOException {
		ByteBuffer buffer = ByteBuffer.wrap(FileUtils.readFileToByteArray(inputFile));
		buffer.order(ByteOrder.LITTLE_ENDIAN);

		// Check that this file is really an xnb file
		if (buffer.get() != 'X' || buffer.get() != 'N' || buffer.get() != 'B') {
			throw new XnbException("not an XNB file: " + inputFile.getName());
		}

		// Ignore target platform, it shouldn't matter
		@SuppressWarnings("unused")
		int targetPlatform = buffer.get();

		int version = buffer.get();
		if (version != 5) {
			throw new XnbException("unsupported XNB version: " + version);
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

		// Shared resources are unused by Terraria assets
		if (Xnb.get7BitEncodedInt(buffer) != 0) {
			throw new XnbException("shared resources are not supported");
		}

		if (Xnb.get7BitEncodedInt(buffer) != 1) {
			throw new XnbException("primary asset is null; this shouldn't happen");
		}

		String xnbFileName = inputFile.getName();
		String baseFileName = xnbFileName.substring(0, xnbFileName.lastIndexOf('.'));

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
				throw new XnbException("unexpected mipCount: " + mipCount);
			}

			if (size != width * height * 4) {
				throw new XnbException("unexpected size: " + size);
			}

			if (surfaceFormat != SURFACEFORMAT_COLOR) {
				throw new XnbException("unexpected surface format: " + surfaceFormat);
			}

			FileOutputStream output = FileUtils.openOutputStream(new File(outputDirectory, baseFileName + ".png"));

			ImageInfo imageInfo = new ImageInfo(width, height, 8, true);
			PngWriter writer = new PngWriter(output, imageInfo);

			byte[] scanline = new byte[width * 4];
			ImageLineByte imageLine = new ImageLineByte(imageInfo, scanline);

			for (int y = 0; y < height; ++y) {
				buffer.get(scanline);
				writer.writeRow(imageLine);
			}

			writer.end();
			break;
		}
		case "Microsoft.Xna.Framework.Content.SoundEffectReader": {
			int audioFormat = buffer.getInt();
			if (audioFormat != 18) {
				throw new XnbException("unimplemented audio format: " + audioFormat);
			}

			int wavCodec = buffer.getShort();
			if (wavCodec != 1) {
				throw new XnbException("unimplemented wav codec: " + wavCodec);
			}

			int channels = buffer.getShort() & 0xffff;
			int samplesPerSecond = buffer.getInt();
			int averageBytesPerSecond = buffer.getInt();
			int blockAlign = buffer.getShort() & 0xffff;
			int bitsPerSample = buffer.getShort() & 0xffff;
			buffer.getShort(); // Unknown
			int dataChunkSize = buffer.getInt();

			// Note that the samples are written directly from the source buffer

			// Create format header
			ByteBuffer writeBuffer = ByteBuffer.allocate(wavHeaderSize);
			writeBuffer.order(ByteOrder.LITTLE_ENDIAN);
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

			FileOutputStream output = FileUtils.openOutputStream(new File(outputDirectory, baseFileName + ".wav"));

			// Write header
			output.write(writeBuffer.array(), writeBuffer.arrayOffset(), writeBuffer.position());
			// Write samples
			output.write(buffer.array(), buffer.arrayOffset() + buffer.position(), dataChunkSize);

			output.close();
			break;
		}
		case "Microsoft.Xna.Framework.Content.SpriteFontReader":
		case "Microsoft.Xna.Framework.Content.EffectReader": {
			return;
		}
		default: {
			throw new XnbException("unsupported asset type: " + typeReaderName);
		}
		}
	}
}
