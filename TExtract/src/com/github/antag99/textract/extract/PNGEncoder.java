package com.github.antag99.textract.extract;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/** PNG encoder (C) 2006-2009 by Christian Fröschlin, www.chrfr.de
 * 
 * Minimal PNG encoder to create PNG streams (and MIDP images) from RGBA arrays.
 * Copyright 2006-2009 Christian Fröschlin www.chrfr.de Terms of Use:
 * You may use the PNG encoder free of charge for any purpose you desire, as long
 * as you do not claim credit for the original sources and agree not to hold me
 * responsible for any damage arising out of its use. */
class PNGEncoder {
	
	
	
	// TODO Reduce the amount of byte arrays created when encoding png.
		// This is important when encoding thousands of files.
		
		static byte[] signature = new byte[] { (byte) 137, (byte) 80, (byte) 78, (byte) 71, (byte) 13, (byte) 10, (byte) 26, (byte) 10 };
		static byte[] idhr = new byte[] { (byte)'I', (byte)'H', (byte)'D', (byte)'R' };
		static byte[] idat  = new byte[] { (byte)'I', (byte)'D', (byte)'A', (byte)'T' };
		static byte[] iend  = new byte[] { (byte)'I', (byte)'E', (byte)'N', (byte)'D' };
		
		public static byte[] toPNG(int width, int height, ByteBuffer buffer) throws IOException {
			byte[] header = createHeaderChunk(width, height);
			byte[] data = createDataChunk(width, height, buffer);
			byte[] trailer = createTrailerChunk();
			ByteArrayOutputStream png = new ByteArrayOutputStream(signature.length
					+ header.length + data.length + trailer.length);
			png.write(signature);
			png.write(header);
			png.write(data);
			png.write(trailer);
			return png.toByteArray();
		}

		static byte[] createHeaderChunk(int width, int height)
				throws IOException {
			ByteArrayOutputStream baos = new ByteArrayOutputStream(13);
			DataOutputStream chunk = new DataOutputStream(baos);
			chunk.writeInt(width);
			chunk.writeInt(height);
			chunk.writeByte(8); // Bitdepth
			chunk.writeByte(6); // Colortype ARGB
			chunk.writeByte(0); // Compression
			chunk.writeByte(0); // Filter
			chunk.writeByte(0); // Interlace
			return toChunk(idhr, baos.toByteArray());
		}

		static byte[] createDataChunk(int width, int height, ByteBuffer buffer) throws IOException {
//			int source = 0;
			int dest = 0;
			byte[] raw = new byte[4 * (width * height) + height];
			for (int y = 0; y < height; y++) {
				raw[dest++] = 0; // No filter
				for (int x = 0; x < width; x++) {
					buffer.get(raw, dest, 4);
					dest += 4;
//					raw[dest++] = red[source];
//					raw[dest++] = green[source];
//					raw[dest++] = blue[source];
//					raw[dest++] = alpha[source++];
				}
			}
			return toChunk(idat, toZLIB(raw));
		}

		static byte[] createTrailerChunk() throws IOException {
			return toChunk(iend, new byte[0]);
		}

		static byte[] toChunk(byte[] bid, byte[] raw) throws IOException {
			ByteArrayOutputStream baos = new ByteArrayOutputStream(raw.length + 12);
			DataOutputStream chunk = new DataOutputStream(baos);
			chunk.writeInt(raw.length);
			chunk.write(bid);
			chunk.write(raw);
			int crc = 0xFFFFFFFF;
			crc = updateCRC(crc, bid);
			crc = updateCRC(crc, raw);
			chunk.writeInt(~crc);
			return baos.toByteArray();
		}

		static int[] crcTable = null;

		static void createCRCTable() {
			crcTable = new int[256];
			for (int i = 0; i < 256; i++) {
				int c = i;
				for (int k = 0; k < 8; k++) {
					c = ((c & 1) > 0) ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
				}
				crcTable[i] = c;
			}
		}

		static int updateCRC(int crc, byte[] raw) {
			if (crcTable == null) {
				createCRCTable();
			}
			for (int i = 0; i < raw.length; i++) {
				crc = crcTable[(crc ^ raw[i]) & 0xFF] ^ (crc >>> 8);
			}
			return crc;
		}
		
		/** Temporary buffer used when compressing.
		 * This won't work when dealing with bigger files,
		 * but terraria's image files shouldn't exceed this limit. */
//		static byte[] buf = new byte[1024];

		/*
		 * This method is called to encode the image data as a zlib block as
		 * required by the PNG specification. This file comes with a minimal ZLIB
		 * encoder which uses uncompressed deflate blocks (fast, short, easy, but no
		 * compression). If you want compression, call another encoder (such as
		 * JZLib?) here.
		 */
		static byte[] toZLIB(byte[] raw) throws IOException {
//			Deflater deflater = new Deflater();
//			deflater.setInput(raw);
//			int bufLength = deflater.deflate(buf);
//			return Arrays.copyOf(buf, bufLength);
			return ZLIB.toZLIB(raw);
		}
		
		static class ZLIB {
			static final int BLOCK_SIZE = 32000;

			public static byte[] toZLIB(byte[] raw) throws IOException {
				ByteArrayOutputStream baos = new ByteArrayOutputStream(raw.length + 6
						+ (raw.length / BLOCK_SIZE) * 5);
				DataOutputStream zlib = new DataOutputStream(baos);
				byte tmp = (byte) 8;
				zlib.writeByte(tmp); // CM = 8, CMINFO = 0
				zlib.writeByte((31 - ((tmp << 8) % 31)) % 31); // FCHECK
																// (FDICT/FLEVEL=0)
				int pos = 0;
				while (raw.length - pos > BLOCK_SIZE) {
					writeUncompressedDeflateBlock(zlib, false, raw, pos,
							(char) BLOCK_SIZE);
					pos += BLOCK_SIZE;
				}
				writeUncompressedDeflateBlock(zlib, true, raw, pos,
						(char) (raw.length - pos));
				// zlib check sum of uncompressed data
				zlib.writeInt(calcADLER32(raw));
				return baos.toByteArray();
			}

			private static void writeUncompressedDeflateBlock(DataOutputStream zlib,
					boolean last, byte[] raw, int off, char len) throws IOException {
				zlib.writeByte((byte) (last ? 1 : 0)); // Final flag, Compression type 0
				zlib.writeByte((byte) (len & 0xFF)); // Length LSB
				zlib.writeByte((byte) ((len & 0xFF00) >> 8)); // Length MSB
				zlib.writeByte((byte) (~len & 0xFF)); // Length 1st complement LSB
				zlib.writeByte((byte) ((~len & 0xFF00) >> 8)); // Length 1st complement
																// MSB
				zlib.write(raw, off, len); // Data
			}

			private static int calcADLER32(byte[] raw) {
				int s1 = 1;
				int s2 = 0;
				for (int i = 0; i < raw.length; i++) {
					int abs = raw[i] >= 0 ? raw[i] : (raw[i] + 256);
					s1 = (s1 + abs) % 65521;
					s2 = (s2 + s1) % 65521;
				}
				return (s2 << 16) + s1;
			}
		}
	
}
