/*
 * Copyright (c) 2021 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package kroppeb.server.command.arguments

import com.google.common.collect.Lists
import kroppeb.server.command.CommandLoader
import kroppeb.server.command.reader.Reader
import kroppeb.server.command.reader.ReaderException
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.pattern.CachedBlockPosition
import net.minecraft.command.argument.*
import net.minecraft.command.argument.NbtPathArgumentType.*
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.*
import net.minecraft.state.property.Property
import net.minecraft.util.math.Direction
import net.minecraft.util.registry.Registry
import java.util.*
import java.util.function.Predicate


//region tag
fun Reader.readCompoundTag(): CompoundTag {
	val tag = CompoundTag()
	readChar('{')
	next()
	if (!tryRead('}')) {
		do {
			next()
			val key = readString()
			next()
			readChar(':')
			next()
			tag.put(key, this.readTag())
			next()
		} while (tryRead(','))
		readChar('}')
	}
	return tag
}

fun Reader.readTag(): Tag {
	return when (peek()) {
		'{' -> readCompoundTag()
		'[' -> this.readListTag()
		else -> this.readPrimitiveTag()
	}
}

fun Reader.readListTag(): ListTag {
	readChar('[')
	val list = ListTag()
	var type: Byte = -1
	next()
	if (!tryRead(']')) {
		do {
			var tag: Tag
			do {
				next()

				tag = readTag()
				next()

				// that was the type
			} while (tryRead(';'))

			val newType = tag.type
			if (type.toInt() == -1) {
				type = newType
			} else if (newType != type) {
				throw RuntimeException() //LIST_MIXED.createWithContext(this.reader, tagReader_2.getCommandFeedbackName(), tagReader_1.getCommandFeedbackName());
			}
			list.add(tag)
		} while (tryRead(','))
		readChar(']')
	}
	return list
}

fun Reader.readPrimitiveTag(): Tag {
	if (isQuotedStringStart) return StringTag.of(readQuotedString())
	val s = readUnquotedString()
	if (s.endsWith("b")) {
		return try {
			ByteTag.of(s.substring(0, s.length - 1).toByte())
		} catch (e: NumberFormatException) {
			StringTag.of(s)
		}
	}
	if (s.endsWith("s")) {
		return try {
			ShortTag.of(s.substring(0, s.length - 1).toShort())
		} catch (e: NumberFormatException) {
			StringTag.of(s)
		}
	}
	if (s.endsWith("l")) {
		return try {
			LongTag.of(s.substring(0, s.length - 1).toLong())
		} catch (e: NumberFormatException) {
			StringTag.of(s)
		}
	}
	if (s.endsWith("d")) {
		return try {
			DoubleTag.of(s.substring(0, s.length - 1).toDouble())
		} catch (e: NumberFormatException) {
			StringTag.of(s)
		}
	}
	return if (s.endsWith("f")) {
		try {
			FloatTag.of(s.substring(0, s.length - 1).toFloat())
		} catch (e: NumberFormatException) {
			StringTag.of(s)
		}
	} else try {
		DoubleTag.of(s.toDouble())
	} catch (e: NumberFormatException) {
		try {
			IntTag.of(s.toInt())
		} catch (e2: NumberFormatException) {
			StringTag.of(s)
		}
	}
}

//endregion tag
//region pos
fun Reader.readPos(): PosArgument {
	return if (peek() == '^') {
		val x = this.readLookingCoordinate()
		readChar(' ')
		val y = this.readLookingCoordinate()
		readChar(' ')
		val z = this.readLookingCoordinate()
		LookingPosArgument(x, y, z)
	} else {
		val x = this.readCoordinateArgument()
		readChar(' ')
		val y = this.readCoordinateArgument()
		readChar(' ')
		val z = this.readCoordinateArgument()
		DefaultPosArgument(x, y, z)
	}
}

private fun Reader.readCoordinateArgument(): CoordinateArgument {
	return if (tryRead('~')) {
		if (canRead() && peek() != ' ') CoordinateArgument(true, readDouble()) else CoordinateArgument(true, 0.0)
	} else {
		CoordinateArgument(false, readSimpleDoubleIntOffset())
	}
}

private fun Reader.readLookingCoordinate(): Double {
	readChar('^')
	return if (canRead() && peek() != ' ') readDouble() else 0.0
}

//endregion
//region block
fun Reader.readBlockPredicate(allowTags: Boolean): Predicate<CachedBlockPosition> {
	return if (tryRead('#')) {
		if (!allowTags) throw ReaderException("Tags are not allowed here")
		val identifier = readIdentifier()
		val blockTag = CommandLoader.getBlockTag(identifier)
		var properties: Map<String?, String?>? = null
		if (tryRead('[')) {
			properties = this.readBlockTagProperties()
		}
		var nbtData: CompoundTag? = null
		if (canRead() && peek() == '{') {
			nbtData = readCompoundTag()
		}
		BlockPredicateArgumentType.TagPredicate(blockTag, properties, nbtData)
	} else {
		val block = this.readBlockId()
		var state = block.defaultState
		var keys: Set<Property<*>>? = null
		var nbtData: CompoundTag? = null
		if (tryRead('[')) {
			val properties = this.readBlockProperties(block)
			keys = properties.keys
			for (entry in properties.entries) {
				state = addProperty<Nothing>(state, entry)
			}
		}
		if (canRead() && peek() == '{') {
			nbtData = readCompoundTag()
		}
		BlockPredicateArgumentType.StatePredicate(
				state, keys, nbtData)
	}
}

fun Reader.readBlock(): BlockStateArgument {
	val block = this.readBlockId()
	var state = block.defaultState
	var keys: Set<Property<*>>? = null
	var nbtData: CompoundTag? = null
	if (tryRead('[')) {
		val properties = this.readBlockProperties(block)
		keys = properties.keys
		for (entry in properties.entries) {
			state = addProperty<Nothing>(state, entry)
		}
	}
	if (canRead() && peek() == '{') {
		nbtData = readCompoundTag()
	}
	return BlockStateArgument(state, keys, nbtData)
}

// to get around java type system
private fun <T : Comparable<T>> addProperty(
		state: BlockState,
		entry: Map.Entry<Property<*>, Comparable<*>>): BlockState {
	@Suppress("UNCHECKED_CAST")
	return state.with(entry.key as Property<T>, entry.value as T)
}

fun Reader.readBlockId(): Block {
	val id = readIdentifier()
	return Registry.BLOCK.getOrEmpty(id).orElseThrow { ReaderException("unknown block: $id") }
}

fun Reader.readBlockProperties(block: Block): Map<Property<*>, Comparable<*>> {
	val stateFactory = block.stateManager
	val properties: MutableMap<Property<*>, Comparable<*>> = HashMap()
	while (true) {
		val key = readString()
		val property = stateFactory.getProperty(key) ?: throw ReaderException("Unknown property: $key")
		if (properties.containsKey(property)) throw ReaderException("Duplicate property: $key")
		next()
		readChar('=')
		next()
		val valueString = readString()
		val value: Optional<*> = property.parse(valueString)
		if (value.isPresent) {
			properties[property] = value.get() as Comparable<*>
		} else {
			throw ReaderException("Unknown value for property ($key): $valueString")
		}
		next()
		if (!tryRead(',')) break
		next()
	}
	readChar(']')
	return properties
}

fun Reader.readBlockTagProperties(): Map<String?, String?> {
	val properties: MutableMap<String?, String?> = HashMap()
	while (true) {
		val key = readString()
		if (properties.containsKey(key)) throw ReaderException("Duplicate property: $key")
		next()
		readChar('=')
		next()
		val value = readString()
		properties[key] = value
		next()
		if (!tryRead(',')) break
		next()
	}
	readChar(']')
	return properties
}

//endregion
//region swizzle
fun Reader.readSwizzle(): EnumSet<Direction.Axis?> {
	val s = readUntilWhitespace()
	val axes = EnumSet.noneOf(Direction.Axis::class.java)
	for (element in s) {
		val axis: Direction.Axis = when (element) {
			'x' -> Direction.Axis.X
			'y' -> Direction.Axis.Y
			'z' -> Direction.Axis.Z
			else -> throw ReaderException("Unknown axis: $element")
		}
		if (!axes.add(axis)) throw ReaderException("Duplicated axis: $element")
	}
	return axes
}

//endregion
//region item
fun Reader.readItemPredicate(allowTags: Boolean): Predicate<ItemStack> {
	return if (tryRead('#')) {
		if (!allowTags) throw ReaderException("Tags are not allowed here")
		val identifier = readIdentifier()
		val itemTag = CommandLoader.getItemTag(identifier)
		var nbtData: CompoundTag? = null
		if (canRead() && peek() == '{') {
			nbtData = readCompoundTag()
		}
		ItemPredicateArgumentType.TagPredicate(itemTag, nbtData)
	} else {
		val item = this.readItemId()
		var nbtData: CompoundTag? = null
		if (canRead() && peek() == '{') {
			nbtData = readCompoundTag()
		}
		ItemPredicateArgumentType.ItemPredicate(item, nbtData)
	}
}

fun Reader.readItemStack(): ItemStack {
	val item = ItemStack(this.readItemId())
	if (canRead() && peek() == '{') {
		item.tag = readCompoundTag()
	}
	return item
}

fun Reader.readItemId(): Item {
	val id = readIdentifier()
	return Registry.ITEM.getOrEmpty(id).orElseThrow { ReaderException("unknown block: $id") }
}

fun Reader.readPath(): NbtPath {
	val list: MutableList<PathNode> = Lists.newArrayList()
	var bl = true
	while (!isWhiteSpace) {
		val pathNode = this.parseNode(bl)
		list.add(pathNode)
		bl = false
		if (canRead()) {
			val c = peek()
			if (c != ' ' && c != '[' && c != '{') {
				readChar('.')
			}
		} else {
			break
		}
	}
	return NbtPath(
			"not available",
			list.toTypedArray(),
			null) // TODO: will cause NPE on error instead of CommandError
}

private fun Reader.parseNode(root: Boolean): PathNode {
	val string: String
	return when (peek()) {
		'"' -> {
			string = readQuotedString()
			this.readCompoundChildNode(string)
		}
		'[' -> {
			skip()
			val i = peek()
			when (i) {
				'{' -> {
					val tag = readCompoundTag()
					readChar(']')
					FilteredListElementNode(tag)
				}
				']' -> {
					skip()
					NbtPathArgumentType.AllListElementNode.INSTANCE
				}
				else -> {
					val j = readInt()
					readChar(']')
					IndexedListElementNode(j)
				}
			}
		}
		'{' -> {
			if (!root) {
				throw ReaderException("invalid nbt path")
			}
			val tag = readCompoundTag()
			FilteredRootNode(tag)
		}
		else -> {
			string = readNamedPath()
			this.readCompoundChildNode(string)
		}
	}
}

private fun Reader.readCompoundChildNode(name: String): PathNode {
	return if (canRead() && peek() == '{') {
		val compoundTag = readCompoundTag()
		FilteredNamedNode(name, compoundTag)
	} else {
		NbtPathArgumentType.NamedNode(name)
	}
}

//endregion
//region ranges
fun Reader.readIntRange(): IntRange {
	val minValue: Int
	val maxValue: Int
	try {
		if (tryRead("..")) {
			minValue = Int.MIN_VALUE
			maxValue = readInt()
		} else {
			minValue = readInt()
			maxValue = if (tryRead("..")) {
				if (canRead() && peek().let{it == '-' || it in '0'..'9'}) {
					readInt()
				} else {
					Int.MAX_VALUE
				}
			} else {
				minValue
			}
		}
		return IntRange(minValue, maxValue)
	}catch (ex:ReaderException){
		throw ReaderException("expected to read a valid range", ex)
	}
}
//endregion



