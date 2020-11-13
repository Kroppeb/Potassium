/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package kroppeb.server.command.commands

import com.google.common.collect.Sets
import kroppeb.server.command.Command
import kroppeb.server.command.InvocationError
import kroppeb.server.command.reader.Reader
import kroppeb.server.command.reader.ReaderException
import kroppeb.server.command.arguments.selector.Selector
import kroppeb.server.command.arguments.selector.Selector.Companion.parse
import kroppeb.server.command.reader.Literal
import kroppeb.server.command.reader.ReadFactory
import net.minecraft.server.command.ServerCommandSource

abstract class TagCommand : Command {
	class Add(val targets: Selector, val name: String?) : TagCommand() {
		override fun execute(source: ServerCommandSource): Int {
			var count = 0
			val entities = targets.getEntities(source)
			if (entities!!.isEmpty()) throw InvocationError()
			for (entity in entities) {
				if (entity!!.addScoreboardTag(name)) count++
			}
			return count
		}

	}

	class List(val targets: Selector) : TagCommand() {
		override fun execute(source: ServerCommandSource): Int {
			val entities = targets.getEntities(source)
			if (entities!!.isEmpty()) throw InvocationError()
			val tags: MutableSet<Any> = Sets.newHashSet()
			for (entity in entities) {
				tags.addAll(entity!!.scoreboardTags)
			}
			return tags.size
		}

	}

	class Remove(val targets: Selector, val name: String?) : TagCommand() {
		override fun execute(source: ServerCommandSource): Int {
			val entities = targets.getEntities(source)
			if (entities!!.isEmpty()) throw InvocationError()
			var count = 0
			for (entity in entities) {
				if (entity!!.removeScoreboardTag(name)) count++
			}
			return count
		}

	}

	companion object:ReadFactory<TagCommand> {
		override fun Reader.parse(): TagCommand {
			val targets = Selector()
			return when (val sub = Literal()) {
				"add" -> Add(targets, Literal())
				"list" -> List(targets)
				"remove" -> Remove(targets, Literal())
				else -> throw ReaderException("Unknown tag subcommand: $sub")
			}
		}
	}
}
