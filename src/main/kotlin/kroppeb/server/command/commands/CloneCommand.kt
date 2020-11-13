/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package kroppeb.server.command.commands

import com.google.common.collect.Lists
import kroppeb.server.command.Command
import kroppeb.server.command.InvocationError
import kroppeb.server.command.reader.*
import net.minecraft.block.Blocks
import net.minecraft.block.pattern.CachedBlockPosition
import net.minecraft.command.argument.BlockPosArgumentType
import net.minecraft.command.argument.PosArgument
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.command.CloneCommand.*
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.TranslatableText
import net.minecraft.util.Clearable
import net.minecraft.util.math.BlockBox
import net.minecraft.util.math.BlockPos
import java.util.*
import java.util.function.Predicate


class CloneCommand(
	val begin: PosArgument,
	val end: PosArgument,
	val destination: PosArgument,
	val filter: Predicate<CachedBlockPosition>,
	val mode: Mode)
	: Command {
	companion object : ReadFactory<CloneCommand> {

		override fun Reader.parse(): CloneCommand {
			val begin = Pos()
			val end = Pos()
			val destination = Pos()
			if (!canRead())
				return CloneCommand(begin, end, destination, { true }, Mode.NORMAL)
			val filter: Predicate<CachedBlockPosition> = when (val filterType = Literal()) {
				"replace" -> Predicate<CachedBlockPosition> { true }
				"masked" -> Predicate<CachedBlockPosition> { !it.blockState.isAir }
				"filter" -> BlockTagPredicate()
				else -> expected("clone <begin> <end> <destination>", "[replace|masked|filter]", filterType)
			}
			if(!canRead())
				return CloneCommand(begin, end, destination, filter, Mode.NORMAL)
			val mode = when(val modeString = Literal()){
				"normal" -> Mode.NORMAL
				"force" -> Mode.FORCE
				"move" -> Mode.MOVE
				else -> expected("clone <begin> <end> <destination> [replace|masked|filtered <filter>]", "[force|move|normal]", modeString)
			}
			return CloneCommand(begin, end, destination, filter, mode)
		}
	}

	// TODO: Optimize clone command
	override fun execute(source: ServerCommandSource): Int {
		val begin = begin.toAbsoluteBlockPos(source)
		val end = end.toAbsoluteBlockPos(source)
		val destination = destination.toAbsoluteBlockPos(source)

		val from = BlockBox(begin, end)
		val destinationEnd: BlockPos = destination.add(from.dimensions)
		val to = BlockBox(destination, destinationEnd)
		if (!mode.allowsOverlap() && to.intersects(from)) {
			throw InvocationError()
		}
		val size = from.blockCountX * from.blockCountY * from.blockCountZ
		if (size > 32768) {
			throw InvocationError()
		}

		val serverWorld = source.world
		if (!serverWorld.isRegionLoaded(begin, end) || !serverWorld.isRegionLoaded(destination, destinationEnd)) {
			throw InvocationError()
		}

		val list: MutableList<BlockInfo> = mutableListOf()
		val list2: MutableList<BlockInfo> = mutableListOf()
		val list3: MutableList<BlockInfo> = mutableListOf()
		val deque: Deque<BlockPos> = Lists.newLinkedList()
		val moveOffset = BlockPos(
			to.minX - from.minX,
			to.minY - from.minY,
			to.minZ - from.minZ)

		for (z in from.minZ..from.maxZ) {
			for (y in from.minY..from.maxY) {
				for (x in from.minX..from.maxX) {
					val fromBlock = BlockPos(x, y, z)
					val toBlock = fromBlock.add(moveOffset)
					val cachedBlockPosition = CachedBlockPosition(serverWorld, fromBlock, false)
					val blockState = cachedBlockPosition.blockState
					if (filter.test(cachedBlockPosition)) {
						// TODO: Can this be `cachedBlockPosition.blockEntity`
						val blockEntity = serverWorld.getBlockEntity(fromBlock)
						if (blockEntity != null) {
							val compoundTag = blockEntity.toTag(CompoundTag())
							list2.add(BlockInfo(toBlock, blockState, compoundTag))
							deque.addLast(fromBlock)
						} else if (!blockState.isOpaqueFullCube(
								serverWorld,
								fromBlock) && !blockState.isFullCube(serverWorld, fromBlock)) {
							list3.add(BlockInfo(toBlock, blockState, null))
							deque.addFirst(fromBlock)
						} else {
							list.add(BlockInfo(toBlock, blockState, null))
							deque.addLast(fromBlock)
						}
					}
				}
			}
		}

		if (mode == Mode.MOVE) {
			for( blockPos6 in deque) {
				val blockEntity2 = serverWorld.getBlockEntity(blockPos6)!!
				Clearable.clear(blockEntity2)
				serverWorld.setBlockState(blockPos6, Blocks.BARRIER.defaultState, 2)
			}
			for( blockPos6 in deque) {
				serverWorld.setBlockState(blockPos6, Blocks.AIR.defaultState, 3)
			}
		}
		val list4: MutableList<BlockInfo> = Lists.newArrayList()
		list4.addAll(list)
		list4.addAll(list2)
		list4.addAll(list3)
		val list5 = Lists.reverse(list4)

		for(blockInfo in list5) {
			val blockEntity3 = serverWorld.getBlockEntity(blockInfo.pos)!!
			Clearable.clear(blockEntity3)
			serverWorld.setBlockState(blockInfo.pos, Blocks.BARRIER.defaultState, 2)
		}
		var m = 0
		for(blockInfo4 in list4) {
			if (serverWorld.setBlockState(blockInfo4.pos, blockInfo4.state, 2)) {
				++m
			}
		}

		for(blockInfo4 in list2) {
			val blockEntity4 = serverWorld.getBlockEntity(blockInfo4.pos)
			if (blockInfo4.blockEntityTag != null && blockEntity4 != null) {
				blockInfo4.blockEntityTag!!.putInt("x", blockInfo4.pos.x)
				blockInfo4.blockEntityTag!!.putInt("y", blockInfo4.pos.y)
				blockInfo4.blockEntityTag!!.putInt("z", blockInfo4.pos.z)
				blockEntity4.fromTag(blockInfo4.state, blockInfo4.blockEntityTag)
				blockEntity4.markDirty()
			}
			serverWorld.setBlockState(blockInfo4.pos, blockInfo4.state, 2)
		}

		for(blockInfo4 in list5){
			serverWorld.updateNeighbors(blockInfo4.pos, blockInfo4.state.block)
		}
		serverWorld.blockTickScheduler.copyScheduledTicks(from, moveOffset)
		if (m == 0) {
			throw InvocationError()
		}
		return m
	}

}
