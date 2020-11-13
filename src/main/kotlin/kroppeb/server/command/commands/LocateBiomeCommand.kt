/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package kroppeb.server.command.commands

import kroppeb.server.command.Command
import kroppeb.server.command.InvocationError
import kroppeb.server.command.getServer
import kroppeb.server.command.reader.Id
import kroppeb.server.command.reader.ReadFactory
import kroppeb.server.command.reader.Reader
import kroppeb.server.command.reader.ReaderException
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper
import net.minecraft.util.registry.Registry
import net.minecraft.world.biome.Biome

class LocateBiomeCommand(val biomeName: Identifier) :Command{
	override fun execute(source: ServerCommandSource): Int {
		// TODO cache
		val biome = source.minecraftServer.registryManager[Registry.BIOME_KEY].getOrEmpty(biomeName).orElseThrow{InvocationError()}

		val sourcePos = BlockPos(source.position)
		val location = source.world.locateBiome(biome, sourcePos, 6400, 8)
			?: throw InvocationError()
		val dx = location.x-sourcePos.x
		val dy = location.y-sourcePos.y
		return MathHelper.floor(MathHelper.sqrt((dx * dx + dy * dy).toFloat()))
	}

	companion object : ReadFactory<LocateBiomeCommand>{
		override fun Reader.parse(): LocateBiomeCommand {
			val id = Id()
			return LocateBiomeCommand(id)
		}

	}
}
