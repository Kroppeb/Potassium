/*
 * Copyright (c) 2021 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package kroppeb.server.command.commands

import kroppeb.server.command.Command
import kroppeb.server.command.arguments.selector.PlayerSelector
import kroppeb.server.command.arguments.selector.Selector
import kroppeb.server.command.reader.*
import net.minecraft.network.packet.s2c.play.TitleS2CPacket
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.minecraft.text.Texts
import net.minecraft.util.registry.Registry


sealed class TitleCommand(val selector: PlayerSelector) : Command {

	companion object : ReadFactory<TitleCommand> {

		override fun Reader.parse(): TitleCommand {
			val targets = PlayerSelector()
			return when (val sub = Literal()) {
				"clear" -> Clear(targets)
				"reset" -> Reset(targets)
				"title" -> Title(targets, Text(), TitleS2CPacket.Action.TITLE)
				"subtitle" -> Title(targets, Text(), TitleS2CPacket.Action.SUBTITLE)
				"actionbar" -> Title(targets, Text(), TitleS2CPacket.Action.ACTIONBAR)
				"times" -> Times(targets, Int(), Int(), Int())
				else -> expected("title", "(clear|reset|title|subtitle|actionbar|times)", sub)
			}
		}
	}

	class Clear(selector: PlayerSelector) : TitleCommand(selector) {
		override fun execute(source: ServerCommandSource): Int {
			val targets = selector.getPlayers(source)

			val titleS2CPacket = TitleS2CPacket(TitleS2CPacket.Action.CLEAR, null)

			for (serverPlayerEntity in targets) {
				serverPlayerEntity.networkHandler.sendPacket(titleS2CPacket)
			}

			return targets.size
		}
	}

	class Reset(selector: PlayerSelector) : TitleCommand(selector) {
		override fun execute(source: ServerCommandSource): Int {
			val targets = selector.getPlayers(source)

			val titleS2CPacket = TitleS2CPacket(TitleS2CPacket.Action.RESET, null)

			for (serverPlayerEntity in targets) {
				serverPlayerEntity.networkHandler.sendPacket(titleS2CPacket)
			}

			return targets.size
		}

	}

	class Title(selector: PlayerSelector, val title: Text, val type: TitleS2CPacket.Action) : TitleCommand(selector) {
		override fun execute(source: ServerCommandSource): Int {
			val targets = selector.getPlayers(source)


			for (serverPlayerEntity in targets) {
				serverPlayerEntity.networkHandler.sendPacket(TitleS2CPacket(type, Texts.parse(source, title, serverPlayerEntity, 0)));
			}

			return targets.size
		}

	}

	class Times(selector: PlayerSelector, val fadeIn:Int, val stay:Int, val fadeOut:Int) : TitleCommand(selector) {
		override fun execute(source: ServerCommandSource): Int {
			val targets = selector.getPlayers(source)

			val titleS2CPacket = TitleS2CPacket(fadeIn, stay, fadeOut);

			for (serverPlayerEntity in targets) {
				serverPlayerEntity.networkHandler.sendPacket(titleS2CPacket)
			}

			return targets.size
		}

	}

}
