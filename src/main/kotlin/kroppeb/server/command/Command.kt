/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package kroppeb.server.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import net.minecraft.server.command.ServerCommandSource

@FunctionalInterface
interface Command : Command<ServerCommandSource> {
	override fun run(context: CommandContext<ServerCommandSource>): Int {
		return try {
			execute(context.source)
		} catch (invocationError: InvocationError) {
			0
		}
	}

	fun execute(source: ServerCommandSource): Int
	fun executeVoid(source: ServerCommandSource) {
		try {
			execute(source)
		} catch (ignored: InvocationError) {
		}
	}
}
