/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package kroppeb.server.command.commands

import com.mojang.brigadier.ResultConsumer
import com.mojang.brigadier.exceptions.CommandSyntaxException
import kroppeb.server.command.*
import kroppeb.server.command.arguments.Score
import kroppeb.server.command.arguments.ScoreComparator
import kroppeb.server.command.arguments.ScoreHolder
import kroppeb.server.command.arguments.SingleScore
import kroppeb.server.command.arguments.selector.Selector
import kroppeb.server.command.arguments.selector.Selector.SingleSelector
import kroppeb.server.command.reader.*
import net.minecraft.block.pattern.CachedBlockPosition
import net.minecraft.command.argument.EntityAnchorArgumentType.EntityAnchor
import net.minecraft.command.argument.NbtPathArgumentType.NbtPath
import net.minecraft.command.argument.PosArgument
import net.minecraft.entity.Entity
import net.minecraft.nbt.*
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.CommandOutput
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.Direction
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec2f
import net.minecraft.util.math.Vec3d
import net.minecraft.util.registry.Registry
import net.minecraft.util.registry.RegistryKey
import org.apache.commons.lang3.NotImplementedException
import java.util.*
import java.util.function.Predicate


class ExecuteCommand : Command {
	private lateinit var output: CommandOutput
	private lateinit var pos: Vec3d
	private lateinit var rot: Vec2f
	private lateinit var world: ServerWorld
	private var level: Int = 0 // Can't use lateinit
	private lateinit var simpleName: String
	private lateinit var name: Text
	private lateinit var server: MinecraftServer
	private var entity: Entity? = null // Can't use lateinit
	private var silent: Boolean = false // Can't use lateinit
	private lateinit var resultConsumer: ResultConsumer<ServerCommandSource>
	private lateinit var entityAnchor: EntityAnchor

	var first: Converter? = null
	private var store: StoreResolver? = null
	override fun execute(source: ServerCommandSource): Int {
		output = source.output
		pos = source.position
		rot = source.rotation
		world = source.world
		level = source.level
		simpleName = source.name
		name = source.displayName
		server = source.minecraftServer
		entity = source.entity
		silent = source.silent
		resultConsumer = source.resultConsumer
		entityAnchor = source.entityAnchor
		first!!.call()
		return 0 // we optimize `run execute` away. No need to return anything;
	}

	// TODO try to replace as many calls to this as possible
	@Deprecated("")
	private fun source(): ServerCommandSource {
		return ServerCommandSource(
			output,
			pos,
			rot,
			world,
			level,
			simpleName,
			name,
			server,
			entity)
	}

	@Throws(ReaderException::class)
	private fun Reader.readConverter(): Converter {
		return when (val subCommand = Literal()) {
			"align" -> Align(Swizzle(), readConverter())
			"anchored" -> Anchored(
				when (val type = Literal()) {
					"eyes" -> EntityAnchor.EYES
					"feet" -> EntityAnchor.FEET
					else -> throw ReaderException("Expected <eyes/feet> but got: $type")
				}, readConverter())
			"as" -> As(Selector(), readConverter())
			"at" -> At(Selector(), readConverter())
			"in" -> {
				val id = Id()
				val registryKey = RegistryKey.of(Registry.DIMENSION, id)
				val dim = getServer() //TODO get server
					.getWorld(registryKey)
					?: throw ReaderException("unknown dimension: $id")
				In(dim, readConverter())
			}
			"facing" ->
				if (tryReadLiteral("entity")) FacingEntity(
					Selector(),
					when (val facing = Literal()) {
						"eyes" -> EntityAnchor.EYES
						"feet" -> EntityAnchor.FEET
						else -> throw ReaderException("unknown facing anchor: $Literal")
					}, readConverter())
				else FacingPos(Pos(), readConverter())
			"positioned" ->
				if (tryReadLiteral("as")) PositionedAs(Selector(), readConverter())
				else Positioned(Pos(), readConverter())
			"rotated" ->
				if (tryReadLiteral("as")) RotatedAs(Selector(), readConverter())
				else throw NotImplementedException("todo")
			"store" -> this.readStore()
			"if" -> this.readIf(true)
			"unless" -> this.readIf(false)
			"run" -> Run(Parser.readFunction(this))
			else -> throw ReaderException("Unknown subcommand: $subCommand")
		}
	}

