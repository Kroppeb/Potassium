/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package kroppeb.server.command;

import kroppeb.server.command.commands.FunctionCommand;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.tag.Tag;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandLoader {
	public static Command[] commands;
	public static Map<String, Command> functions = new HashMap<>();
	private static List<FunctionCommand> queued = new ArrayList<>();
	
	public static void reset(){
		functions.clear();
		queued.clear();
	}
	
	public static void loadAll() {
		for (FunctionCommand command : queued) {
			command.build();
		}
		queued.clear();
	}
	
	public static void queue(FunctionCommand functionCommand) {
		queued.add(functionCommand);
	}
	
	public static Tag<Block> getBlockTag(Identifier identifier) {
		return null;
	}
	
	public static Tag<Item> getItemTag(Identifier identifier) {
		return null;
	}
}
