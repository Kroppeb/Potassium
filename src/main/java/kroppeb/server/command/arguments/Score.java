/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package kroppeb.server.command.arguments;

import kroppeb.server.command.InvocationError;
import kroppeb.server.command.reader.Reader;
import kroppeb.server.command.reader.ReaderException;
import net.minecraft.entity.Entity;
import net.minecraft.scoreboard.ScoreboardPlayerScore;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.Collection;
import java.util.Collections;

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
	
	public int addValue(ServerWorld world, Vec3d position, Entity entity, int value) throws InvocationError {
		// TODO implement Score::addValue; should return the sum of the results, or error if no entities were found.
		throw new InvocationError();
	}
	
	public int setValue(ServerWorld world, Vec3d position, Entity entity, int value) throws InvocationError {
		// TODO implement Score::setValue; should return sum of results, or error if no entities.
		throw new InvocationError(); // value * #entities.
	}
	
	public int resetValue(ServerWorld world, Vec3d position, Entity entity) throws InvocationError {
		// TODO implement Score::resetValue; should return entity count, or error if no entities.
		throw new InvocationError();
	}
	
	public Collection<ScoreboardPlayerScore> getAll() {
		return Collections.emptyList(); // TODO
	}
}
