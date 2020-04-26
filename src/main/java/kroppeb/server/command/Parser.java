/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package kroppeb.server.command;


import kroppeb.server.command.arguments.SetBlockCommand;
import kroppeb.server.command.commands.*;
import kroppeb.server.command.commands.ExecuteCommand;
import kroppeb.server.command.reader.Reader;
import kroppeb.server.command.reader.ReaderException;
import kroppeb.server.command.reader.StringReader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Parser {
	static public Command readFunction(Reader reader) throws ReaderException {
		Command result;
		String command = reader.readLiteral();
		switch (command) {/*
			case "advancement":
				return Advancement.read(reader);*/
			case "data":
				result = DataCommand.read(reader);
				break;
			case "execute":
				result = ExecuteCommand.read(reader);
				break;
			case "function":
				result = FunctionCommand.read(reader);
				break;
			case "kill":
				result = KillCommand.read(reader);
				break;
			case "scoreboard":
				result = ScoreboardCommand.read(reader);
				break;
			case "setblock":
				result = SetBlockCommand.read(reader);
				break;
			case "summon":
				result = SummonCommand.read(reader);
				break;
			case "tag":
				result = TagCommand.read(reader);
				break;
			
			default:
				throw new ReaderException("Unknown command: " + command);
		}
		reader.endLine();
		return result;
	}
	
	static public List<Command> readFile(List<String> file) throws ReaderException {
		List<Command> cmds = new ArrayList<>();
		List<String> errors = new ArrayList<>();
		
		StringReader reader = new StringReader();
		int line = 0;
		for (String i : file) {
			line++;
			reader.setLine(i.trim());
			if (!reader.canRead() || reader.peek() == '#')
				continue;
			try {
				cmds.add(readFunction(reader));
			} catch (ReaderException e) {
				errors.add("line: " + line + ", pos: " + (reader.index + 1) + "\n\t" + e.getMessage());
			}
		}
		if (errors.isEmpty())
			return cmds;
		throw new ReaderException(String.join("\n", errors));
	}
	
	static public List<Command> readFile(Path file) throws IOException, ReaderException {
		return readFile(Files.readAllLines(file, StandardCharsets.UTF_8));
	}
}
