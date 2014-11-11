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
		if(buffer.remaining() < 4) flush();
		buffer.putInt(value);
	}
	
	public void writeShort(short value) throws IOException {
		if(buffer.remaining() < 2) flush();
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
