/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package kroppeb.server.command.arguments

import kroppeb.server.command.InvocationError
import kroppeb.server.command.arguments.selector.Selector
import kroppeb.server.command.arguments.selector.Selector.SingleSelector
import kroppeb.server.command.reader.Literal
import kroppeb.server.command.reader.ReadFactory
import kroppeb.server.command.reader.Reader
import kroppeb.server.command.reader.ReaderException
import net.minecraft.server.command.ServerCommandSource

sealed class ScoreHolder {
	abstract fun getEntities(source: ServerCommandSource): Collection<String>

	abstract class SingleScoreHolder : ScoreHolder() {
		abstract fun getEntity(source: ServerCommandSource): String

		companion object : ReadFactory<SingleScoreHolder> {
			override fun Reader.parse(): SingleScoreHolder {
				return if (peek() == '@') {
					val selector = SingleSelector()
					Entity(selector)
				} else {
					val name = Literal()
					if (name == "*")
						throw ReaderException("UNSUPPORTED: this is expecting a single entity, I'm not supporting `*` here")
					else
						Named(name)
				}
			}
		}
	}

	class Named(val name: String) : SingleScoreHolder() {
		override fun getEntity(source: ServerCommandSource): String = name
		override fun getEntities(source: ServerCommandSource): Collection<String> = listOf(name)
	}

	class Entity(val selector: SingleSelector) : SingleScoreHolder() {
		override fun getEntity(source: ServerCommandSource): String = selector.getEntity(source)?.entityName
			?: throw InvocationError()

		override fun getEntities(source: ServerCommandSource): Collection<String> =
			selector.getEntity(source)?.let {
				listOf(it.entityName)
			} ?: throw InvocationError()
	}

	class Entities(val selector: Selector) : ScoreHolder() {
		override fun getEntities(source: ServerCommandSource): Collection<String> {
			val entities = selector.getEntities(source)
			if (entities.isEmpty())
				throw InvocationError()
			return entities.map { it.entityName }
		}
	}

	object All : ScoreHolder() {
		override fun getEntities(source: ServerCommandSource): Collection<String> {
			return source.minecraftServer.scoreboard.knownPlayers
		}
	}

	companion object : ReadFactory<ScoreHolder> {
		override fun Reader.parse(): ScoreHolder {
			return if (peek() == '@') {
				val selector = Selector()
				if (selector is SingleSelector) {
					Entity(selector)
				} else {
					Entities(selector)
				}
			} else {
				val name = Literal()
				if (name == "*")
					All
				else
					Named(name)
			}
		}
	}
}
