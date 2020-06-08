/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package kroppeb.server.command.commands

import com.mojang.datafixers.types.templates.Sum
import kroppeb.server.command.Command
import kroppeb.server.command.InvocationError
import kroppeb.server.command.arguments.Resource
import kroppeb.server.command.arguments.readCompoundTag
import kroppeb.server.command.arguments.readPos
import kroppeb.server.command.reader.*
import net.minecraft.command.arguments.PosArgument
import net.minecraft.entity.*
import net.minecraft.entity.mob.MobEntity
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

abstract class SummonCommand : Command {
	class SummonDefault(val pos: PosArgument?, val tag: CompoundTag?, val initialize: Boolean) : SummonCommand() {
		@Throws(InvocationError::class)
		override fun execute(source: ServerCommandSource): Int {
			val world = source.world
			val pos = pos!!.toAbsolutePos(source)
			require(World.method_25953(BlockPos(pos))) // TODO better errors
			val entity2 = EntityType.loadEntityWithPassengers(tag, world) { entityx: Entity ->
				entityx.refreshPositionAndAngles(pos.x, pos.y, pos.z, entityx.yaw, entityx.pitch)
				if (world.tryLoadEntity(entityx)) entityx else null
			}
			if (entity2 == null) {
				throw InvocationError()
			} else {
				if (initialize && entity2 is MobEntity) {
					entity2.initialize(world, world.getLocalDifficulty(entity2
							.getBlockPos()),TODO("huh") , null as EntityData?, null as CompoundTag?)
				}
			}
			return 1
		}

	}

	class SummonLightning(val pos: PosArgument?) : SummonCommand() {
		@Throws(InvocationError::class)
		override fun execute(source: ServerCommandSource): Int {
			val world = source.world
			val pos = pos!!.toAbsolutePos(source)
			if (!World.method_25953(BlockPos(pos))) throw InvocationError()
			val lightningEntity = LightningEntity(world, pos.x, pos.y, pos.z, false)
			world.addLightning(lightningEntity)
			return 1
		}

	}

	companion object:ReadFactory<SummonCommand> {
		fun of(type: Resource, pos: PosArgument?, tag: CompoundTag?): SummonCommand {
			var tag = tag
			return if (!(type.namespace == null || type.namespace == "minecraft") && type.path.size == 1 && (type.path[0]
							== "lightning_bolt")) {
				val init = tag == null
				if (init) tag = CompoundTag()
				tag!!.putString("id", type.toString())
				SummonDefault(pos, tag, init)
			} else {
				SummonLightning(pos)
			}
		}

		@Throws(ReaderException::class)
		override fun Reader.parse(): SummonCommand {
			val entityType: Resource = Resource()
			var pos: PosArgument? = null
			var tag: CompoundTag? = null
			if (canRead()) {
				pos = Pos()
				if (canRead()) tag = Compound()
			}
			return of(entityType, pos, tag)
		}
	}
}
