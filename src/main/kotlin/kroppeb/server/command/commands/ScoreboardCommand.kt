/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package kroppeb.server.command.commands

import kroppeb.server.command.Command
import kroppeb.server.command.InvocationError
import kroppeb.server.command.arguments.Score
import kroppeb.server.command.commands.ScoreboardCommand.PlayerCommand.Reset
import kroppeb.server.command.reader.*
import net.minecraft.scoreboard.ScoreboardPlayerScore
import net.minecraft.server.command.ServerCommandSource

abstract class ScoreboardCommand : Command {
	abstract class ObjectiveCommand : ScoreboardCommand()
	abstract class PlayerCommand : ScoreboardCommand() {
		class Add(private val score: Score, private val value: Int) : PlayerCommand() {
			@Throws(InvocationError::class)
			override fun execute(source: ServerCommandSource): Int {
				return score.addValue(source.world, source.position, source.entity, value)
			}

		}

		class Set(private val score: Score, private val value: Int) : PlayerCommand() {
			@Throws(InvocationError::class)
			override fun execute(source: ServerCommandSource): Int {
				return score.setValue(source.world, source.position, source.entity, value)
			}

		}

		class Reset(private val score: Score) : PlayerCommand() {
			@Throws(InvocationError::class)
			override fun execute(source: ServerCommandSource): Int {
				return score.resetValue(source.world, source.position, source.entity)
			}

		}

		class Operation(val target: Score, val op: Op, val source: Score) : PlayerCommand() {
			override fun execute(source: ServerCommandSource): Int {
				if (true) TODO("This needs fixing")
				var r = 0
				val targets = target.all
				val sources = this.source.all
				for (t in targets!!) {
					for (s in sources!!) {
						op.apply(t, s)
						r += t!!.score
					}
				}
				return r
			}

			enum class Op {
				ADD {
					override fun apply(target: ScoreboardPlayerScore?, source: ScoreboardPlayerScore?) {
						target!!.incrementScore(source!!.score)
					}
				},
				SUB {
					override fun apply(target: ScoreboardPlayerScore?, source: ScoreboardPlayerScore?) {
						target!!.incrementScore(-source!!.score)
					}
				},
				MUL {
					override fun apply(target: ScoreboardPlayerScore?, source: ScoreboardPlayerScore?) {
						target!!.score = target.score * source!!.score
					}
				},
				DIV {
					override fun apply(target: ScoreboardPlayerScore?, source: ScoreboardPlayerScore?) {
						target!!.score = target.score / source!!.score
					}
				},
				MOD {
					override fun apply(target: ScoreboardPlayerScore?, source: ScoreboardPlayerScore?) {
						target!!.score = target.score % source!!.score
					}
				},
				ASS {
					override fun apply(target: ScoreboardPlayerScore?, source: ScoreboardPlayerScore?) {
						target!!.score = source!!.score
					}
				},
				MIN {
					override fun apply(target: ScoreboardPlayerScore?, source: ScoreboardPlayerScore?) {
						target!!.score = Math.min(target.score, source!!.score)
					}
				},
				MAX {
					override fun apply(target: ScoreboardPlayerScore?, source: ScoreboardPlayerScore?) {
						target!!.score = Math.max(target.score, source!!.score)
					}
				},
				SWP {
					override fun apply(target: ScoreboardPlayerScore?, source: ScoreboardPlayerScore?) {
						val s = source!!.score
						val t = target!!.score
						source.score = t
						target.score = s
					}
				};

				//abstract void Apply(ScoreboardPlayerScore target, ScoreboardPlayerScore source);
				abstract fun apply(target: ScoreboardPlayerScore?, source: ScoreboardPlayerScore?)
			}

			companion object : ReadFactory<Operation> {
				@Throws(ReaderException::class)
				override fun Reader.parse(): Operation {
					val target = Score()
					val op: Op = when (val opString = Literal()) {
						"+=" -> Op.ADD
						"-=" -> Op.SUB
						"*=" -> Op.MUL
						"/=" -> Op.DIV
						"%=" -> Op.MOD
						"=" -> Op.ASS
						"<" -> Op.MIN
						">" -> Op.MAX
						"><" -> Op.SWP
						else -> throw ReaderException("Unknown scoreboard operation: $opString")
					}
					val source: Score = Score()
					return Operation(target, op, source)
				}
			}

		}
	}

	companion object : ReadFactory<ScoreboardCommand> {
		@Throws(ReaderException::class)
		override fun Reader.parse(): ScoreboardCommand {
			return when (val sub = Literal()) {
				"objectives" -> this.readObjective()
				"players" -> this.readPlayer()
				else -> throw ReaderException("Unexpected scoreboard literal: $sub")
			}
		}

		@Throws(ReaderException::class)
		private fun Reader.readObjective(): ScoreboardCommand {
			throw ReaderException("Unexpected scoreboard objectives is not implemented")
			/*String sub = reader.readLiteral();
		switch (sub){
			case "add":
			case "players":
			default:
			throw new ReaderException("Unexpected scoreboard objectives literal: " + sub);
		}*/
		}

		@Throws(ReaderException::class)
		private fun Reader.readPlayer(): ScoreboardCommand {
			return when (val sub = Literal()) {
				"list" -> throw ReaderException("scoreboard players list isn't supported here atm, I'm lazy *cries*") // TODO
				"add" -> PlayerCommand.Add(Score(), Int())
				"remove" -> PlayerCommand.Add(Score(), -Int())
				"set" -> PlayerCommand.Set(Score(), Int())
				"reset" -> PlayerCommand.Reset(Score())
				"operation" -> PlayerCommand.Operation()
				else -> throw ReaderException("Unexpected scoreboard players literal: $sub")
			}
		}
	}
}