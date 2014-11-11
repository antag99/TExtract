package com.github.antag99.textract.extract;


import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


/*
 * This file was taken from https://code.google.com/p/jwow-kit/,
 * but modified to only decode the Dxt3 format.
 */

/**
 * References: http://madx.dk/wowdev/wiki/index.php?title=BLP
 * http://forum.worldwindcentral.com/showthread.php?p=71605
 * http://en.wikipedia.org/wiki/S3_Texture_Compression
 * http://oss.sgi.com/projects
 * /ogl-sample/registry/EXT/texture_compression_s3tc.txt
 * http://msdn.microsoft.com/en-us/library/bb147243%28VS.85%29.aspx
 * 
 * This code is not at all optimized for performance nor for cleanliness.
 * 
 * ---
 * 
 * @author Dan Watling <dan@synaptik.com>
 **/
class Dxt3 {
	protected static class Color {
		public int r, g, b;

		public Color() {
			this.r = this.g = this.b = 0;
		}
	}

	// Yanked from http://forum.worldwindcentral.com/showthread.php?p=71605
	protected static Color getColor565(int pixel) {
		Color color = new Color();

		color.r = (int) (((long) pixel) & 0xf800) >>> 8;
		color.g = (int) (((long) pixel) & 0x07e0) >>> 3;
		color.b = (int) (((long) pixel) & 0x001f) << 3;

		return color;
	}

	// Yanked from http://forum.worldwindcentral.com/showthread.php?p=71605
	protected static Color[] expandLookupTableDXT3(short c0, short c1) {
		Color[] c = new Color[] { getColor565(c0), getColor565(c1),
				new Color(), new Color() };

		c[2].r = (2 * c[0].r + c[1].r + 1) / 3;
		c[2].g = (2 * c[0].g + c[1].g + 1) / 3;
		c[2].b = (2 * c[0].b + c[1].b + 1) / 3;

		c[3].r = (c[0].r + 2 * c[1].r + 1) / 3;
		c[3].g = (c[0].g + 2 * c[1].g + 1) / 3;
		c[3].b = (c[0].b + 2 * c[1].b + 1) / 3;

		return c;
	}

	// Yanked from http://forum.worldwindcentral.com/showthread.php?p=71605
	protected static int getPixel888(Color color) {
		int r = color.r;
		int g = color.g;
		int b = color.b;
		return r << 16 | g << 8 | b;
	}

	public static BufferedImage getBufferedImage(int width, int height, ByteBuffer bb) {
		bb.order(ByteOrder.LITTLE_ENDIAN);
		
		int[] pixels = new int[16];
		int[] alphas = new int[16];

		BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);

		int numTilesWide = width / 4;
		int numTilesHigh = height / 4;
		for (int i = 0; i < numTilesHigh; i++) {
			for (int j = 0; j < numTilesWide; j++) {
				// Read the alpha table.
				long alphaData = bb.getLong();
				for (int k = alphas.length - 1; k >= 0; k--) {
					alphas[k] = (int) (alphaData >>> (k * 4)) & 0xF; // Alphas are just 4 bits per pixel
					alphas[k] <<= 4;
				}

				short minColor = bb.getShort();
				short maxColor = bb.getShort();
				Color[] lookupTable = expandLookupTableDXT3(minColor, maxColor);

				int colorData = bb.getInt();

				for (int k = pixels.length - 1; k >= 0; k--) {
					int colorCode = (colorData >>> k * 2) & 0x03;
					pixels[k] = (alphas[k] << 24) | getPixel888(lookupTable[colorCode]);
				}

				result.setRGB(j * 4, i * 4, 4, 4, pixels, 0, 4);
			}
		}
		return result;
	}
}