	@Throws(ReaderException::class)
	fun Reader.readIf(positive: Boolean): Converter {
		return when (val type = Literal()) {
			"block" -> IfBlock(Pos(), BlockTagPredicate(), positive, tryReadConverter())
			"entity" -> IfEntity(Selector(), positive, tryReadConverter())
			"score" -> IfScore(SingleScore(), ScoreComparator.invoke(), positive, tryReadConverter())
			else -> throw ReaderException("Unknown if subcommand: $type")
		}
	}

	@Throws(ReaderException::class)
	fun Reader.tryReadConverter(): Converter? {
		return if (!canRead()) null else readConverter()
	}

	@Throws(ReaderException::class)
	fun Reader.readStore(): Store {
		val save = Literal()
		val result: Boolean = when (save) {
			"result" -> true
			"success" -> false
			else -> throw ReaderException("expected result/success, got: $save")
		}
		return when (val type = Literal()) {
			"block" -> StoreBlock(result, Pos(), Path(), Cast(), Double(), readConverter())
			"bossbar" -> StoreBossBar(
				result, Id(), when (Literal()) {
				"value" -> true
				"max" -> false
				else -> throw ReaderException("expected result/success, got: $save")
			}, readConverter())
			"entity" -> StoreEntity(result, SingleSelector(), Path(), Cast(), Double(), readConverter())
			"score" -> StoreScore(result, ScoreHolder(), Literal(), readConverter()) // TODO fix store score
			"storage" -> StoreStorage(result, Id(), Path(), Cast(), Double(), readConverter())
			else -> throw ReaderException("Unknown store subcommand: $type")
		}
	}

	abstract class Converter {
		abstract fun call()
	}

	inner class Align(val axes: EnumSet<Direction.Axis?>, val next: Converter) : Converter() {
		override fun call() {
			val posCache = pos
			pos = pos.floorAlongAxes(axes)
			next.call()
			pos = posCache
		}

	}

	inner class Anchored(val ea: EntityAnchor, val next: Converter) : Converter() {
		override fun call() {
			val entityAnchorCache = ea
			entityAnchor = ea
			next.call()
			entityAnchor = entityAnchorCache
		}

	}

	inner class As(val selector: Selector, val next: Converter) : Converter() {
		override fun call() {
			val entityCache = entity
			for (selectorEntity in selector.getEntities(world, pos, entity)) {
				entity = selectorEntity
				next.call()
			}
			entity = entityCache
		}

	}

	inner class At(val selector: Selector, val next: Converter) : Converter() {
		override fun call() {
			val posCache = pos
			val rotCache = rot
			for (selectorEntity in selector.getEntities(world, pos, entity)) {
				pos = selectorEntity.pos
				rot = selectorEntity.rotationClient
				next.call()
			}
			pos = posCache
			rot = rotCache
		}

	}

	inner class FacingPos(
		val posArgument: PosArgument,
		val next: Converter) : Converter() {
		override fun call() {
			val rotCache = rot
			val start: Vec3d = if (entity == null) pos else entityAnchor.offset.apply(pos, entity)
			val end = posArgument.toAbsolutePos(source())
			val d = end.x - start.x
			val e = end.y - start.y
			val f = end.z - start.z
			val g = MathHelper.sqrt(d * d + f * f).toDouble()
			val h = MathHelper
				.wrapDegrees((-(MathHelper.atan2(e, g) * 57.2957763671875)).toFloat()) // almost 180 / pi
			val i = MathHelper.wrapDegrees((MathHelper.atan2(f, d) * 57.2957763671875).toFloat() - 90.0f)
			rot = Vec2f(h, i)
			next.call()
			rot = rotCache
		}

	}

	inner class FacingEntity(
		val selector: Selector,
		private val anchor: EntityAnchor,
		val next: Converter) : Converter() {
		override fun call() {
			val rotCache = rot
			val start: Vec3d = if (entity == null) pos else entityAnchor.offset.apply(pos, entity)
			for (selectorEntity in selector.getEntities(world, pos, entity)) {
				val end = anchor.positionAt(selectorEntity)
				val d = end.x - start.x
				val e = end.y - start.y
				val f = end.z - start.z
				val g = MathHelper.sqrt(d * d + f * f).toDouble()
				val h = MathHelper
					.wrapDegrees((-(MathHelper.atan2(e, g) * 57.2957763671875f)).toFloat()) // (180 - 1e-5) / pi
				val i = MathHelper.wrapDegrees((MathHelper.atan2(f, d) * 57.2957763671875f).toFloat() - 90.0f)
				rot = Vec2f(h, i)
				next.call()
			}
			rot = rotCache
		}

	}

