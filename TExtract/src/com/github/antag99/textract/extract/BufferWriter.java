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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class BufferWriter {
	private ByteBuffer buffer;
	private OutputStream output;

	public BufferWriter(OutputStream output) {
		buffer = ByteBuffer.allocate(8);
		this.output = output;
	}

	public void setOrder(ByteOrder order) {
		buffer.order(order);
	}

	public ByteOrder getOrder() {
		return buffer.order();
	}

	public void writeBytes(byte[] value) throws IOException {
		flush();
		output.write(value);
	}

	public void writeInt(int value) throws IOException {
		if (buffer.remaining() < 4)
			flush();
		buffer.putInt(value);
	}

	public void writeShort(short value) throws IOException {
		if (buffer.remaining() < 2)
			flush();
		buffer.putShort(value);
	}

	public void flush() throws IOException {
		output.write(buffer.array(), buffer.arrayOffset(), buffer.position());
		buffer.position(0);
	}

	public void close() throws IOException {
		flush();
		output.close();
	}
}
