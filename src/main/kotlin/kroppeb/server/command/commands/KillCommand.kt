/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package kroppeb.server.command.commands

import kroppeb.server.command.Command
import kroppeb.server.command.InvocationError
import kroppeb.server.command.arguments.selector.Selector
import kroppeb.server.command.reader.ReadFactory
import kroppeb.server.command.reader.Reader
import kroppeb.server.command.reader.ReaderException
import net.minecraft.server.command.ServerCommandSource

class KillCommand(val targets: Selector) : Command {
	@Throws(InvocationError::class)
	override fun execute(source: ServerCommandSource): Int {
		val entities = targets.getEntities(source)
		for (entity in entities!!) {
			entity!!.kill()
		}
		val size = entities.size
		if (size == 0) throw InvocationError()
		return size
	}

	companion object : ReadFactory<KillCommand> {
		@Throws(ReaderException::class)
		override fun Reader.parse(): KillCommand {
			return KillCommand(Selector())
		}
	}

}