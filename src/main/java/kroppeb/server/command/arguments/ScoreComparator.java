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

public abstract class ScoreComparator {
	abstract public boolean compareTo(Score other, ServerWorld world, Vec3d pos, Entity entity);
	
	static public class ScoreScore extends ScoreComparator{
		final Score score;
		final IntComparator comparator;
		
		public ScoreScore(Score score, IntComparator comparator) {
			this.score = score;
			this.comparator = comparator;
		}
		
		@Override
		public boolean compareTo(Score other, ServerWorld world, Vec3d pos, Entity entity) {
			return comparator.compare(
					other.getValue(world, pos, entity),
					score.getValue(world, pos, entity)
			);
		}
	}
	
	static public class ScoreMatches extends ScoreComparator{
		final int min;
		final int max;
		
		public ScoreMatches(int min, int max) {
			this.min = min;
			this.max = max;
		}
		
		@Override
		public boolean compareTo(Score other, ServerWorld world, Vec3d pos, Entity entity) {
			int value = other.getValue(world, pos, entity);
			return min <= value && value <= max;
		}
	}
	
	public static ScoreComparator read(Reader reader) throws ReaderException {
		if(reader.tryReadLiteral("matches")){
			if(reader.tryRead('.')){
				reader.readChar('.');
				return new ScoreMatches(Integer.MIN_VALUE, reader.readInt());
			}else{
				int min = reader.readInt();
				if(reader.tryRead('.')){
					reader.readChar('.');
					if(!reader.canRead() || reader.isWhiteSpace())
						return new ScoreMatches(min, Integer.MAX_VALUE);
					else
						return new ScoreMatches(min, reader.readInt());
				}else
					return new ScoreMatches(min, min);
			}
		}else{
			IntComparator comparator = IntComparator.read(reader);
			reader.moveNext();
			Score score = Score.read(reader);
			return new ScoreScore(score, comparator);
		}
	}
}
