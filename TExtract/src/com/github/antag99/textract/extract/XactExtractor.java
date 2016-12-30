
/*******************************************************************************
 * Copyright (C) 2014-2016 Anton Gustafsson
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

/*
  MIT License
  Copyright © 2006 The Mono.Xna Team
  All rights reserved.

  Permission is hereby granted, free of charge, to any person obtaining a copy
  of this software and associated documentation files (the "Software"), to deal
  in the Software without restriction, including without limitation the rights
  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  copies of the Software, and to permit persons to whom the Software is
  furnished to do so, subject to the following conditions:

  The above copyright notice and this permission notice shall be included in all
  copies or substantial portions of the Software.

  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  SOFTWARE.
*/

package com.github.antag99.textract.extract;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;

/**
 * Extracts music files. </p> Can be extended to provide logging support by
 * overriding the {@link #percentage(float)} and {@link #status(String)} methods.
 */
public class XactExtractor {
	// XWB parsing was adapted from MonoGame

	// Track codecs
	static final int MiniFormatTag_PCM = 0x0;
	static final int MiniFormatTag_XMA = 0x1;
	static final int MiniFormatTag_ADPCM = 0x2;
	static final int MiniFormatTag_WMA = 0x3;

	static final int Flag_Compact = 0x00020000;

	// WAV Encoding
	private static final byte[] RIFF = "RIFF".getBytes(Charset.forName("UTF-8"));
	private static final byte[] WAVE = "WAVE".getBytes(Charset.forName("UTF-8"));
	// Note the space after fmt.
	private static final byte[] fmt = "fmt ".getBytes(Charset.forName("UTF-8"));
	private static final byte[] data = "data".getBytes(Charset.forName("UTF-8"));
	private static final int wavHeaderSize = RIFF.length + 4 + WAVE.length + fmt.length + 4 + 2 + 2 + 4 + 4 + 2 + 2 + data.length + 4;

	/** Mapping of music wave bank indexes to their names */
	static final String[] trackNames = {
			"01_OverworldNight",
			"02_Eerie",
			"03_OverworldDay",
			"04_Boss1",
			"05_TitleScreen",
			"06_Jungle",
			"07_Corruption",
			"08_Hallow",
			"09_UndergroundCorruption",
			"10_UndergroundHallow",
			"11_Boss2",
			"12_Underground",
			"13_Boss3",
			"14_Snow",
			"15_Space",
			"16_Crimson",
			"17_Golem",
			"18_AlternateDay",
			"19_Rain",
			"20_UndergroundSnow",
			"21_Desert",
			"22_Ocean",
			"23_Dungeon",
			"24_Plantera",
			"25_QueenBee",
			"26_Lizhard",
			"27_Eclipse",
			"28_RainAmbience",
			"29_Mushrooms",
			"30_PumpkinMoon",
			"31_AlternateUnderground",
			"32_FrostMoon",
			"33_UndergroundCrimson",
			"34_LunarBoss",
			"35_PirateInvasion",
			"36_Underworld",
			"37_MartianMadness",
			"38_MoonLord",
			"39_GoblinArmy",
			"40_Sandstorm",
			"41_OldOnesArmy"
	};

	public XactExtractor() {
	}

	protected void status(String status) {
	}

	protected void percentage(float percentage) {
	}

