/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package kroppeb.server.command;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;

public interface Command extends com.mojang.brigadier.Command<ServerCommandSource> {
	
	@Override
	default int run(CommandContext<ServerCommandSource> context){
		return execute(context.getSource());
	}
	
	int execute(ServerCommandSource source);
}
