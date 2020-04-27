/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package kroppeb.server.command.commands;

import kroppeb.server.command.Command;
import kroppeb.server.command.InvocationError;
import kroppeb.server.command.arguments.Score;
import kroppeb.server.command.reader.Reader;
import kroppeb.server.command.reader.ReaderException;
import net.minecraft.scoreboard.ScoreboardPlayerScore;
import net.minecraft.server.command.ServerCommandSource;

import java.util.Collection;

abstract public class ScoreboardCommand implements Command {
	
	public static ScoreboardCommand read(Reader reader) throws ReaderException {
		String sub = reader.readLiteral();
		switch (sub) {
			case "objectives":
				return readObjective(reader);
			case "players":
				return readPlayer(reader);
			default:
				throw new ReaderException("Unexpected scoreboard literal: " + sub);
		}
		
	}
	
	private static ScoreboardCommand readObjective(Reader reader) throws ReaderException {
		throw new ReaderException("Unexpected scoreboard objectives is not implemented");
		/*String sub = reader.readLiteral();
		switch (sub){
			case "add":
			case "players":
			default:
			throw new ReaderException("Unexpected scoreboard objectives literal: " + sub);
		}*/
	}
	
	private static ScoreboardCommand readPlayer(Reader reader) throws ReaderException {
		String sub = reader.readLiteral();
		
		switch (sub) {
			case "list":
				throw new ReaderException("scoreboard players list isn't supported here atm, I'm lazy *cries*"); // TODO
			case "add":
				Score score = Score.read(reader);
				reader.moveNext();
				int value = reader.readInt();
				return new PlayerCommand.Add(score, value);
			case "remove":
				score = Score.read(reader);
				reader.moveNext();
				value = -reader.readInt();
				return new PlayerCommand.Add(score, value);
			case "set":
				score = Score.read(reader);
				reader.moveNext();
				value = reader.readInt();
				return new PlayerCommand.Set(score, value);
			case "reset":
				score = Score.read(reader);
				return new PlayerCommand.Reset(score);
			case "operation":
				return PlayerCommand.Operation.read(reader);
				// TODO enable
			default:
				throw new ReaderException("Unexpected scoreboard players literal: " + sub);
		}
	}
	
	abstract public static class ObjectiveCommand extends ScoreboardCommand {
	}
	
	abstract public static class PlayerCommand extends ScoreboardCommand {
		static public class Add extends PlayerCommand {
			final private Score score;
			final private int value;
			
			public Add(Score score, int value) {
				this.score = score;
				this.value = value;
			}
			
			@Override
			public int execute(ServerCommandSource source) throws InvocationError {
				return score.addValue(source.getWorld(), source.getPosition(), source.getEntity(), value);
			}
		}
		
		static public class Set extends PlayerCommand {
			final private Score score;
			final private int value;
			
			public Set(Score score, int value) {
				this.score = score;
				this.value = value;
			}
			
			@Override
			public int execute(ServerCommandSource source) throws InvocationError {
				return score.setValue(source.getWorld(), source.getPosition(), source.getEntity(), value);
			}
		}
		
		static public class Reset extends PlayerCommand {
			final private Score score;
			
			public Reset(Score score) {
				this.score = score;
			}
			
			@Override
			public int execute(ServerCommandSource source) throws InvocationError {
				return score.resetValue(source.getWorld(), source.getPosition(), source.getEntity());
			}
		}
		
		static public class Operation extends PlayerCommand{
			final Score target;
			final Op op;
			final Score source;
			
			public Operation(Score target, Op op, Score source) {
				this.target = target;
				this.op = op;
				this.source = source;
			}
			
			public static ScoreboardCommand.PlayerCommand.Operation read(Reader reader) throws ReaderException {
				Score target = Score.read(reader);
				reader.moveNext();
				String opString = reader.readUntilWhitespace();
				Op op;
				switch (opString){
					case "+=": op = Op.ADD; break;
					case "-=": op = Op.SUB; break;
					case "*=": op = Op.MUL; break;
					case "/=": op = Op.DIV; break;
					case "%=": op = Op.MOD; break;
					case "=": op = Op.ASS; break;
					case "<": op = Op.MIN; break;
					case ">": op = Op.MAX; break;
					case "><": op = Op.SWP; break;
					default: throw new ReaderException("Unknown scoreboard operation: " + opString);
				}
				reader.moveNext();
				Score source = Score.read(reader);
				
				return new ScoreboardCommand.PlayerCommand.Operation(target, op, source);
			}
			
			@Override
			public int execute(ServerCommandSource source) {
				if(true)
					throw new RuntimeException("This needs fixing"); // TODO
				int r = 0;
				Collection<ScoreboardPlayerScore> targets = this.target.getAll();
				Collection<ScoreboardPlayerScore> sources = this.source.getAll();
				for (ScoreboardPlayerScore t : targets) {
					for (ScoreboardPlayerScore s : sources) {
						this.op.apply(t,s);
						r += t.getScore();
					}
				}
				return r;
			}
			
			enum Op {
				ADD {
					@Override
					public void apply(ScoreboardPlayerScore target, ScoreboardPlayerScore source) {
						target.incrementScore(source.getScore());
					}
				},
				SUB {
					@Override
					public void apply(ScoreboardPlayerScore target, ScoreboardPlayerScore source) {
						target.incrementScore(-source.getScore());
					}
				},
				MUL {
					@Override
					public void apply(ScoreboardPlayerScore target, ScoreboardPlayerScore source) {
						target.setScore(target.getScore() * source.getScore());
					}
				},
				DIV {
					@Override
					public void apply(ScoreboardPlayerScore target, ScoreboardPlayerScore source) {
						target.setScore(target.getScore() / source.getScore());
					}
				},
				MOD {
					@Override
					public void apply(ScoreboardPlayerScore target, ScoreboardPlayerScore source) {
						target.setScore(target.getScore() % source.getScore());
					}
				},
				ASS {
					@Override
					public void apply(ScoreboardPlayerScore target, ScoreboardPlayerScore source) {
						target.setScore(source.getScore());
					}
				},
				MIN {
					@Override
					public void apply(ScoreboardPlayerScore target, ScoreboardPlayerScore source) {
						target.setScore(Math.min(target.getScore(), source.getScore()));
					}
				},
				MAX {
					@Override
					public void apply(ScoreboardPlayerScore target, ScoreboardPlayerScore source) {
						target.setScore(Math.max(target.getScore(), source.getScore()));
					}
				},
				SWP {
					@Override
					public void apply(ScoreboardPlayerScore target, ScoreboardPlayerScore source) {
						int s = source.getScore();
						int t = target.getScore();
						source.setScore(t);
						target.setScore(s);
					}
				};
				
				//abstract void Apply(ScoreboardPlayerScore target, ScoreboardPlayerScore source);
				
				public abstract void apply(ScoreboardPlayerScore target, ScoreboardPlayerScore source);
			}
		}
	}
}
