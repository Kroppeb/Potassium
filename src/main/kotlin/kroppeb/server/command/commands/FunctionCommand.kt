/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package kroppeb.server.command.commands

import kroppeb.server.command.Command
import kroppeb.server.command.CommandLoader
import kroppeb.server.command.InvocationError
import kroppeb.server.command.arguments.Resource
import kroppeb.server.command.reader.ReadFactory
import kroppeb.server.command.reader.Reader
import kroppeb.server.command.reader.ReaderException
import net.minecraft.server.command.ServerCommandSource

class FunctionCommand(val resource: Resource) : Command {
	lateinit var function: Command

	override fun execute(source: ServerCommandSource): Int {
		return function.execute(source)
	}

	fun build() {
		function = CommandLoader.functions[resource.toString()]?:throw RuntimeException("Missing function: $resource")
	}

	companion object:ReadFactory<FunctionCommand> {
		override fun Reader.parse(): FunctionCommand {
			return FunctionCommand(Resource())
		}
	}

	init {
		CommandLoader.queue(this)
	}
}
