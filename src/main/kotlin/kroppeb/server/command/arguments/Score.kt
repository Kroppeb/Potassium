/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package kroppeb.server.command.arguments

import kroppeb.server.command.InvocationError
import kroppeb.server.command.arguments.ScoreHolder.SingleScoreHolder
import kroppeb.server.command.getScoreboard
import kroppeb.server.command.reader.ReadFactory
import kroppeb.server.command.reader.Reader
import kroppeb.server.command.reader.Scoreboard
import net.minecraft.scoreboard.ScoreboardPlayerScore
import net.minecraft.server.command.ServerCommandSource

class Score(val scoreHolder: ScoreHolder, val name: String) {


	fun addValue(source: ServerCommandSource, score: Int): Int {
		val objective = source.getScoreboard(name)
		val scoreboard = source.minecraftServer.scoreboard
		val targets = scoreHolder.getEntities(source)
		var i = 0

		for (string in targets) {
			val scoreboardPlayerScore = scoreboard.getPlayerScore(string, objective)
			scoreboardPlayerScore.score = scoreboardPlayerScore.score + score
			i += scoreboardPlayerScore.score
		}

		return i
	}

	fun setValue(source: ServerCommandSource, score: Int): Int {
		val objective = source.getScoreboard(name)
		val scoreboard = source.minecraftServer.scoreboard
		val targets = scoreHolder.getEntities(source)

		for (string in targets) {
			val scoreboardPlayerScore = scoreboard.getPlayerScore(string, objective)
			scoreboardPlayerScore.score = score
		}

		return score * targets.size
	}

	fun resetValue(source: ServerCommandSource): Int {
		val objective = source.getScoreboard(name)
		val scoreboard = source.minecraftServer.scoreboard
		val targets = scoreHolder.getEntities(source)

		for (string in targets) {
			scoreboard.resetPlayerScore(string, objective)
		}
		return targets.size
	}

	companion object : ReadFactory<Score> {
		override fun Reader.parse(): Score {
			val scoreHolder = ScoreHolder()
			val scoreboard = Scoreboard()
			return Score(scoreHolder, scoreboard)
		}
	}
}

class SingleScore(val scoreHolder: SingleScoreHolder, val scoreboard: String) {
	fun getValue(source: ServerCommandSource): Int {
		return source.minecraftServer.scoreboard.getPlayerScore(
			scoreHolder.getEntity(source),
			source.getScoreboard(scoreboard)).score
	}

	companion object : ReadFactory<SingleScore> {
		override fun Reader.parse(): SingleScore {
			val scoreHolder = SingleScoreHolder.invoke()
			val scoreboard = Scoreboard()
			return SingleScore(scoreHolder, scoreboard)
		}
	}
}
