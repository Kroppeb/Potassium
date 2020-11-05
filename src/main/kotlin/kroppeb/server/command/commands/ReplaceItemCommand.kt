/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package kroppeb.server.command.commands

import com.google.common.collect.Lists
import kroppeb.server.command.Command
import kroppeb.server.command.InvocationError
import kroppeb.server.command.arguments.selector.Selector
import kroppeb.server.command.reader.*
import kroppeb.server.command.toItemStackArgument
import kroppeb.server.command.toLoadedBlockPosition
import net.minecraft.command.arguments.ItemSlotArgumentType
import net.minecraft.command.arguments.ItemStackArgument
import net.minecraft.command.arguments.PosArgument
import net.minecraft.inventory.Inventory
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.TranslatableText


sealed class ReplaceItemCommand
	: Command {
	companion object : ReadFactory<ReplaceItemCommand> {

		override fun Reader.parse(): ReplaceItemCommand =
			when (val sub = Literal()) {
				"block" -> {
					val pos = Pos()
					val slotString = Literal()
					val slot = ItemSlotArgumentType.slotNamesToSlotCommandId[slotString]
						?: throw ReaderException("invalid slot: $slotString")
					val item = ItemPredicate().toItemStackArgument()
					if (canRead())
						Block(pos, slot, item, Int())
					else
						Block(pos, slot, item, 1)
				}
				"entity" -> {
					val targets = Selector()
					val slotString = Literal()
					val slot = ItemSlotArgumentType.slotNamesToSlotCommandId[slotString]
						?: throw ReaderException("invalid slot: $slotString")
					val item = ItemPredicate().toItemStackArgument()
					if (canRead())
						Entity(targets, slot, item, Int())
					else
						Entity(targets, slot, item, 1)
				}
				else -> expected("replace", "block|entity", sub);
			}

	}

	class Block(
		val pos: PosArgument,
		val slot: Int,
		val item: ItemStackArgument,
		val count: Int) : ReplaceItemCommand() {
		override fun execute(source: ServerCommandSource): Int {
			val loadedPos = pos.toLoadedBlockPosition(source) ?: throw InvocationError()
			val stack = item.createStack(count, false);
			val inventory = source.world.getBlockEntity(loadedPos)
			if (inventory !is Inventory) throw InvocationError()

			if (slot < 0 || slot >= inventory.size()) throw InvocationError()

			inventory.setStack(slot, stack)
			return 1
		}
	}

	class Entity(
		val targets: Selector,
		val slot: Int,
		val item: ItemStackArgument,
		val count: Int) : ReplaceItemCommand() {
		override fun execute(source: ServerCommandSource): Int {
			var i = 0

			for (entity in targets.getEntities(source)) {
				if (entity is ServerPlayerEntity) {
					entity.playerScreenHandler.sendContentUpdates()
				}
				if (entity.equip(slot, item.createStack(count, false))) {
					i++
					if (entity is ServerPlayerEntity) {
						entity.playerScreenHandler.sendContentUpdates()
					}
				}
			}

			if (i == 0) throw InvocationError()
			return i;
		}
	}

}


