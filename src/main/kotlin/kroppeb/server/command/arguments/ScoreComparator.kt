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
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.Vec3d

abstract class ScoreComparator {
	abstract fun compareTo(other: Score, world: ServerWorld?, pos: Vec3d?, entity: Entity?): Boolean
	class ScoreScore(val score: Score, val comparator: IntComparator) : ScoreComparator() {
		override fun compareTo(other: Score, world: ServerWorld?, pos: Vec3d?, entity: Entity?): Boolean {
			return comparator.compare(
					other.getValue(world, pos, entity),
					score.getValue(world, pos, entity)
			)
		}

	}

	class ScoreMatches(val min: Int, val max: Int) : ScoreComparator() {
		override fun compareTo(other: Score, world: ServerWorld?, pos: Vec3d?, entity: Entity?): Boolean {
			val value = other.getValue(world, pos, entity)
			return value in min..max
		}

	}

	companion object : ReadFactory<ScoreComparator>{
		@Throws(ReaderException::class)
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
				val score: Score = Score.read(this)
				ScoreScore(score, comparator)
			}
		}
	}
}