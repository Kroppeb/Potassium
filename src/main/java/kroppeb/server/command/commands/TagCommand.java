/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package kroppeb.server.command.commands;

import com.google.common.collect.Sets;
import kroppeb.server.command.Command;
import kroppeb.server.command.arguments.Selector;
import kroppeb.server.command.reader.Reader;
import kroppeb.server.command.reader.ReaderException;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.ServerCommandSource;

import java.util.HashSet;
import java.util.Set;

abstract public class TagCommand implements Command {
	static public TagCommand read(Reader reader) throws ReaderException {
		Selector targets = Selector.read(reader);
		reader.moveNext();
		String sub = reader.readLiteral();
		switch(sub){
			case "add": return new Add(targets, reader.readUnquotedString());
			case "list": return new List(targets);
			case "remove": return new Remove(targets, reader.readUnquotedString());
			default: throw new ReaderException("Unknown tag subcommand: " + sub);
		}
	}
	
	static public class Add extends TagCommand{
		final Selector targets;
		final String name;
		
		public Add(Selector targets, String name) {
			this.targets = targets;
			this.name = name;
		}
		
		@Override
		public int execute(ServerCommandSource source) {
			int count = 0;
			for (Entity entity : targets.getEntities(source)) {
				if(entity.addScoreboardTag(name))
					count++;
			}
			return count; // TODO throw if 0?
		}
	}
	
	static public class List extends TagCommand{
		final Selector targets;
		
		public List(Selector targets) {
			this.targets = targets;
		}
		
		@Override
		public int execute(ServerCommandSource source) {
			Set<Object> tags = Sets.newHashSet();
			for (Entity entity : targets.getEntities(source)) {
				tags.addAll(entity.getScoreboardTags());
			}
			return tags.size();
		}
	}
	
	static public class Remove extends TagCommand{
		final Selector targets;
		final String name;
		
		public Remove(Selector targets, String name) {
			this.targets = targets;
			this.name = name;
		}
		
		@Override
		public int execute(ServerCommandSource source) {
			int count = 0;
			for (Entity entity : targets.getEntities(source)) {
				if(entity.removeScoreboardTag(name))
					count++;
			}
			return count; // TODO throw if 0?
		}
	}
}