	inner class In(
		val dimension: ServerWorld,
		val next: Converter) : Converter() {
		override fun call() {
			val worldCache = world
			world = dimension
			next.call()
			world = worldCache
		}

	}

	inner class Positioned(
		val innerPos: PosArgument,
		val next: Converter) : Converter() {
		override fun call() {
			val posCache: Vec3d = pos
			pos = innerPos.toAbsolutePos(source())
			next.call()
			pos = posCache
		}

	}

	inner class PositionedAs(
		val selector: Selector,
		val next: Converter) : Converter() {
		override fun call() {
			val posCache = pos
			val r = 0
			for (selectorEntity in selector.getEntities(world, pos, entity)) {
				pos = selectorEntity.pos
				next.call()
			}
			pos = posCache
		}

	}

	inner class Rotated(
		val innerRot: Vec2f,
		val next: Converter) : Converter() {
		override fun call() {
			val rotCache = innerRot
			rot = innerRot
			next.call()
			rot = rotCache
		}

	}

	inner class RotatedAs(
		val selector: Selector,
		val next: Converter) : Converter() {
		override fun call() {
			val rotCache = rot
			for (selectorEntity in selector.getEntities(world, pos, entity)) {
				rot = selectorEntity.rotationClient
				next.call()
			}
			rot = rotCache
		}

	}

	internal enum class Cast {
		BYTE {
			override fun convert(input: Double): AbstractNumberTag? {
				return ByteTag.of(input.toInt().toByte())
			}
		},
		SHORT {
			override fun convert(input: Double): AbstractNumberTag? {
				return ShortTag.of(input.toInt().toShort())
			}
		},
		INT {
			override fun convert(input: Double): AbstractNumberTag? {
				return IntTag.of(input.toInt())
			}
		},
		LONG {
			override fun convert(input: Double): AbstractNumberTag? {
				return LongTag.of(input.toLong())
			}
		},
		FLOAT {
			override fun convert(input: Double): AbstractNumberTag? {
				return FloatTag.of(input.toFloat())
			}
		},
		DOUBLE {
			override fun convert(input: Double): AbstractNumberTag? {
				return DoubleTag.of(input)
			}
		};

		abstract fun convert(input: Double): AbstractNumberTag?

		companion object : ReadFactory<Cast> {
			@Throws(ReaderException::class)
			override fun Reader.parse(): Cast {
				return when (val type = readUntilWhitespace()) {
					"byte" -> BYTE
					"short" -> SHORT
					"int" -> INT
					"long" -> LONG
					"float" -> FLOAT
					"double" -> DOUBLE
					else -> throw ReaderException("Unknown datatype: $type")
				}
			}
		}
	}

	abstract inner class StoreResolver protected constructor(val result: Boolean) {
		val prev: StoreResolver?
		fun fail() {
			set(0)
			prev?.fail()
		}

		fun success(value: Int) {
			set(if (result) value else 1)
			prev?.success(value)
		}

		abstract fun set(value: Int)
		fun restore() {
			store = prev
		}

		init {
			prev = store
			store = this
		}
	}

	abstract inner class Store protected constructor(
		val result: Boolean,
		val next: Converter) : Converter() {
		override fun call() {
			val resolver = this.resolve()
			if (resolver == null) {
				if (store != null) store!!.fail()
				return
			}
			next.call()
			resolver.restore()
		}

		protected abstract fun resolve(): StoreResolver?

	}

	internal abstract inner class NbtContainer protected constructor(
		next: Converter,
		result: Boolean,
		val path: NbtPath,
		val cast: Cast,
		val scale: Double) : Store(result, next)

