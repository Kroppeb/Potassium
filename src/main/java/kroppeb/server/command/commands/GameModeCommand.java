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
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.GameMode;

import java.util.Collection;

abstract public class GameModeCommand implements Command {
	final Selector targets;
	final GameMode gm;

	public GameModeCommand(Selector targets, GameMode gm) {
		this.targets = targets;
		this.gm = gm;
	}

	public static GameModeCommand read(Reader reader) throws ReaderException {
		Selector targets = null;
		GameMode gm = GameMode.byName(reader.readLiteral()); // TODO: handle custom gamemodes
		if (reader.hasNext()) {
			targets = Selector.read(reader);
		}
		return new GameModeCommand(targets, gm);
	}

	public int execute(ServerCommandSource source) {
		int e = 0;
		Collection<Entity> entities;
		if (targets == null) {
			entities = Collections.<Entity>singleton(source.getPlayer());
		} else {
			entities = targets.getEntities(source);
		}
		for (Entity entity : entities) {
			if (entity instanceof PlayerEntity) {
				((PlayerEntity) entity).setGameMode(gm);
				e++;
			}
		}
		if (e == 0) {
			// TODO: throw error on failed command
		}
		return e;
	}
}
