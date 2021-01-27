/*
 * Copyright (c) 2021 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package kroppeb.server.command

import kroppeb.server.command.commands.*
import kroppeb.server.command.reader.Reader
import kroppeb.server.command.reader.ReaderException
import kroppeb.server.command.reader.StringReader
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

object Parser {
	fun readFunction(reader: Reader): Command {
		val result: Command
		val command = reader.readLiteral()
		result = when (command) {
			"advancement" -> AdvancementCommand.read(reader)
			"attribute" -> AttributeCommand.read(reader)
			"bossbar" -> BossBarCommand.read(reader)
			"clear" -> ClearCommand.read(reader)
			"data" -> DataCommand.read(reader)
			"effect" -> EffectCommand.read(reader)
			"execute" -> ExecuteCommand.read(reader)
			"experience" -> ExperienceCommand.read(reader)
			"function" -> FunctionCommand.read(reader)
			"kill" -> KillCommand.read(reader)
			"locatebiome" -> LocateBiomeCommand.read(reader)
			"playsound" -> PlaySoundCommand.read(reader)
			"replaceitem" -> ReplaceItemCommand.read(reader)
			"scoreboard" -> ScoreboardCommand.read(reader)
			"setblock" -> SetBlockCommand.read(reader)
			"summon" -> SummonCommand.read(reader)
			"tag" -> TagCommand.read(reader)
			"title" -> TitleCommand.read(reader)
			"xp" -> ExperienceCommand.read(reader)
			else -> throw ReaderException("Unknown command: $command")
		}
		reader.endLine()
		return result
	}

	fun readFile(file: List<String>): List<Command> {
		val cmds: MutableList<Command> = ArrayList()
		val errors: MutableList<Pair<String,Throwable>> = ArrayList()
		val reader = StringReader()
		var line = 0
		for (i in file) {
			line++
			reader.line = i.trim { it <= ' ' } // this was automatically generated, idk
			if (!reader.canRead() || reader.peek() == '#') continue
			try {
				cmds.add(readFunction(reader))
			} catch (e: ReaderException) {
				errors.add("""line: $line, pos: ${reader.index + 1}
	${e.message}""" to e)
			}
		}
		if (errors.isEmpty()) return cmds

		val ex = ReaderException(errors.joinToString(separator = "\n") { it.first },errors[0].second)
		for(i in errors.drop(1))
			errors[0].second.addSuppressed(i.second)
		throw ex
	}

	fun readFile(file: Path): List<Command> {
		return readFile(Files.readAllLines(file, StandardCharsets.UTF_8))
	}
}
