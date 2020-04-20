/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package kroppeb.server.command.arguments;

import kroppeb.server.command.reader.Reader;
import kroppeb.server.command.reader.ReaderException;
import net.minecraft.command.arguments.CoordinateArgument;
import net.minecraft.command.arguments.DefaultPosArgument;
import net.minecraft.command.arguments.LookingPosArgument;
import net.minecraft.command.arguments.PosArgument;
import net.minecraft.nbt.*;

public class ArgumentParser {
	
	//region tag
	static public CompoundTag readCompoundTag(Reader reader) throws ReaderException {
		CompoundTag tag = new CompoundTag();
		reader.readChar('{');
		reader.next();
		if (!reader.tryRead('}')) {
			do {
				String key = reader.readString();
				reader.next();
				reader.readChar(':');
				reader.next();
				tag.put(key, readTag(reader));
				reader.next();
			} while (reader.tryRead(','));
			reader.readChar('}');
		}
		return tag;
	}
	
	static public Tag readTag(Reader reader) throws ReaderException {
		switch (reader.peek()) {
			case '{':
				return readCompoundTag(reader);
			case '[':
				return readListTag(reader);
			default:
				return readPrimitiveTag(reader);
		}
	}
	
	public static ListTag readListTag(Reader reader) throws ReaderException {
		reader.readChar('[');
		ListTag list = new ListTag();
		byte type = -1;
		reader.next();
		while (reader.peek() != ']') {
			Tag tag = readTag(reader);
			reader.next();
			byte newType = tag.getType();
			if (type == -1) {
				type = newType;
			} else if (newType != type) {
				throw new RuntimeException();//LIST_MIXED.createWithContext(this.reader, tagReader_2.getCommandFeedbackName(), tagReader_1.getCommandFeedbackName());
			}
			
			list.add(tag);
			if (!reader.tryRead(',')) {
				break;
			}
			reader.next();
		}
		
		reader.readChar(']');
		return list;
	}
	
	static public Tag readPrimitiveTag(Reader reader) throws ReaderException {
		if (reader.isQuotedStringStart())
			return StringTag.of(reader.readQuotedString());
		String s = reader.readUnquotedString();
		if (s.endsWith("b")) {
			try {
				return ByteTag.of(Byte.parseByte(s.substring(0, s.length() - 1)));
			} catch (NumberFormatException e) {
				return StringTag.of(s);
			}
		}
		if (s.endsWith("s")) {
			try {
				return ShortTag.of(Short.parseShort(s.substring(0, s.length() - 1)));
			} catch (NumberFormatException e) {
				return StringTag.of(s);
			}
		}
		if (s.endsWith("l")) {
			try {
				return LongTag.of(Long.parseLong(s.substring(0, s.length() - 1)));
			} catch (NumberFormatException e) {
				return StringTag.of(s);
			}
		}
		if (s.endsWith("d")) {
			try {
				return DoubleTag.of(Double.parseDouble(s.substring(0, s.length() - 1)));
			} catch (NumberFormatException e) {
				return StringTag.of(s);
			}
		}
		if (s.endsWith("f")) {
			try {
				return FloatTag.of(Float.parseFloat(s.substring(0, s.length() - 1)));
			} catch (NumberFormatException e) {
				return StringTag.of(s);
			}
		}
		try {
			return DoubleTag.of(Double.parseDouble(s));
		} catch (NumberFormatException e) {
			try {
				return IntTag.of(Integer.parseInt(s));
			} catch (NumberFormatException e2) {
				return StringTag.of(s);
			}
		}
	}
	
	//endregion tag
	//region pos
	public static PosArgument readPos(Reader reader) throws ReaderException {
		if (reader.peek() == '^') {
			double x = readLookingCoordinate(reader);
			reader.readChar(' ');
			double y = readLookingCoordinate(reader);
			reader.readChar(' ');
			double z = readLookingCoordinate(reader);
			return new LookingPosArgument(x, y, z);
		} else {
			CoordinateArgument x = readCoordinateArgument(reader);
			reader.readChar(' ');
			CoordinateArgument y = readCoordinateArgument(reader);
			reader.readChar(' ');
			CoordinateArgument z = readCoordinateArgument(reader);
			return new DefaultPosArgument(x, y, z);
		}
	}
	
	private static CoordinateArgument readCoordinateArgument(Reader reader) throws ReaderException {
		return new CoordinateArgument(reader.tryRead('~'), reader.readDouble());
	}
	
	private static double readLookingCoordinate(Reader reader) throws ReaderException {
		reader.readChar('^');
		return reader.readDouble();
	}
	//endregion
	
}
