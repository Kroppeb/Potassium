/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package kroppeb.server.command.commands;

import kroppeb.server.command.Command;
import kroppeb.server.command.CommandLoader;
import kroppeb.server.command.InvocationError;
import kroppeb.server.command.arguments.Resource;
import kroppeb.server.command.reader.Reader;
import kroppeb.server.command.reader.ReaderException;
import net.minecraft.server.command.ServerCommandSource;

public class FunctionCommand implements Command {
	Command function;
	final Resource resource;
	
	public FunctionCommand(Resource resource) {
		this.resource = resource;
		CommandLoader.queue(this);
	}
	
	
	@Override
	public int execute(ServerCommandSource source) throws InvocationError {
		return function.execute(source);
	}
	
	public void build(){
		function = CommandLoader.functions.get(resource.toString());
	}
	
	public static FunctionCommand read(Reader reader) throws ReaderException {
		return new FunctionCommand(Resource.read(reader));
	}
}
