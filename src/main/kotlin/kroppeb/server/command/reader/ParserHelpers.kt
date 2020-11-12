/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package kroppeb.server.command.reader

import kroppeb.server.command.arguments.*
import net.minecraft.block.pattern.CachedBlockPosition
import net.minecraft.command.arguments.BlockPredicateArgumentType
import net.minecraft.command.arguments.ItemPredicateArgumentType
import net.minecraft.command.arguments.NbtPathArgumentType
import net.minecraft.command.arguments.NbtPathArgumentType.NbtPath
import net.minecraft.command.arguments.PosArgument
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.Direction
import net.minecraft.util.registry.Registry
import java.util.*
import java.util.function.Predicate

@ReaderDslMarker
object Literal:ReadFactory<String>{
	override fun Reader.parse(): String = readUntilWhitespace()
}
@ReaderDslMarker
object Path:ReadFactory<NbtPath>{
	override fun Reader.parse(): NbtPath = readPath()
}
@ReaderDslMarker
object Id: ReadFactory<Identifier> {
	override fun Reader.parse(): Identifier = readIdentifier()
}
@ReaderDslMarker
object Compound: ReadFactory<CompoundTag> {
	override fun Reader.parse(): CompoundTag = readCompoundTag()
}

object Swizzle: ReadFactory<EnumSet<Direction.Axis?>> {
	override fun Reader.parse(): EnumSet<Direction.Axis?> = readSwizzle()
}

object Pos: ReadFactory<PosArgument> {
	override fun Reader.parse(): PosArgument = readPos()
}

object BlockPredicate: ReadFactory<BlockPredicateArgumentType.StatePredicate> {
	override fun Reader.parse(): BlockPredicateArgumentType.StatePredicate = readBlockPredicate(false) as BlockPredicateArgumentType.StatePredicate
}

object BlockTagPredicate: ReadFactory<Predicate<CachedBlockPosition>> {
	override fun Reader.parse(): Predicate<CachedBlockPosition> = readBlockPredicate(true)
}

object ItemPredicate: ReadFactory<ItemPredicateArgumentType.ItemPredicate> {
	override fun Reader.parse(): ItemPredicateArgumentType.ItemPredicate = readItemPredicate(false) as ItemPredicateArgumentType.ItemPredicate
}

object ItemTagPredicate: ReadFactory<Predicate<ItemStack>> {
	override fun Reader.parse(): Predicate<ItemStack> = readItemPredicate(true)
}

object Scoreboard: ReadFactory<String> {
	override fun Reader.parse(): String {
		val scoreboard = readUnquotedString()
		if (scoreboard.length > 16) throw ReaderException("scoreboard name too long")
		return scoreboard
	}
}

@Suppress("FunctionName")
@ReaderDslMarker
fun Reader.Boolean() = when(val s=Literal()){
	"true" -> true
	"false" -> false
	else -> throw ReaderException("Invalid boolean `$s`")
}

@Suppress("FunctionName")
@ReaderDslMarker
fun Reader.Int() = readAndMove { readInt() }

@Suppress("FunctionName")
@ReaderDslMarker
fun Reader.Double() = readAndMove { readDouble() }

@Suppress("FunctionName")
@ReaderDslMarker
fun Reader.Float() = readAndMove { readDouble() }.toFloat()

@Suppress("FunctionName")
@ReaderDslMarker
fun Reader.String() = readAndMove { readString() }

@Suppress("FunctionName")
@ReaderDslMarker
fun Reader.UUID() = try{UUID.fromString(Literal())}catch (e:IllegalAccessException){
	throw ReaderException("Invalid uuid", e)
}

@Suppress("FunctionName")
@ReaderDslMarker
fun Reader.Block() = Id().let{ Registry.BLOCK.getOrEmpty(it).orElse(null)?:throw ReaderException("unknown block: $it") }

@Suppress("FunctionName")
@ReaderDslMarker
fun Reader.BlockState() = readAndMove { readBlock() }

@Suppress("FunctionName")
@ReaderDslMarker
inline fun <reified T> Reader.JSON() = readAndMove { readJson<T>() }

@Suppress("FunctionName")
@ReaderDslMarker
fun Reader.Text() = JSON<Text>()

fun <T>Reader.read(factory: ReadFactory<T>) : T = factory.read(this)
