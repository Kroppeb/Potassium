/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package kroppeb.server.command.commands

import com.google.common.collect.Iterables
import com.mojang.brigadier.exceptions.CommandSyntaxException
import kroppeb.server.command.Command
import kroppeb.server.command.InvocationError
import kroppeb.server.command.arguments.NbtDataContainer
import kroppeb.server.command.arguments.NbtDataSource
import kroppeb.server.command.arguments.NbtDataSource.NbtDataPathSource
import kroppeb.server.command.arguments.readPath
import kroppeb.server.command.commands.DataCommand.Modify.*
import kroppeb.server.command.reader.*
import net.minecraft.command.argument.NbtPathArgumentType.NbtPath
import net.minecraft.nbt.*
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.util.math.MathHelper

abstract class DataCommand protected constructor(val target: NbtDataContainer) : Command {
	class Get(target: NbtDataContainer, path: NbtPath?, val scale: Double?) : DataCommand(target) {
		val dataSource: NbtDataPathSource = NbtDataPathSource(target, path)

		@Throws(InvocationError::class)
		override fun execute(source: ServerCommandSource): Int {
			if (dataSource.path == null) {
				target.getTag(source)
				return 1
			}
			val tag = dataSource.getTag(source)
			return when {
				scale == null -> when (tag) {
					is AbstractNumberTag -> MathHelper.floor(tag.double)
					is AbstractListTag<*> -> tag.size
					is CompoundTag -> tag.size
					is StringTag -> tag.asString().length
					else -> throw InvocationError()
				}
				tag is AbstractNumberTag -> {
					MathHelper.floor(tag.double * scale)
				}
				else -> throw InvocationError()
			}
		}

	}

	class Merge(target: NbtDataContainer, val tag: CompoundTag) : DataCommand(target) {
		@Throws(InvocationError::class)
		override fun execute(source: ServerCommandSource): Int {
			val data = target.getTag(source)
			val result = data.copy().copyFrom(tag)
			if (data == result) throw InvocationError()
			target.setTag(source, result)
			return 1
		}

	}

	class Remove(target: NbtDataContainer, val path: NbtPath) : DataCommand(target) {
		@Throws(InvocationError::class)
		override fun execute(source: ServerCommandSource): Int {
			val data = target.getTag(source)

			when (val i = path.remove(data)) {
				0 -> throw InvocationError()
				else -> {
					target.setTag(source, data)
					return i
				}
			}
		}
	}

	abstract class Modify(target: NbtDataContainer, val path: NbtPath, val source: NbtDataSource?) :
			DataCommand(target) {

		class Append(target: NbtDataContainer, path: NbtPath, source: NbtDataSource?) :
				Modify(target, path, source) {
			override fun execute(source: ServerCommandSource): Int {
				val tag = target.getTag(source)
				val i = insert(-1, tag, path, this.source!!.getData(source)) // shouldn't be 0 if error is throw.
				target.setTag(source, tag)
				return i
			}
		}

		class Prepend(target: NbtDataContainer, path: NbtPath, source: NbtDataSource?) : Modify(
				target,
				path,
				source) {
			override fun execute(source: ServerCommandSource): Int {
				val tag = target.getTag(source)
				val i = insert(0, tag, path, this.source!!.getData(source)) // shouldn't be 0 if error is throw.
				target.setTag(source, tag)
				return i
			}
		}

		class Insert(target: NbtDataContainer, path: NbtPath, source: NbtDataSource?, val index: Int) : Modify(
				target,
				path,
				source) {
			override fun execute(source: ServerCommandSource): Int {
				val tag = target.getTag(source)
				val i = insert(index, tag, path, this.source!!.getData(source)) // shouldn't be 0 if error is throw.
				target.setTag(source, tag)
				return i
			}

		}

		class Set(target: NbtDataContainer, path: NbtPath, source: NbtDataSource?) : Modify(
				target,
				path,
				source) {
			@Throws(InvocationError::class)
			override fun execute(source: ServerCommandSource): Int {
				val tag = target.getTag(source)
				val last = Iterables.getLast(this.source!!.getData(source))
				return try {
					val i = path.put(tag) { last!!.copy() }
					target.setTag(source, tag)
					i
				} catch (e: CommandSyntaxException) {
					throw InvocationError()
				}
			}
		}

		class Merge(target: NbtDataContainer, path: NbtPath, source: NbtDataSource?) : Modify(
				target,
				path,
				source) {
			@Throws(InvocationError::class)
			override fun execute(source: ServerCommandSource): Int {
				val tags: List<Tag>
				val result = target.getTag(source)
				tags = try {
					path.getOrInit(result) { CompoundTag() }
				} catch (e: CommandSyntaxException) {
					throw InvocationError()
				}
				val list = this.source!!.getData(source)
				var i = 0
				var compoundTag2: CompoundTag
				var compoundTag3: CompoundTag
				for (tag in tags) {
					if (tag !is CompoundTag) {
						throw InvocationError()
					}
					compoundTag2 = tag
					compoundTag3 = compoundTag2.copy()
					for (tag2 in list!!) {
						if (tag2 !is CompoundTag) {
							throw InvocationError()
						}
						compoundTag2.copyFrom(tag2)
					}
					i += if (compoundTag3 == compoundTag2) 0 else 1
				}
				if (i == 0) throw InvocationError()
				target.setTag(source, result)
				return i
			}
		}

		companion object {
			@Throws(InvocationError::class)
			fun insert(integer: Int, sourceTag: CompoundTag?, path: NbtPath?, tags: List<Tag?>?): Int {
				// I was too lazy to copy
				return try {
					val i = net.minecraft.server.command.DataCommand.executeInsert(integer, sourceTag, path, tags)
					if (i == 0) {
						throw InvocationError()
					}
					i
				} catch (e: CommandSyntaxException) {
					throw InvocationError()
				}
			}
		}

	}

	companion object : ReadFactory<DataCommand> {
		@Throws(ReaderException::class)
		override fun Reader.parse(): DataCommand {
			return when (val type = Literal()) {
				"get" -> {
					val target = NbtDataContainer()
					if (canRead()) {
						val path = Path()
						if (canRead()) Get(target, path, Double())
						else Get(target, path, null)
					} else
						Get(target, null, null)
				}
				"merge" -> Merge(NbtDataContainer(), Compound())
				"modify" -> {
					val target = NbtDataContainer()
					val path = Path()
					when (val mode = Literal()) {
						"append" -> Append(target, path, NbtDataSource.invoke())
						"insert" -> Insert(target, path, NbtDataSource.invoke(), Int())
						"merge" -> Merge(target, path, NbtDataSource.invoke())
						"prepend" -> Prepend(target, path, NbtDataSource.invoke())
						"set" -> Set(target, path, NbtDataSource.invoke())
						else -> throw ReaderException("Unknown nbt operation: $mode")
					}
				}
				"remove" -> Remove(NbtDataContainer(), Path())
				else -> throw ReaderException("Unknown subcommand: $type")
			}
		}
	}

}
