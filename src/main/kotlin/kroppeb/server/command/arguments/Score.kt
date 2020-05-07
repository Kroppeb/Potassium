/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package kroppeb.server.command.arguments

import kroppeb.server.command.InvocationError
import kroppeb.server.command.reader.ReadFactory
import kroppeb.server.command.reader.Reader
import kroppeb.server.command.reader.ReaderException
import net.minecraft.entity.Entity
import net.minecraft.scoreboard.ScoreboardPlayerScore
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.Vec3d

class Score internal constructor(val scoreHolder: ScoreHolder, val scoreboard: String?) {
	fun getValue(world: ServerWorld?, pos: Vec3d?, entity: Entity?): Int {
		return 0
		// TODO
		// world.getScoreboard()
	}

	@Throws(InvocationError::class)
	fun addValue(world: ServerWorld?, position: Vec3d?, entity: Entity?, value: Int): Int {
		// TODO implement Score::addValue; should return the sum of the results, or error if no entities were found.
		throw InvocationError()
	}

	@Throws(InvocationError::class)
	fun setValue(world: ServerWorld?, position: Vec3d?, entity: Entity?, value: Int): Int {
		// TODO implement Score::setValue; should return sum of results, or error if no entities.
		throw InvocationError() // value * #entities.
	}

	@Throws(InvocationError::class)
	fun resetValue(world: ServerWorld?, position: Vec3d?, entity: Entity?): Int {
		// TODO implement Score::resetValue; should return entity count, or error if no entities.
		throw InvocationError()
	}

	// TODO
	val all: Collection<ScoreboardPlayerScore>
		get() = emptyList() // TODO

	companion object : ReadFactory<Score>{
		@Throws(ReaderException::class)
		override fun Reader.parse(): Score {
			val scoreHolder = ScoreHolder()
			val scoreboard = readUnquotedString()
			if (scoreboard.length > 16) throw ReaderException("scoreboard name too long") // TODO move this to argument parser
			return Score(scoreHolder, scoreboard)
		}
	}

}