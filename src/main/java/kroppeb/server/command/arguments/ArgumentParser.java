/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package kroppeb.server.command.arguments;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import kroppeb.server.command.CommandLoader;
import kroppeb.server.command.reader.Reader;
import kroppeb.server.command.reader.ReaderException;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.arguments.*;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ClearCommand;
import net.minecraft.server.command.ExecuteCommand;
import net.minecraft.server.command.GiveCommand;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;

import java.util.*;
import java.util.function.Predicate;

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
		if (reader.tryRead('~')) {
			if (reader.canRead() && reader.peek() != ' ')
				return new CoordinateArgument(true, reader.readSimpleDouble());
			return new CoordinateArgument(true, 0.0D);
		} else {
			return new CoordinateArgument(false, reader.readSimpleDoubleIntOffset());
		}
	}
	
	private static double readLookingCoordinate(Reader reader) throws ReaderException {
		reader.readChar('^');
		if (reader.canRead() && reader.peek() != ' ')
			return reader.readSimpleDouble();
		return 0.0D;
	}
	
	//endregion
	//region block
	static public Predicate<CachedBlockPosition> readBlockPredicate(Reader reader, boolean allowTags) throws ReaderException {
		if (reader.tryRead('#')) {
			if (!allowTags)
				throw new ReaderException("Tags are not allowed here");
			Identifier identifier = reader.readIdentifier();
			net.minecraft.tag.Tag<Block> blockTag = CommandLoader.getBlockTag(identifier);
			Map<String, String> properties = null;
			if (reader.tryRead('[')) {
				properties = readBlockTagProperties(reader);
			}
			CompoundTag nbtData = null;
			if (reader.canRead() && reader.peek() == '{') {
				nbtData = ArgumentParser.readCompoundTag(reader);
			}
			
			return new BlockPredicateArgumentType.TagPredicate(blockTag, properties, nbtData);
		} else {
			Block block = readBlockId(reader);
			BlockState state = block.getDefaultState();
			Set<Property<?>> keys = null;
			CompoundTag nbtData = null;
			
			if (reader.tryRead('[')) {
				Map<Property<?>, Comparable<?>> properties = readBlockProperties(reader, block);
				keys = properties.keySet();
				for (Map.Entry<Property<?>, Comparable<?>> entry : properties.entrySet()) {
					state = addProperty(state, entry);
				}
			}
			
			if (reader.canRead() && reader.peek() == '{') {
				nbtData = ArgumentParser.readCompoundTag(reader);
			}
			
			return new BlockPredicateArgumentType.StatePredicate(
					state, keys, nbtData);
		}
	}
	
	static public BlockStateArgument readBlock(Reader reader) throws ReaderException {
		Block block = readBlockId(reader);
		BlockState state = block.getDefaultState();
		Set<Property<?>> keys = null;
		CompoundTag nbtData = null;
		
		if (reader.tryRead('[')) {
			Map<Property<?>, Comparable<?>> properties = readBlockProperties(reader, block);
			keys = properties.keySet();
			for (Map.Entry<Property<?>, Comparable<?>> entry : properties.entrySet()) {
				state = addProperty(state, entry);
			}
		}
		
		if (reader.canRead() && reader.peek() == '{') {
			nbtData = ArgumentParser.readCompoundTag(reader);
		}
		
		return new BlockStateArgument(state, keys, nbtData);
	}
	
	// to get around java type system
	private static <T extends Comparable<T>> BlockState addProperty(BlockState state, Map.Entry<Property<?>, Comparable<?>> entry) {
		//noinspection unchecked
		return state.with((Property<T>) entry.getKey(), (T) entry.getValue());
	}
	
	
	static public Block readBlockId(Reader reader) throws ReaderException {
		Identifier id = reader.readIdentifier();
		return Registry.BLOCK.getOrEmpty(id).orElseThrow(() -> new ReaderException("unknown block: " + id.toString()));
	}
	
	
	static public Map<Property<?>, Comparable<?>> readBlockProperties(Reader reader, Block block) throws ReaderException {
		StateManager<Block, BlockState> stateFactory = block.getStateManager();
		Map<Property<?>, Comparable<?>> properties = new HashMap<>();
		while (true) {
			String key = reader.readString();
			Property<?> property = stateFactory.getProperty(key);
			
			if (property == null)
				throw new ReaderException("Unknown property: " + key);
			
			if (properties.containsKey(property))
				throw new ReaderException("Duplicate property: " + key);
			
			reader.next();
			reader.readChar('=');
			reader.next();
			String valueString = reader.readString();
			Optional<?> value = property.parse(valueString);
			
			if (value.isPresent()) {
				properties.put(property, (Comparable<?>) value.get());
			} else {
				throw new ReaderException("Unknown value for property (" + key + "): " + valueString);
			}
			
			
			reader.next();
			if (!reader.tryRead(','))
				break;
			reader.next();
		}
		reader.readChar(']');
		return properties;
	}
	
	static public Map<String, String> readBlockTagProperties(Reader reader) throws ReaderException {
		Map<String, String> properties = new HashMap<>();
		while (true) {
			String key = reader.readString();
			
			if (properties.containsKey(key))
				throw new ReaderException("Duplicate property: " + key);
			
			reader.next();
			reader.readChar('=');
			reader.next();
			String value = reader.readString();
			
			properties.put(key, value);
			reader.next();
			if (!reader.tryRead(','))
				break;
			reader.next();
		}
		reader.readChar(']');
		return properties;
	}
	
	//endregion
	//region swizzle
	static public EnumSet<Direction.Axis> readSwizzle(Reader reader) throws ReaderException {
		String s = reader.readUntilWhitespace();
		EnumSet<Direction.Axis> axes = EnumSet.noneOf(Direction.Axis.class);
		
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			Direction.Axis axis;
			switch (c) {
				case 'x':
					axis = Direction.Axis.X;
					break;
				case 'y':
					axis = Direction.Axis.Y;
					break;
				case 'z':
					axis = Direction.Axis.Z;
					break;
				default:
					throw new ReaderException("Unknown axis: " + c);
			}
			if (!axes.add(axis))
				throw new ReaderException("Duplicated axis: " + c);
		}
		return axes;
	}
	
	//endregion
	//region item
	public static Predicate<ItemStack> readItemPredicate(Reader reader, boolean allowTags) throws ReaderException {
		if (reader.tryRead('#')) {
			if (!allowTags)
				throw new ReaderException("Tags are not allowed here");
			Identifier identifier = reader.readIdentifier();
			net.minecraft.tag.Tag<Item> itemTag = CommandLoader.getItemTag(identifier);
			
			CompoundTag nbtData = null;
			if (reader.canRead() && reader.peek() == '{') {
				nbtData = ArgumentParser.readCompoundTag(reader);
			}
			
			return new ItemPredicateArgumentType.TagPredicate(itemTag, nbtData);
		} else {
			Item item = readItemId(reader);
			CompoundTag nbtData = null;
			
			if (reader.canRead() && reader.peek() == '{') {
				nbtData = ArgumentParser.readCompoundTag(reader);
			}
			
			return new ItemPredicateArgumentType.ItemPredicate(item, nbtData);
		}
		
	}
	
	public static ItemStack readItemStack(Reader reader) throws ReaderException {
		ItemStack item = new ItemStack(readItemId(reader));
		if (reader.canRead() && reader.peek() == '{') {
			item.setTag(ArgumentParser.readCompoundTag(reader));
		}
		return item;
	}
	
	static public Item readItemId(Reader reader) throws ReaderException {
		Identifier id = reader.readIdentifier();
		return Registry.ITEM.getOrEmpty(id).orElseThrow(() -> new ReaderException("unknown block: " + id.toString()));
	}
	
	//endregion
}
