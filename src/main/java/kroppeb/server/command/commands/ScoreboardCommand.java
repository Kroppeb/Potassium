/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package kroppeb.server.command.commands;

import kroppeb.server.command.Command;
import kroppeb.server.command.arguments.Score;
import kroppeb.server.command.arguments.ScoreHolder;
import kroppeb.server.command.reader.Reader;
import kroppeb.server.command.reader.ReaderException;
import net.minecraft.server.command.ServerCommandSource;

abstract public class ScoreboardCommand implements Command {
	
	public static ScoreboardCommand read(Reader reader) throws ReaderException {
		String sub = reader.readLiteral();
		switch (sub){
			case "objectives": return readObjective(reader);
			case "players": return readPlayer(reader);
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
		
		switch (sub){
			case "list": throw new ReaderException("scoreboard players list isn't supported here atm, I'm lazy *cries*"); // TODO
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
			default:
			throw new ReaderException("Unexpected scoreboard players literal: " + sub);
		}
	}
	
	abstract public static class ObjectiveCommand extends ScoreboardCommand{}
	abstract public static class PlayerCommand extends ScoreboardCommand{
		static public class Add extends PlayerCommand {
			final private Score score;
			final private int value;
			
			public Add(Score score, int value) {
				this.score = score;
				this.value = value;
			}
			
			@Override
			public int execute(ServerCommandSource source) {
				score.addValue(source.getWorld(),source.getPosition(), source.getEntity(), value);
				return 1; // TODO entities affected
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
			public int execute(ServerCommandSource source) {
				score.setValue(source.getWorld(),source.getPosition(), source.getEntity(), value);
				return 1; // TODO entities affected
			}
		}
	}
}
