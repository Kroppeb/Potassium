/*
 * Copyright (c) 2021 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package kroppeb.server.command.commands

import kroppeb.server.command.Command
import kroppeb.server.command.InvocationError
import kroppeb.server.command.arguments.selector.PlayerSelector
import kroppeb.server.command.arguments.selector.SinglePlayerSelector
import kroppeb.server.command.reader.*
import net.minecraft.server.command.ExperienceCommand.Component
import net.minecraft.server.command.ServerCommandSource


sealed class ExperienceCommand : Command {
	abstract val target: PlayerSelector

	companion object : ReadFactory<ExperienceCommand> {

		override fun Reader.parse(): ExperienceCommand {
			when (val sub = Literal()) {
				"add" -> {
					val targets = PlayerSelector()
					val amount = Int()
					if (!canRead())
						return Modify.Add(targets, amount, Component.POINTS)
					return when (val comp = Literal()) {
						"points" -> Modify.Add(targets, amount, Component.POINTS)
						"levels" -> Modify.Add(targets, amount, Component.LEVELS)
						else -> expected("experience <targets> <amount>", "(points|levels)", comp)
					}
				}

				"set" -> {
					val targets = PlayerSelector()
					val amount = Int()
					if (!canRead())
						return Modify.Add(targets, amount, Component.POINTS)
					return when (val comp = Literal()) {
						"points" -> Modify.Set(targets, amount, Component.POINTS)
						"levels" -> Modify.Set(targets, amount, Component.LEVELS)
						else -> expected("experience <targets> <amount>", "(points|levels)", comp)
					}
				}

				"query" -> {
					val targets = SinglePlayerSelector()
					return when (val comp = Literal()) {
						"points" -> Query(targets, Component.POINTS)
						"levels" -> Query(targets, Component.LEVELS)
						else -> expected("experience <targets> <amount>", "(points|levels)", comp)
					}
				}
				else -> expected("experience", "(add|set|query)", sub)
			}
		}
	}

	sealed class Modify(override val target: PlayerSelector, val amount: Int, val component: Component) :
		ExperienceCommand() {


		class Add(target: PlayerSelector, amount: Int, component: Component) : Modify(target, amount, component) {
			override fun execute(source: ServerCommandSource): Int {
				val targets = target.getPlayers(source)
				for (serverPlayerEntity in targets) {
					component.adder.accept(serverPlayerEntity, amount)
				}
				return targets.size
			}
		}

		class Set(target: PlayerSelector, amount: Int, component: Component) : Modify(target, amount, component) {
			override fun execute(source: ServerCommandSource): Int {
				val targets = target.getPlayers(source)
				val i = targets.count { component.setter.test(it, amount) }

				if (i == 0) {
					throw InvocationError()
				}

				return targets.size
			}
		}
	}

	class Query(override val target: SinglePlayerSelector, val component: Component) : ExperienceCommand() {
		override fun execute(source: ServerCommandSource): Int {
			return component.getter.applyAsInt(target.getPlayer(source) ?: throw InvocationError())
		}
	}

}
