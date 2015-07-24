/* MSADPCMToPCM - Public Domain MSADPCM Decoder
 * https://github.com/flibitijibibo/MSADPCMToPCM
 *
 * Written by Ethan "flibitijibibo" Lee
 * http://www.flibitijibibo.com/
 *
 * Released under public domain.
 * No warranty implied; use at your own risk.
 *
 * For more on the MSADPCM format, see the MultimediaWiki:
 * http://wiki.multimedia.cx/index.php?title=Microsoft_ADPCM
 */

package com.github.antag99.textract.extract;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Converts the Microsoft ADPCM format to PCM; stolen from MonoGame:
 * https://github.com/mono/MonoGame/blob/d82c0dcb20e65efa68caf9f9222a48675eac6382/MonoGame.Framework/Audio/MSADPCMToPCM.cs
 */
final class ADPCMConverter {
	/**
	 * A bunch of magical numbers that predict the sample data from the
	 * MSADPCM wavedata. Do not attempt to understand at all costs!
	 */
	private static final int[] adaptionTable = {
			230, 230, 230, 230, 307, 409, 512, 614,
			768, 614, 512, 409, 307, 230, 230, 230
	};
	private static final int[] adaptCoeff_1 = {
			256, 512, 0, 192, 240, 460, 392
	};
	private static final int[] adaptCoeff_2 = {
			0, -256, 0, 64, 0, -208, -232
	};

	/**
	 * Splits the MSADPCM samples from each byte block.
	 * 
	 * @param block An MSADPCM sample byte
	 * @param nibbleBlock we copy the parsed shorts into here
	 */
	private static void getNibbleBlock(int block, int[] nibbleBlock) {
		nibbleBlock[0] = (int) (block >>> 4); // Upper half
		nibbleBlock[1] = (int) (block & 0xF); // Lower half
	}

	/**
	 * Calculates PCM samples based on previous samples and a nibble input.
	 * 
	 * @param nibble A parsed MSADPCM sample we got from getNibbleBlock
	 * @param predictor The predictor we get from the MSADPCM block's preamble
	 * @param sample_1 The first sample we use to predict the next sample
	 * @param sample_2 The second sample we use to predict the next sample
	 * @param delta Used to calculate the final sample
	 * @return The calculated PCM sample
	 */
	private short calculateSample(
			int nibble,
			int predictor,
			short[] sample_1,
			short[] sample_2,
			short[] delta
			) {
		// Get a signed number out of the nibble. We need to retain the
		// original nibble value for when we access AdaptionTable[].
		byte signedNibble = (byte) nibble;// sbyte
		if ((signedNibble & 0x8) == 0x8) {
			signedNibble -= 0x10;
		}

		// Calculate new sample
		int sampleInt = (
				((sample_1[0] * adaptCoeff_1[predictor]) +
				(sample_2[0] * adaptCoeff_2[predictor])
				) / 256
				);
		sampleInt += signedNibble * delta[0];

		// Clamp result to 16-bit
		short sample;
		if (sampleInt < Short.MIN_VALUE) {
			sample = Short.MIN_VALUE;
		} else if (sampleInt > Short.MAX_VALUE) {
			sample = Short.MAX_VALUE;
		} else {
			sample = (short) sampleInt;
		}

		// Shuffle samples, get new delta
		sample_2[0] = sample_1[0];
		sample_1[0] = sample;
		delta[0] = (short) (adaptionTable[nibble] * delta[0] / 256);

		// Saturate the delta to a lower bound of 16
		if (delta[0] < 16)
			delta[0] = 16;

		return sample;
	}

	/**
	 * Decodes MSADPCM data to signed 16-bit PCM data.
	 * 
	 * @param source A ByteBuffer containing the headerless MSADPCM data
	 * @param numChannels The number of channels (WAVEFORMATEX nChannels)
	 * @param blockAlign The ADPCM block size (WAVEFORMATEX nBlockAlign)
	 * @return A byte array containing the raw 16-bit PCM wavedata
	 *
	 *         NOTE: The original MSADPCMToPCM class returns as a short[] array!
	 */
	public byte[] convertToPCM(
			ByteBuffer source,
			short numChannels,
			short blockAlign
			) {
		source.order(ByteOrder.LITTLE_ENDIAN);
		try {
			// We write to output when reading the PCM data, then we convert
			// it back to a short array at the end.
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			BufferWriter pcmOut = new BufferWriter(output);
			pcmOut.setOrder(ByteOrder.LITTLE_ENDIAN);

			// We'll be using this to get each sample from the blocks.
			int[] nibbleBlock = new int[2];

			// Assuming the whole stream is what we want.
			long fileLength = source.limit() - blockAlign;

			// Mono or Stereo?
			if (numChannels == 1) {
				// Read to the end of the file.
				while (source.position() <= fileLength) {
					// Read block preamble
					int predictor = source.get() & 0xff;
					short[] delta = { source.getShort() };
					short[] sample_1 = { source.getShort() };
					short[] sample_2 = { source.getShort() };

					// Send the initial samples straight to PCM out.
					pcmOut.writeShort(sample_2[0]);
					pcmOut.writeShort(sample_1[0]);

					// Go through the bytes in this MSADPCM block.
					for (int bytes = 0; bytes < (blockAlign + 15); bytes++) {
						// Each sample is one half of a nibbleBlock.
						getNibbleBlock(source.get() & 0xff, nibbleBlock);
						for (int i = 0; i < 2; i++) {
							pcmOut.writeShort(
									calculateSample(
											nibbleBlock[i],
											predictor,
											sample_1,
											sample_2,
											delta
									)
									);
						}
					}
				}
			} else if (numChannels == 2) {
				// Read to the end of the file.
				while (source.position() <= fileLength) {
					// Read block preamble
					int l_predictor = source.get() & 0xff;
					int r_predictor = source.get() & 0xff;
					short[] l_delta = { source.getShort() };
					short[] r_delta = { source.getShort() };
					short[] l_sample_1 = { source.getShort() };
					short[] r_sample_1 = { source.getShort() };
					short[] l_sample_2 = { source.getShort() };
					short[] r_sample_2 = { source.getShort() };

					// Send the initial samples straight to PCM out.
					pcmOut.writeShort(l_sample_2[0]);
					pcmOut.writeShort(r_sample_2[0]);
					pcmOut.writeShort(l_sample_1[0]);
					pcmOut.writeShort(r_sample_1[0]);

					// Go through the bytes in this MSADPCM block.
					for (int bytes = 0; bytes < ((blockAlign + 15) * 2); bytes++) {
						// Each block carries one left/right sample.
						getNibbleBlock(source.get() & 0xff, nibbleBlock);

						// Left channel...
						pcmOut.writeShort(
								calculateSample(
										nibbleBlock[0],
										l_predictor,
										l_sample_1,
										l_sample_2,
										l_delta
								)
								);

						// Right channel...
						pcmOut.writeShort(
								calculateSample(
										nibbleBlock[1],
										r_predictor,
										r_sample_1,
										r_sample_2,
										r_delta
								)
								);
					}
				}
			} else {
				throw new AssertionError("MSADPCM WAVEDATA IS NOT MONO OR STEREO!");
			}

			// We're done writing PCM data...
			pcmOut.close();
			output.close();

			// Return the array.
			return output.toByteArray();
		} catch (IOException ex) {
			throw new AssertionError("This should not happen as no I/O"
					+ " resources are used", ex);
		}
	}
}
