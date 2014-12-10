package com.github.antag99.textract.extract;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;

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

	public static char getCSharpChar(ByteBuffer buffer) {
		char result = (char) buffer.get();
		if ((result & 0x80) != 0) {
			int bytes = 1;
			while ((result & (0x80 >> bytes)) != 0)
				bytes++;
			result &= (1 << (8 - bytes)) - 1;
			while (--bytes > 0) {
				result <<= 6;
				result |= buffer.get() & 0x3F;
			}
		}
		return result;
	}

	public static <T> void getList(ByteBuffer buffer, List<T> list, Class<T> clazz) {
		if (get7BitEncodedInt(buffer) == 0)
			return; // Null list
		int len = buffer.getInt();
		for (int i = 0; i < len; ++i) {
			if (clazz == Rectangle.class) {
				list.add(clazz.cast(getRectangle(buffer)));
			} else if (clazz == Vector3.class) {
				list.add(clazz.cast(getVector3(buffer)));
			} else {
				throw new RuntimeException("Unsupported array type");
			}
		}
	}

	private static Charset utf8 = Charset.forName("UTF-8");

	public static String getCSharpString(ByteBuffer buffer) {
		int len = get7BitEncodedInt(buffer);
		byte[] buf = new byte[len];
		buffer.get(buf);

		return new String(buf, utf8);
	}

	public static Rectangle getRectangle(ByteBuffer buffer) {
		return new Rectangle(buffer.getInt(), buffer.getInt(), buffer.getInt(), buffer.getInt());
	}

	public static Vector3 getVector3(ByteBuffer buffer) {
		return new Vector3(buffer.getFloat(), buffer.getFloat(), buffer.getFloat());
	}
}
