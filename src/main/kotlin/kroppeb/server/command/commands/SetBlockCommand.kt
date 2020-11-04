/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package kroppeb.server.command.commands

import kroppeb.server.command.Command
import kroppeb.server.command.InvocationError
import kroppeb.server.command.reader.*
import kroppeb.server.command.toLoadedBlockPosition
import net.minecraft.block.pattern.CachedBlockPosition
import net.minecraft.command.arguments.BlockStateArgument
import net.minecraft.command.arguments.PosArgument
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Clearable
import net.minecraft.util.math.BlockPos


class SetBlockCommand(val pos: PosArgument, val block: BlockStateArgument, val type: Type) : Command {
	enum class Type {
		Replace {
			override fun execute(pos: BlockPos, block: BlockStateArgument, serverWorld: ServerWorld): Int {
				Clearable.clear(serverWorld.getBlockEntity(pos))
				if (!block.setBlockState(serverWorld, pos, 2))
					throw InvocationError()
				serverWorld.updateNeighbors(pos, block.blockState.block)
				return 1
			}
		},
		Destroy {
			override fun execute(pos: BlockPos, block: BlockStateArgument, serverWorld: ServerWorld): Int {
				serverWorld.breakBlock(pos, true)
				if (!block.blockState.isAir || !serverWorld.getBlockState(pos).isAir)
					if (!block.setBlockState(serverWorld, pos, 2))
						throw InvocationError()

				serverWorld.updateNeighbors(pos, block.blockState.block)
				return 1
			}
		},
		Keep {
			override fun execute(pos: BlockPos, block: BlockStateArgument, serverWorld: ServerWorld): Int {
				val cbp = CachedBlockPosition(serverWorld, pos, true)
				if (!cbp.world.isAir(cbp.blockPos))
					throw InvocationError()
				return Replace.execute(pos, block, serverWorld)
			}
		};

		abstract fun execute(pos: BlockPos, block: BlockStateArgument, serverWorld: ServerWorld): Int
	}


	companion object : ReadFactory<SetBlockCommand> {
		override fun Reader.parse(): SetBlockCommand {
			val pos = Pos()
			val block = BlockState()
			return SetBlockCommand(
				pos, block,
				if (canRead()) {
					when (val type = Literal()) {
						"replace" -> Type.Replace
						"destroy" -> Type.Destroy
						"keep" -> Type.Keep
						else -> expected("setblock <pos> <block>", "[replace|destroy|keep]", type)
					}
				} else {
					Type.Replace
				}
			)
		}

	}

	override fun execute(source: ServerCommandSource): Int {
		val pos = this.pos.toLoadedBlockPosition(source) ?: throw InvocationError()
		return type.execute(pos, block, source.world)
	}
}
