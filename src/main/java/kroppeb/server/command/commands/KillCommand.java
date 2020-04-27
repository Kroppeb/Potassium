/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package kroppeb.server.command.commands;

import kroppeb.server.command.Command;
import kroppeb.server.command.InvocationError;
import kroppeb.server.command.arguments.Selector;
import kroppeb.server.command.reader.Reader;
import kroppeb.server.command.reader.ReaderException;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.ServerCommandSource;

import java.util.Collection;

public class KillCommand implements Command {
	final Selector targets;
	
	public KillCommand(Selector targets) {
		this.targets = targets;
	}
	
	public static KillCommand read(Reader reader) throws ReaderException {
		return new KillCommand(Selector.read(reader));
	}
	
	@Override
	public int execute(ServerCommandSource source) throws InvocationError {
		Collection<? extends Entity> entities = targets.getEntities(source);
		for (Entity entity : entities) {
			entity.kill();
		}
		int size = entities.size();
		if(size == 0)
			throw new InvocationError();
		return size;
	}
}
