/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package kroppeb.server.command.arguments

import kroppeb.server.command.arguments.selector.Selector
import kroppeb.server.command.arguments.selector.Selector.SingleSelector
import kroppeb.server.command.reader.ReadFactory
import kroppeb.server.command.reader.Reader
import kroppeb.server.command.reader.ReaderException
import net.minecraft.server.command.ServerCommandSource

sealed class ScoreHolder {
	abstract class SingleScoreHolder : ScoreHolder() {
		fun getEntity(source: ServerCommandSource?): net.minecraft.entity.Entity {
			throw RuntimeException() // TODO fix & implement SingleScoreHolder;
		}
	}

	class Named(val name: String?) : SingleScoreHolder()

	class Entity(val selector: SingleSelector) : SingleScoreHolder()

	class Entities(val selector: Selector) : ScoreHolder()

	object All : ScoreHolder()
	companion object: ReadFactory<ScoreHolder> {
		@Throws(ReaderException::class)
		override fun Reader.parse(): ScoreHolder {
			return if (peek() == '@') {
				val selector = Selector()
				if (selector is SingleSelector) {
					Entity(selector)
				} else {
					Entities(selector)
				}
			} else {
				Named(readUntilWhitespace())
			}
		}
	}
}