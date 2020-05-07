/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package kroppeb.server.command.arguments

import kroppeb.server.command.Command
import kroppeb.server.command.reader.Reader
import kroppeb.server.command.reader.ReaderException
import net.minecraft.block.pattern.CachedBlockPosition
import net.minecraft.command.arguments.BlockStateArgument
import net.minecraft.command.arguments.PosArgument
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.util.Clearable

class SetBlockCommand(val pos: PosArgument, val block: BlockStateArgument, val destroy: Boolean, val keep: Boolean) : Command {
	override fun execute(source: ServerCommandSource): Int {
		val blockPos = pos.toAbsoluteBlockPos(source)
		val world = source.world
		if (!world.isChunkLoaded(blockPos)) return 0 // TODO throw here?
		val sourceBlock = CachedBlockPosition(world, blockPos, true)
		if (keep && sourceBlock.blockState.isAir) return 0 // TODO throw here?
		return if (destroy) {
			world.breakBlock(blockPos, true)
			if (block.blockState.isAir && sourceBlock.blockState.isAir ||
					block.setBlockState(world, blockPos, 2)) {
				world.updateNeighbors(blockPos, block.blockState.block)
				1
			} else {
				0 // TODO throw here?
			}
		} else {
			Clearable.clear(sourceBlock.blockEntity)
			if (block.setBlockState(world, blockPos, 2)) {
				world.updateNeighbors(blockPos, block.blockState.block)
				1
			} else {
				0 // TODO throw here?
			}
		}
	}

	companion object {
		@Throws(ReaderException::class)
		fun read(reader: Reader): SetBlockCommand {
			val pos = reader.readPos()
			reader.moveNext()
			val block = reader.readBlock()
			if (reader.hasNext()) {
				val mode = reader.readLiteral()
				return when (mode) {
					"replace" -> SetBlockCommand(pos, block, false, false)
					"keep" -> SetBlockCommand(pos, block, false, true)
					"destroy" -> SetBlockCommand(pos, block, true, false)
					else -> throw ReaderException("Unexpected mode value: $mode")
				}
			}
			return SetBlockCommand(pos, block, false, false)
		}
	}

}