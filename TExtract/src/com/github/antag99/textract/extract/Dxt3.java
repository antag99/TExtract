package com.github.antag99.textract.extract;


import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


/*
 * This file was taken from https://code.google.com/p/jwow-kit/,
 * but modified to only decode the Dxt format.
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
	
	/*
	 * TODO: Get rid of BufferedImage and use a custom image data
	 * TODO: Make this an enumeration? (DxtCompression)
	 */
	
	/*
	 * This base class provides functionality required
	 * by the different versions of the algorithms,
	 * the actual implementations are in the subclasses.
	 */

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

	protected static Color getColor555(int pixel) {
		Color color = new Color();

		color.r = (int) (((long) pixel) & 0xf800) >>> 8;
		color.g = (int) (((long) pixel) & 0x07c0) >>> 3;
		color.b = (int) (((long) pixel) & 0x001f) << 3;

		return color;
	}

	protected static int[] expandAlphaTable(short a0, short a1) {
		int[] a = new int[] { a0, a1, 0, 0, 0, 0, 0, 0 };

		if (a[0] > a[1]) {
			a[2] = (6 * a[0] + 1 * a[1] + 3) / 7;
			a[3] = (5 * a[0] + 2 * a[1] + 3) / 7;
			a[4] = (4 * a[0] + 3 * a[1] + 3) / 7;
			a[5] = (3 * a[0] + 4 * a[1] + 3) / 7;
			a[6] = (2 * a[0] + 5 * a[1] + 3) / 7;
			a[7] = (1 * a[0] + 6 * a[1] + 3) / 7;
		} else {
			a[2] = (4 * a[0] + 1 * a[1] + 2) / 5;
			a[3] = (3 * a[0] + 2 * a[1] + 2) / 5;
			a[4] = (2 * a[0] + 3 * a[1] + 2) / 5;
			a[5] = (1 * a[0] + 4 * a[1] + 2) / 5;
			a[6] = 0;
			a[7] = 255;
		}

		return a;
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

	protected static Color[] expandLookupTableDXT1(short c0, short c1,
			boolean alpha) {
		int uC0 = c0;
		int uC1 = c1;

		if (uC0 < 0) {
			uC0 = 65536 + c0;
		}
		if (uC1 < 0) {
			uC1 = 65536 + c1;
		}
		Color[] c = new Color[] { getColor565(c0), getColor565(c1),
				new Color(), new Color() };

		if (alpha && uC0 > uC1 || !alpha) {
			c[2].r = (2 * c[0].r + c[1].r + 1) / 3;
			c[2].g = (2 * c[0].g + c[1].g + 1) / 3;
			c[2].b = (2 * c[0].b + c[1].b + 1) / 3;

			c[3].r = (c[0].r + 1 + 2 * c[1].r) / 3;
			c[3].g = (c[0].g + 1 + 2 * c[1].g) / 3;
			c[3].b = (c[0].b + 1 + 2 * c[1].b) / 3;
		} else {
			c[2].r = (c[0].r + c[1].r + 1) / 2;
			c[2].g = (c[0].g + c[1].g + 1) / 2;
			c[2].b = (c[0].b + c[1].b + 1) / 2;

			c[3].r = 0;
			c[3].g = 0;
			c[3].b = 0;
		}

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
