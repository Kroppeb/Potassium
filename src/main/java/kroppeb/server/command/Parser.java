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
		String command = reader.readLiteral();
		switch (command) {/*
			case "advancement":
				return Advancement.read(reader);*/
			case "execute":
				return ExecuteCommand.read(reader);
			case "function":
				return FunctionCommand.read(reader);
			case "scoreboard":
				return ScoreboardCommand.read(reader);
			case "setblock":
				return SetBlockCommand.read(reader);
			case "summon":
				return SummonCommand.read(reader);
			
			default:
				throw new ReaderException("Unknown command: " + command);
		}
	}
	
	static public List<Command> readFile(List<String> file) throws ReaderException {
		List<Command> cmds = new ArrayList<>();
		List<String> errors = new ArrayList<>();
		
		StringReader reader = new StringReader();
		int line = 0;
		for (String i : file) {
			line++;
			reader.setLine(i);
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
