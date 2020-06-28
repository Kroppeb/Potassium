/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package kroppeb.server.command.arguments.selector

import kroppeb.server.command.arguments.selector.Selector.Companion.parse
import kroppeb.server.command.reader.Reader
import kroppeb.server.command.reader.ReaderException
import net.minecraft.entity.Entity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.Vec3d

interface SinglePlayerSelector : Selector.SingleSelector, PlayerSelector {
	override fun getEntities(world: ServerWorld, pos: Vec3d?, executor: Entity?): Collection<Entity> {
		return getPlayers(world, pos, executor)
	}

	override fun getEntity(world: ServerWorld, pos: Vec3d?, executor: Entity?): Entity? {
		return getPlayer(world, pos, executor)
	}

	override fun getPlayers(world: ServerWorld, pos: Vec3d?, executor: Entity?): Collection<ServerPlayerEntity> {
		val entity = getPlayer(world, pos, executor) ?: return emptySet()
		return setOf(entity)
	}

	fun getPlayer(world: ServerWorld, pos: Vec3d?, executor: Entity?): ServerPlayerEntity?
	fun getPlayer(source: ServerCommandSource): PlayerEntity? {
		return getPlayer(source.world, source.position, source.entity)
	}

	companion object {
		@Throws(ReaderException::class)
		fun Reader.readSinglePlayerSelector(): SinglePlayerSelector {
			val selector = parse()
			if (selector is SinglePlayerSelector) return selector
			throw ReaderException("not limited to 1 player") // TODO check limit value why
		}
	}
}
