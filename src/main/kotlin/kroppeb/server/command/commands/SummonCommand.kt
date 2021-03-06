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
import net.minecraft.command.argument.PosArgument
import net.minecraft.entity.*
import net.minecraft.entity.mob.MobEntity
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

class SummonCommand(val pos: PosArgument?, val tag: CompoundTag, val initialize: Boolean)
	: Command {

	override fun execute(source: ServerCommandSource): Int {
		val world = source.world
		val pos = pos!!.toAbsolutePos(source)
		require(World.isValid(BlockPos(pos))) // TODO better errors

		// todo check if the copy is needed
		val entity2 = EntityType.loadEntityWithPassengers(tag.copy(), world) { entity: Entity ->
			entity.refreshPositionAndAngles(pos.x, pos.y, pos.z, entity.yaw, entity.pitch)
			entity
		}
			?: throw InvocationError()

		if (initialize && entity2 is MobEntity) {
			entity2.initialize(
				world,
				world.getLocalDifficulty(entity2.getBlockPos()),
				SpawnReason.COMMAND,
				null,
				null
			)
		}
		if(!world.shouldCreateNewEntityWithPassenger(entity2))
			throw InvocationError()

		return 1
	}


	companion object : ReadFactory<SummonCommand> {
		fun of(type: Identifier, pos: PosArgument?, tag: CompoundTag?): SummonCommand {
			val init = tag == null
			val ctag = tag ?: CompoundTag()
			ctag.putString("id", type.toString())
			return SummonCommand(pos, ctag, init)

		}

		override fun Reader.parse(): SummonCommand {
			val entityType = Id()
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
