/*
 * Copyright (c) 2020 MadHau5
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package kroppeb.server.command.commands;

import kroppeb.server.command.Command;
import kroppeb.server.command.arguments.Selector;
import kroppeb.server.command.reader.Reader;
import kroppeb.server.command.reader.ReaderException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.Collection;

abstract public class GiveCommand implements Command {
	final Selector targets;
	final Identifier item;
	final CompoundTag nbt;
	final int count;

	public GiveCommand(Selector targets, Identifier item, CompoundTag nbt, int count) {
		this.targets = targets;
		this.item = item;
		this.nbt = nbt;
		this.count = count;
	}

	public static GiveCommand read(Reader reader) throws ReaderException {
		Selector targets = Selector.read(reader);
		Identifier item = Registry.ITEM.get(reader.readIdentifier());
		CompoundTag nbt;
		int count = 1;
		boolean hideParticles = false;
		if (reader.hasNext()) {
			// nbt = ???;
			// TODO: Process NBT tags
		}
		if (reader.hasNext()) {
			reader.moveNext();
			count = reader.readInt();
		}
		return new GiveCommand(targets, item, nbt, count);
	}

	public int execute(ServerCommandSource source) {
		int e = 0;
		
		ItemStack stack = ItemStack(item, stackSize);
		// TODO: throw error for 'illegal' item (not found / more than max count)
		
		Collection<Entity> entities = targets.getEntities(source);
		for (Entity entity : entities) {
			if (entity instanceof PlayerEntity) {
				player = (PlayerEntity) entity;
				player.inventory.insertStack(stack);
				// TODO: drop item on full player inventory
				e++;
			}
		}
		if (e == 0) {
			// TODO: throw error on failed command
		}
		return e;
	}
}
