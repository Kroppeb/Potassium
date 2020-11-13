/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package kroppeb.server.command.arguments

import kroppeb.server.command.arguments.selector.Selector.SingleSelector
import kroppeb.server.command.reader.*
import net.minecraft.command.argument.PosArgument
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.util.Identifier

interface NbtDataContainer {
	fun getTag(source: ServerCommandSource): CompoundTag
	fun setTag(source: ServerCommandSource, tag: CompoundTag?)
	class Block(val pos: PosArgument) : NbtDataContainer {
		override fun getTag(source: ServerCommandSource): CompoundTag {
			val blockPos = pos.toAbsoluteBlockPos(source)
			val world = source.world
			if (!world.isChunkLoaded(blockPos)) {
				throw RuntimeException("not loaded") // TODO decent error
			}
			val tag = CompoundTag()
			world.getBlockEntity(blockPos)!!.toTag(tag) // TODO check null
			return tag
		}

		override fun setTag(source: ServerCommandSource, tag: CompoundTag?) {
			val blockPos = pos.toAbsoluteBlockPos(source)
			val world = source.world
			if (!world.isChunkLoaded(blockPos)) {
				throw RuntimeException("not loaded") // TODO decent error
			}
			val blockEntity = world.getBlockEntity(blockPos)
			blockEntity!!.fromTag(blockEntity.cachedState, tag) //TODO check null
		}

	}

	class Entity internal constructor(val selector: SingleSelector) : NbtDataContainer {
		override fun getTag(source: ServerCommandSource): CompoundTag {
			val tag = CompoundTag()
			selector.getEntity(source)!!.toTag(tag) // TODO check null?
			return tag
		}

		override fun setTag(source: ServerCommandSource, tag: CompoundTag?) {
			selector.getEntity(source)!!.fromTag(tag)
		}

	}

	class Storage internal constructor(val id: Identifier?) : NbtDataContainer {
		override fun getTag(source: ServerCommandSource): CompoundTag {
			return source.minecraftServer.dataCommandStorage[id]
		}

		override fun setTag(source: ServerCommandSource, tag: CompoundTag?) {
			source.minecraftServer.dataCommandStorage[id] = tag
		}

	}
	@ReaderDslMarker
	companion object: ReadFactory<NbtDataContainer> {
		@Throws(ReaderException::class)
		/* TODO we are an arg so the last call can't be a invoke but should be a read. Maybe make this easier to notice
		    or make it cache the fact we read a space last.
		* */
		override fun Reader.parse(): NbtDataContainer {
			return when (val type = Literal()) {
				"block" -> Block(Pos.read(this))
				"entity" -> Entity(SingleSelector.read(this))
				"storage" -> Storage(Id.read(this))
				else -> throw ReaderException("Unknown bt storage: $type")
			}
		}
	}
}
