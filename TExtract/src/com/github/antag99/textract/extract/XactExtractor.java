package com.github.antag99.textract.extract;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.github.antag99.textract.StatusReporter;

public class XactExtractor {
	// XWB parsing was adapted from MonoGame
	private static final Logger logger = LogManager.getLogger(XactExtractor.class);
	
	protected File inputFile;
	protected File outputDirectory;

	// Track codecs
	private static final int MiniFormatTag_PCM = 0x0;
	private static final int MiniFormatTag_XMA = 0x1;
	private static final int MiniFormatTag_ADPCM = 0x2;
	private static final int MiniFormatTag_WMA = 0x3;

	/** Mapping of music wave bank indexes to their names */
	static final String[] trackNames = {
		"OverworldNight", //1
		"Eerie", //2
		"OverworldDay", //3
		"Boss1", //4
		"TitleScreen", //5
		"Jungle", //6
		"Corruption", //7
		"Hallow", //8
		"UndergroundCorruption", //9
		"UndergroundHallow", //10
		"Boss2", //11
		"Underground", //12
		"Boss3", //13
		"Snow", //14
		"Space", //15
		"Crimson", //16
		"Golem", //17
		"AlternateDay", //18
		"Rain", //19
		"UndergroundSnow", //20
		"Desert", //21
		"Ocean", //22
		"Dungeon", //23
		"Plantera", //24
		"QueenBee", //25
		"Lizhard", //26
		"Eclipse", //27
		"RainAmbience", //28
		"Mushrooms", //29
		"PumpkinMoon", //30
		"AlternateUnderground", //31
		"FrostMoon", //32
		"UndergroundCrimson" //33
	};

	private static final int Flag_Compact = 0x00020000;
	
	private StatusReporter statusReporter = StatusReporter.mutedReporter;

	public XactExtractor() {
	}
	
	public StatusReporter getStatusReporter() {
		return statusReporter;
	}
	
	public void setStatusReporter(StatusReporter statusReporter) {
		this.statusReporter = statusReporter;
	}
	
	public File getInputFile() {
		return inputFile;
	}
	
	public File getOutputDirectory() {
		return outputDirectory;
	}
	
	public void setInputFile(File inputFile) {
		this.inputFile = inputFile;
	}
	
	public void setOutputDirectory(File outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	public void extract() {
		try {
			statusReporter.reportTaskStatus("Parsing XACT header");
			statusReporter.reportTaskPercentage(0f);
			ByteBuffer buffer = ByteBuffer.wrap(FileUtils.readFileToByteArray(inputFile));
			buffer.order(ByteOrder.LITTLE_ENDIAN);
			statusReporter.reportTaskPercentage(0.05f);
			
			int Format = 0;
			int PlayRegionLength = 0;
			int PlayRegionOffset = 0;
	
			int wavebank_offset = 0;

			if(buffer.get() != 'W' || buffer.get() != 'B' ||
					buffer.get() != 'N' || buffer.get() != 'D') {
				throw new IOException("Unrecognized header");
			}
	
			int Version = buffer.getInt();
	
			// Skip trailing bytes of the version
			buffer.getInt();
	
			if (Version != 46) {
				throw new IOException("Unsupported version");
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
				throw new IOException("Compact wavebanks are not supported");
			}
	
			int playregion_offset = segmentOffsets[4];
			for (int current_entry = 0; current_entry < EntryCount; current_entry++) {
				String track = current_entry < trackNames.length ? trackNames[current_entry] : "Unknown_" + ((current_entry - trackNames.length) + 1);
				
				statusReporter.reportTaskStatus("Extracting " + track);
				statusReporter.reportTaskPercentage(0.1f + (0.9f / EntryCount) * current_entry);
				
				buffer.position(wavebank_offset);
				if (EntryMetaDataElementSize >= 4) buffer.getInt(); // FlagsAndDuration
				if (EntryMetaDataElementSize >= 8) Format = buffer.getInt();
				if (EntryMetaDataElementSize >= 12) PlayRegionOffset = buffer.getInt();
				if (EntryMetaDataElementSize >= 16) PlayRegionLength = buffer.getInt();
				if (EntryMetaDataElementSize >= 20) buffer.getInt(); // LoopRegionOffset
				if (EntryMetaDataElementSize >= 24) buffer.getInt(); // LoopRegionLength

				wavebank_offset += EntryMetaDataElementSize;
				PlayRegionOffset += playregion_offset;

				int codec = (Format) & ((1 << 2) - 1);
				int chans = (Format >> (2)) & ((1 << 3) - 1);
				int rate = (Format >> (2 + 3)) & ((1 << 18) - 1);
				int align = (Format >> (2 + 3 + 18)) & ((1 << 8) - 1);

				buffer.position(PlayRegionOffset);
				byte[] audiodata = new byte[PlayRegionLength];
				buffer.get(audiodata);

				// Terraria's default tracks are only xWma; all wavebanks i have seen uses it
				if (codec == MiniFormatTag_WMA) {
					// Note that it could still be another codec than xWma,
					// but that scenario isn't handled here.

					// This part has been ported from XWMA-to-pcm-u8
					// Not the most beautiful code in the world,
					// but it does the job.

					// I do not know if this code outputs valid XWMA files,
					// but FFMPEG accepts them so it's all right.

					File xWmaFile = new File(outputDirectory, track + ".wma");

    				FileOutputStream xWmaOutput = FileUtils.openOutputStream(xWmaFile);
//    				xWmaOutput.write(output.array(), output.arrayOffset(), output.position());

    				BufferWriter output = new BufferWriter(xWmaOutput);
    				output.setOrder(ByteOrder.LITTLE_ENDIAN);
    				output.writeBytes("RIFF".getBytes(Charset.forName("UTF-8")));
//    				int odRIChunkSize = output.position();
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
				} else {
					throw new IOException("Unimplemented codec " + codec);
				}
			}
		} catch(IOException ex) {
			logger.error("An I/O error occured when extracting Wave Bank", ex);
		} catch(Throwable t) {
			logger.error("An unexpected error occured when extracting Wave Bank", t);
		}
	}
}
