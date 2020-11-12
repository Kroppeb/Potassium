/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package kroppeb.server.command.arguments

import kroppeb.server.command.reader.ReadFactory
import kroppeb.server.command.reader.Reader
import kroppeb.server.command.reader.ReaderException
import net.minecraft.entity.Entity
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.Vec3d

abstract class ScoreComparator {
	abstract fun compareTo(other: SingleScore, source:ServerCommandSource): Boolean
	class ScoreScore(val score: SingleScore, val comparator: IntComparator) : ScoreComparator() {
		override fun compareTo(other: SingleScore, source:ServerCommandSource): Boolean {
			return comparator.compare(
					other.getValue(source),
					score.getValue(source)
			)
		}

	}

	class ScoreMatches(val min: Int, val max: Int) : ScoreComparator() {
		override fun compareTo(other: SingleScore, source:ServerCommandSource): Boolean {
			val value = other.getValue(source)
			return value in min..max
		}

	}

	companion object : ReadFactory<ScoreComparator>{
		override fun Reader.parse(): ScoreComparator {
			return if (tryReadLiteral("matches")) {
				if (tryRead('.')) {
					readChar('.')
					ScoreMatches(Int.MIN_VALUE, readInt())
				} else {
					val min = readInt()
					if (tryRead('.')) {
						readChar('.')
						if (!canRead() || isWhiteSpace) ScoreMatches(min, Int.MAX_VALUE) else ScoreMatches(min, readInt())
					} else ScoreMatches(min, min)
				}
			} else {
				val comparator: IntComparator = IntComparator.read(this)
				moveNext()
				val score = SingleScore.read(this)
				ScoreScore(score, comparator)
			}
		}
	}
}