	/**
	 * @param inputFile The XWB file to extract
	 * @param outputDirectory The directory to put the extracted files inside
	 * @param statusReporter The status reporter to use for reporting which tracks that are currently extracted.
	 * 
	 * @throws XnbException If the XWB file was malformed
	 * @throws IOException If an I/O error occurs
	 */
	public void extract(File inputFile, File outputDirectory) throws XnbException, IOException {
		status("Parsing XWB file header");
		percentage(0f);

		ByteBuffer buffer = ByteBuffer.wrap(FileUtils.readFileToByteArray(inputFile));
		buffer.order(ByteOrder.LITTLE_ENDIAN);

		int Format = 0;
		int PlayRegionLength = 0;
		int PlayRegionOffset = 0;

		int wavebank_offset = 0;

		if (buffer.get() != 'W' || buffer.get() != 'B' ||
				buffer.get() != 'N' || buffer.get() != 'D') {
			throw new XnbException("not an XWB file: " + inputFile.getName());
		}

		int Version = buffer.getInt();

		// Skip trailing bytes of the version
		buffer.getInt();

		if (Version != 46) {
			throw new XnbException("unsupported version: " + Version);
		}

		int[] segmentOffsets = new int[5];
		int[] segmentLengths = new int[5];

		for (int i = 0; i < 5; i++) {
			segmentOffsets[i] = buffer.getInt();
			segmentLengths[i] = buffer.getInt();
		}

		buffer.position(segmentOffsets[0]);

		int Flags = buffer.getInt();
		int EntryCount = buffer.getInt();

		// Skip terraria's wave bank's name. "Wave Bank".
		buffer.position(buffer.position() + 64);

		int EntryMetaDataElementSize = buffer.getInt();
		buffer.getInt(); // EntryNameElementSize
		buffer.getInt(); // Alignment
		wavebank_offset = segmentOffsets[1];

		if ((Flags & Flag_Compact) != 0) {
			throw new XnbException("compact wavebanks are not supported");
		}

		int playregion_offset = segmentOffsets[4];
		for (int current_entry = 0; current_entry < EntryCount; current_entry++) {
			String track = current_entry < trackNames.length ? trackNames[current_entry] : (current_entry + 1) + "_Unknown";

			status("Extracting " + track);
			percentage(0.1f + (0.9f / EntryCount) * current_entry);

			buffer.position(wavebank_offset);
			if (EntryMetaDataElementSize >= 4)
				buffer.getInt(); // FlagsAndDuration
			if (EntryMetaDataElementSize >= 8)
				Format = buffer.getInt();
			if (EntryMetaDataElementSize >= 12)
				PlayRegionOffset = buffer.getInt();
			if (EntryMetaDataElementSize >= 16)
				PlayRegionLength = buffer.getInt();
			if (EntryMetaDataElementSize >= 20)
				buffer.getInt(); // LoopRegionOffset
			if (EntryMetaDataElementSize >= 24)
				buffer.getInt(); // LoopRegionLength

			wavebank_offset += EntryMetaDataElementSize;
			PlayRegionOffset += playregion_offset;

			int codec = (Format) & ((1 << 2) - 1);
			int chans = (Format >> (2)) & ((1 << 3) - 1);
			int rate = (Format >> (2 + 3)) & ((1 << 18) - 1);
			int align = (Format >> (2 + 3 + 18)) & ((1 << 8) - 1);

			buffer.position(PlayRegionOffset);
			byte[] audiodata = new byte[PlayRegionLength];
			buffer.get(audiodata);

			// The codecs used by Terraria are currently xWMA and ADPCM.
			// The xWMA format is not supported by FNA, so it's only used
			// on Windows. This implementation uses ffmpeg to convert the raw
			// xWMA data to WAVE; a minified Windows executable is embedded.
			// PCM was introduced for the last tracks in the 1.3.3 update.
			if (codec == MiniFormatTag_PCM) {
				ByteBuffer writeBuffer = ByteBuffer.allocate(wavHeaderSize);
				writeBuffer.order(ByteOrder.LITTLE_ENDIAN);
				writeBuffer.put(RIFF); // chunk id
				writeBuffer.putInt(audiodata.length + 36); // chunk size
				writeBuffer.put(WAVE); // RIFF type
				writeBuffer.put(fmt); // chunk id
				writeBuffer.putInt(16); // format header size
				writeBuffer.putShort((short) 1); // format (PCM)
				writeBuffer.putShort((short) chans); // channels
				writeBuffer.putInt(rate); // samples per second
				int bitsPerSample = 16;
				int blockAlign = (bitsPerSample / 8) * chans;
				writeBuffer.putInt(rate * blockAlign); // byte rate/ average bytes per second
				writeBuffer.putShort((short) blockAlign);
				writeBuffer.putShort((short) bitsPerSample);
				writeBuffer.put(data); // chunk id
				writeBuffer.putInt(audiodata.length); // data size

				FileOutputStream output = new FileOutputStream(new File(outputDirectory, track + ".wav"));
				output.write(writeBuffer.array(), writeBuffer.arrayOffset(), writeBuffer.position());
				output.write(audiodata);
				output.close();
			} else if (codec == MiniFormatTag_WMA) {
				// Note that it could still be another codec than xWma,
				// but that scenario isn't handled here.

				// This part has been ported from XWMA-to-pcm-u8
				// Not the most beautiful code in the world,
				// but it does the job.

				// I do not know if this code outputs valid XWMA files,
				// but FFMPEG accepts them so it's all right for this usage.

				File xWmaFile = new File(outputDirectory, track + ".wma");

				FileOutputStream xWmaOutput = FileUtils.openOutputStream(xWmaFile);
				// xWmaOutput.write(output.array(), output.arrayOffset(), output.position());

				BufferWriter output = new BufferWriter(xWmaOutput);
				output.setOrder(ByteOrder.LITTLE_ENDIAN);
				output.writeBytes("RIFF".getBytes(Charset.forName("UTF-8")));
				// int odRIChunkSize = output.position();
				output.writeInt(0); // Full file size, ignored by ffmpeg
				output.writeBytes("XWMA".getBytes(Charset.forName("UTF-8")));
				output.writeBytes("fmt ".getBytes(Charset.forName("UTF-8")));
				output.writeInt(18);
				output.writeShort((short) 0x161);
				output.writeShort((short) chans);
				output.writeInt(rate);

				int[] wmaAverageBytesPerSec = new int[] { 12000, 24000, 4000, 6000, 8000, 20000 };
				int[] wmaBlockAlign = new int[] { 929, 1487, 1280, 2230, 8917, 8192, 4459, 5945,
						2304, 1536, 1485, 1008, 2731, 4096, 6827, 5462 };

				int averageBytesPerSec = align > wmaAverageBytesPerSec.length ? wmaAverageBytesPerSec[align >> 5] : wmaAverageBytesPerSec[align];

				int blockAlign = align > wmaBlockAlign.length ? wmaBlockAlign[align & 0xf] : wmaBlockAlign[align];

				output.writeInt(averageBytesPerSec);
				output.writeShort((short) blockAlign);
				output.writeShort((short) 16);
				output.writeShort((short) 0);
				output.writeBytes("dpds".getBytes(Charset.forName("UTF-8")));
				int packetLength = blockAlign;
				int packetNum = audiodata.length / packetLength;
				output.writeInt(packetNum * 4);

				int fullSize = (PlayRegionLength * averageBytesPerSec % 4096 != 0) ? (1 + (int) (PlayRegionLength
						* averageBytesPerSec / 4096)) * 4096
						: PlayRegionLength;
				int allBlocks = fullSize / 4096;
				int avgBlocksPerPacket = allBlocks / packetNum;
				int spareBlocks = allBlocks - (avgBlocksPerPacket * packetNum);

				int accu = 0;
				for (int i = 0; i < packetNum; ++i) {
					accu += avgBlocksPerPacket * 4096;
					if (spareBlocks != 0) {
						accu += 4096;
						--spareBlocks;
					}
					output.writeInt(accu);
				}

				output.writeBytes("data".getBytes(Charset.forName("UTF-8")));

				output.writeInt(PlayRegionLength);
				output.writeBytes(audiodata);
				// Replacing the file size placeholder, dosen't matter with ffmpeg
				// int pos = output.position();
				// output.position(odRIChunkSize);
				// output.putInt(pos - 8);
				// output.position(pos);
				output.close();

				File outputFile = new File(outputDirectory, track + ".wav");
				Ffmpeg.convert(xWmaFile, outputFile);

				xWmaFile.delete();
			} else if (codec == MiniFormatTag_ADPCM) {
				// Convert ADPCM data to PCM
				audiodata = new ADPCMConverter().convertToPCM(
						ByteBuffer.wrap(audiodata), (short) chans, (short) align);
				// Encode PCM as a WAVE file; note that most magic values used
				// here were obtained via trial and error, so it might break...
				ByteBuffer writeBuffer = ByteBuffer.allocate(wavHeaderSize);
				writeBuffer.order(ByteOrder.LITTLE_ENDIAN);
				writeBuffer.put(RIFF);
				writeBuffer.putInt(audiodata.length + 36);
				writeBuffer.put(WAVE);
				writeBuffer.put(fmt);
				writeBuffer.putInt(16);
				writeBuffer.putShort((short) 1); // format code
				writeBuffer.putShort((short) chans); // channels
				writeBuffer.putInt(rate); // blocks per second
				writeBuffer.putInt(rate * 4); // bytes per second
				writeBuffer.putShort((short) 4); // data block alignment
				writeBuffer.putShort((short) 16); // bits per sample
				writeBuffer.put(data);
				writeBuffer.putInt(audiodata.length); // dataChunkSize
				FileOutputStream output = new FileOutputStream(new File(outputDirectory, track + ".wav"));
				output.write(writeBuffer.array(), writeBuffer.arrayOffset(), writeBuffer.position());
				output.write(audiodata);
				output.close();
			} else {
				throw new XnbException("unimplemented codec " + codec);
			}
		}
	}
}
