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
import net.minecraft.server.command.AdvancementCommand.Operation
import net.minecraft.server.command.AdvancementCommand.Selection
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier

abstract class AdvancementCommand protected constructor(
		val operation: Operation,
		val targets: PlayerSelector,
		val selection: Selection?,
		val advancement: Identifier?) : Command {

	internal class AdvancementAdvancement(
			operation: Operation, targets: PlayerSelector, selection: Selection?, advancement: Identifier?) :
			AdvancementCommand(operation, targets, selection, advancement) {
		override fun execute(source: ServerCommandSource): Int {
			var i = 0
			val entities = targets.getPlayers(source)
			val advancements = net.minecraft.server.command.AdvancementCommand.select(
					source.minecraftServer.advancementLoader[advancement], selection) // TODO cache
			for (entity in entities!!) {
				i += operation.processAll(entity as ServerPlayerEntity?, advancements)
			}
			if (i == 0) throw InvocationError()
			return i
		}
	}

	internal class AdvancementCriterion(
			operation: Operation, targets: PlayerSelector, advancement: Identifier?, val criterion: Identifier?) :
			AdvancementCommand(operation, targets, Selection.ONLY, advancement) {
		override fun execute(source: ServerCommandSource): Int {
			var i = 0
			val entities = targets.getPlayers(source)
			val adv = source.minecraftServer.advancementLoader[advancement] // TODO cache
			for (entity in entities!!) {
				if (operation.processEachCriterion(entity as ServerPlayerEntity?, adv, criterion.toString())) i++
			}
			if (i == 0) throw InvocationError()
			return i
		}

	}

	companion object : ReadFactory<AdvancementCommand> {
		override fun Reader.parse(): AdvancementCommand {

			val operation: Operation = when (val word = Literal()) {
				"grant" -> Operation.GRANT
				"revoke" -> Operation.REVOKE
				else -> throw ReaderException("expected (grant/revoke), got $word")
			}

			val targets: PlayerSelector = PlayerSelector()

			val selection: Selection = when (val word = Literal()) {
				"everything" -> return AdvancementAdvancement(operation, targets, Selection.EVERYTHING, null)
				"only" -> Selection.ONLY
				"from" -> Selection.FROM
				"through" -> Selection.THROUGH
				"until" -> Selection.UNTIL
				else -> throw ReaderException("expected a mode of selection, got $word")
			}

			val advancement = Id()
			return if (selection == Selection.ONLY && hasNext())
				AdvancementCriterion(operation, targets, advancement, readIdentifier())
			else
				AdvancementAdvancement(operation, targets, selection, advancement)
		}
	}

}
