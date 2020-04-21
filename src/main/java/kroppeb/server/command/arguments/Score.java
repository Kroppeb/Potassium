/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package kroppeb.server.command.arguments;

import kroppeb.server.command.reader.Reader;
import kroppeb.server.command.reader.ReaderException;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

public class Score {
	final ScoreHolder scoreHolder;
	final String scoreboard;
	
	Score(ScoreHolder scoreHolder, String scoreboard) {
		this.scoreHolder = scoreHolder;
		this.scoreboard = scoreboard;
	}
	
	public static Score read(Reader reader) throws ReaderException {
		ScoreHolder scoreHolder = ScoreHolder.read(reader);
		reader.moveNext();
		String scoreboard = reader.readUnquotedString();
		if(scoreboard.length() > 16)
			throw new ReaderException("scoreboard name too long"); // TODO move this to argument parser
		return new Score(scoreHolder, scoreboard);
	}
	
	public int getValue(ServerWorld world, Vec3d pos, Entity entity) {
		return 0;
		// TODO
		// world.getScoreboard()
	}
}
