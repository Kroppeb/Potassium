/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package kroppeb.server.command.commands

import kroppeb.server.command.Command
import kroppeb.server.command.InvocationError
import kroppeb.server.command.arguments.selector.PlayerSelector
import kroppeb.server.command.reader.*
import net.minecraft.command.arguments.DefaultPosArgument
import net.minecraft.command.arguments.PosArgument
import net.minecraft.network.packet.s2c.play.PlaySoundIdS2CPacket
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.sound.SoundCategory
import net.minecraft.util.Identifier
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import kotlin.math.pow


class PlaySoundCommand(
	val sound: Identifier,
	val category: SoundCategory,
	val targets: PlayerSelector,
	val pos: PosArgument,
	val volume: Float,
	val pitch: Float,
	val minVolume: Float
) : Command {
	companion object : ReadFactory<PlaySoundCommand> {

		val categories = SoundCategory.values().associateBy { it.getName() }

		override fun Reader.parse(): PlaySoundCommand {
			val sound = Id()
			val categoryString = Literal()
			val category = categories[categoryString]
				?: throw ReaderException("$categoryString isn't a sound category")
			val targets = PlayerSelector()
			if (!canRead())
				return PlaySoundCommand(sound, category, targets, DefaultPosArgument.zero(), 1.0f, 1.0f, 0.0f)
			val pos = Pos()
			if (!canRead())
				return PlaySoundCommand(sound, category, targets, pos, 1.0f, 1.0f, 0.0f)
			val volume = Float()
			if (!canRead())
				return PlaySoundCommand(sound, category, targets, pos, volume, 1.0f, 0.0f)
			val pitch = Float()
			if (!canRead())
				return PlaySoundCommand(sound, category, targets, pos, volume, pitch, 0.0f)
			val minVolume = Float()
			return PlaySoundCommand(sound, category, targets, pos, volume, pitch, minVolume)
		}
	}

	override fun execute(source: ServerCommandSource): Int {
		val d = (volume.coerceAtLeast(1.0f) * 16.0f).toDouble().pow(2.0)

		var i = 0
		val targets = targets.getPlayers(source)
		val pos = pos.toAbsolutePos(source)


		for (serverPlayerEntity in targets) {
			val dx: Double = pos.x - serverPlayerEntity.x
			val dy: Double = pos.y - serverPlayerEntity.y
			val dz: Double = pos.z - serverPlayerEntity.z
			val dist = dx * dx + dy * dy + dz * dz
			if (dist <= d) {
				serverPlayerEntity.networkHandler.sendPacket(PlaySoundIdS2CPacket(sound, category, pos, volume, pitch))
			}else {
				if (minVolume > 0.0f) {
					val k = MathHelper.sqrt(dist).toDouble()
					serverPlayerEntity.networkHandler.sendPacket(PlaySoundIdS2CPacket(sound, category, Vec3d(
						serverPlayerEntity.x + dx / k * 2.0,
						serverPlayerEntity.y + dy / k * 2.0,
						serverPlayerEntity.z + dz / k * 2.0), minVolume, pitch))
				} else {
					continue
				}
			}
			++i
		}

		if (i == 0) throw InvocationError()
		return i
	}

}
