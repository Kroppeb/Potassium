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
import kroppeb.server.command.arguments.selector.Selector
import kroppeb.server.command.reader.Int
import kroppeb.server.command.reader.ItemPredicate
import kroppeb.server.command.reader.ReadFactory
import kroppeb.server.command.reader.Reader
import net.minecraft.command.argument.ItemPredicateArgumentType
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.TranslatableText


class ClearCommand(val target: PlayerSelector, val item: ItemPredicateArgumentType.ItemPredicate?, val maxCount: Int)
	: Command {
	companion object : ReadFactory<ClearCommand> {

		override fun Reader.parse(): ClearCommand {
			if (!canRead())
				return ClearCommand(Selector.Self, null, -1)
			val targets = PlayerSelector()

			if (!canRead())
				return ClearCommand(targets, null, -1)
			val item = ItemPredicate()

			if (!canRead())
				return ClearCommand(targets, item, -1)
			val count = Int()

			return ClearCommand(targets, item, count)
		}
	}

	override fun execute(source: ServerCommandSource): Int {
		var i = 0
		val targets = this.target.getPlayers(source)

		for(serverPlayerEntity in targets){
			i += serverPlayerEntity.inventory.remove(
				item,
				maxCount,
				serverPlayerEntity.playerScreenHandler.method_29281())
			serverPlayerEntity.currentScreenHandler.sendContentUpdates()
			serverPlayerEntity.playerScreenHandler.onContentChanged(serverPlayerEntity.inventory)
			serverPlayerEntity.updateCursorStack()
		}

		if (i == 0) throw InvocationError()
		return i
	}

}