	internal inner class StoreBlock(
		result: Boolean,
		val pos: PosArgument,
		path: NbtPath,
		cast: Cast,
		scale: Double,
		next: Converter) : NbtContainer(next, result, path, cast, scale) {
		override fun resolve(): StoreResolver? {
			val blockPos = pos.toAbsoluteBlockPos(source())
			val block = world.getBlockEntity(blockPos) ?: return null
			return object : StoreResolver(result) {
				override fun set(value: Int) {
					val tag = CompoundTag()
					block.toTag(tag)
					try {
						path.put(tag) { cast.convert(value * scale) }
					} catch (e: CommandSyntaxException) {
						return
					}
					block.fromTag(block.cachedState, tag)
				}
			}
		}

	}

	internal inner class StoreEntity(
		result: Boolean,
		val selector: SingleSelector,
		path: NbtPath,
		cast: Cast,
		scale: Double,
		next: Converter) : NbtContainer(next, result, path, cast, scale) {
		override fun resolve(): StoreResolver? {
			val entity = selector.getEntity(source()) ?: return null
			return object : StoreResolver(result) {
				override fun set(value: Int) {
					val tag = CompoundTag()
					entity.toTag(tag)
					try {
						path.put(tag) { cast.convert(value * scale) }
					} catch (e: CommandSyntaxException) {
						return
					}
					entity.fromTag(tag)
				}
			}
		}

	}

	internal inner class StoreStorage(
		result: Boolean,
		val id: Identifier,
		path: NbtPath,
		cast: Cast,
		scale: Double,
		next: Converter) : NbtContainer(next, result, path, cast, scale) {
		override fun resolve(): StoreResolver? {
			val storage = server.dataCommandStorage
			return object : StoreResolver(result) {
				override fun set(value: Int) {
					val tag = storage[id]
					try {
						path.put(tag) { cast.convert(value * scale) }
					} catch (e: CommandSyntaxException) {
						return
					}
					storage[id] = tag
				}
			}
		}

	}

	internal inner class StoreBossBar(
		result: Boolean,
		val id: Identifier?,
		val value: Boolean,
		next: Converter) : Store(result, next) {
		override fun resolve(): StoreResolver? {
			val manager = server.bossBarManager
			val bar = manager[id] ?: return null
			return object : StoreResolver(result) {
				override fun set(value: Int) {
					if (this@StoreBossBar.value) {
						bar.value = value
					} else {
						bar.maxValue = value
					}
				}
			}
		}

	}

	internal inner class StoreScore(
		result: Boolean,
		val holder: ScoreHolder,
		val scoreboard: String?,
		next: Converter) : Store(result, next) {
		override fun resolve(): StoreResolver? {
			// TODO implement store score.
			throw RuntimeException()
		}

	}

	inner class IfBlock(
		val pos: PosArgument,
		val predicate: Predicate<CachedBlockPosition>,
		val positive: Boolean,
		val next: Converter?) : Converter() {
		override fun call() {
			val blockPos = pos.toAbsoluteBlockPos(source())
			if (!world.isChunkLoaded(blockPos)) return
			val cachedBlock = CachedBlockPosition(world, blockPos, true)
			if (predicate.test(cachedBlock) == positive) next?.call()
				?: TODO("I think I still have to do something here")
		}

	}

	inner class IfEntity(
		val selector: Selector,
		val positive: Boolean,
		val next: Converter?) : Converter() {
		override fun call() {
			// TODO if(selector.exists())
			if (!selector.getEntities(world, pos, entity).isEmpty() == positive) {
				next?.call()
					?: TODO("should be nullable? I think adding a virutal run at the end might be better if possible")
			}
		}

	}

	inner class IfScore(
		val score: SingleScore,
		val comparator: ScoreComparator,
		val positive: Boolean,
		val next: Converter?) : Converter() {
		override fun call() {
			if (comparator.compareTo(score, source()) == positive) {
				next?.call() ?: TODO("should be nullable")
			}
		}

	}

	inner class Run(val cmd: Command?) : Converter() {
		override fun call() {
			try {
				cmd!!.execute(source())
			} catch (invocationError: InvocationError) {
				TODO("save values.")
			}
		}

	}

	companion object : ReadFactory<ExecuteCommand> {
		@Throws(ReaderException::class)
		override fun Reader.parse(): ExecuteCommand = with(ExecuteCommand()) {
			readConverter()
			this
		}
	}
}
