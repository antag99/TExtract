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

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Provides methods for getting primitives and
 * basic types from a byte buffer.
 * These methods assumes that the bytebuffer's order
 * is little endian.
 */
public final class Xnb {
	private Xnb() {
	}

	public static int get7BitEncodedInt(ByteBuffer buffer) {
		int result = 0;
		int bitsRead = 0;
		int value;

		do {
			value = buffer.get();
			result |= (value & 0x7f) << bitsRead;
			bitsRead += 7;
		} while ((value & 0x80) != 0);

		return result;
	}

	private static Charset utf8 = Charset.forName("UTF-8");

	public static String getCSharpString(ByteBuffer buffer) {
		int len = get7BitEncodedInt(buffer);
		byte[] buf = new byte[len];
		buffer.get(buf);

		return new String(buf, utf8);
	}
}
