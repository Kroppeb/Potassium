/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package kroppeb.server.command

import kroppeb.server.command.commands.FunctionCommand
import net.minecraft.block.Block
import net.minecraft.item.Item
import net.minecraft.tag.Tag
import net.minecraft.util.Identifier
import java.util.*

object CommandLoader {
	@JvmField
	var commands: Array<Command> = Array(0){error("nope")}
	@JvmField
	var functions: MutableMap<String?, Command?> = HashMap()
	private val queued: MutableList<FunctionCommand> = ArrayList()
	fun reset() {
		functions.clear()
		queued.clear()
	}

	fun loadAll() {
		for (command in queued) {
			command.build()
		}
		queued.clear()
	}

	fun queue(functionCommand: FunctionCommand) {
		queued.add(functionCommand)
	}

	fun getBlockTag(identifier: Identifier?): Tag<Block>? {
		return null
	}

	fun getItemTag(identifier: Identifier?): Tag<Item>? {
		return null
	}
}