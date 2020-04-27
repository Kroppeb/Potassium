/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package kroppeb.server.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;

@FunctionalInterface
public interface Command extends com.mojang.brigadier.Command<ServerCommandSource> {
	
	@Override
	default int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		try {
			return execute(context.getSource());
		} catch (InvocationError invocationError) {
			return 0;
		}
	}
	
	int execute(ServerCommandSource source) throws InvocationError;
	default void executeVoid(ServerCommandSource source){
		try {
			execute(source);
		} catch (InvocationError ignored) {
		}
	}
}
