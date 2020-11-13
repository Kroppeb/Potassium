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
import kroppeb.server.command.arguments.SingleScore
import kroppeb.server.command.getScoreboard
import kroppeb.server.command.reader.*
import net.minecraft.scoreboard.ScoreboardCriterion
import net.minecraft.scoreboard.ScoreboardCriterion.createStatCriterion
import net.minecraft.scoreboard.ScoreboardObjective
import net.minecraft.scoreboard.ScoreboardPlayerScore
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.LiteralText
import net.minecraft.text.Text

sealed class ScoreboardCommand : Command {
	sealed class ObjectiveCommand : ScoreboardCommand() {
		object List : ObjectiveCommand() {
			override fun execute(source: ServerCommandSource): Int {
				return source.minecraftServer.scoreboard.objectives.size
			}
		}

		class Add(val name: String, val criterion: ScoreboardCriterion, val displayName: Text) : ObjectiveCommand() {
			override fun execute(source: ServerCommandSource): Int {
				val scoreboard: net.minecraft.scoreboard.Scoreboard = source.minecraftServer.scoreboard
				if (scoreboard.getNullableObjective(name) != null) throw InvocationError()
				scoreboard.addObjective(name, criterion, displayName, criterion.criterionType)
				return scoreboard.objectives.size
			}
		}

		sealed class Modify(val name: String) : ObjectiveCommand() {
			class DisplayName(name: String, val displayName: Text) : Modify(name) {
				override fun execute(source: ServerCommandSource): Int {
					val objective = source.getScoreboard(name)
					if (objective.displayName != displayName) {
						objective.displayName = displayName
					}
					return 0
				}
			}

			class RenderType(name: String, val type: ScoreboardCriterion.RenderType) : Modify(name) {
				override fun execute(source: ServerCommandSource): Int {
					val objective = source.getScoreboard(name)
					if (objective.renderType != type) {
						objective.renderType = type
					}
					return 0
				}
			}
		}

		class Remove(val name: String) : ObjectiveCommand() {
			override fun execute(source: ServerCommandSource): Int {
				val objective = source.getScoreboard(name)
				val scoreboard: net.minecraft.scoreboard.Scoreboard = source.minecraftServer.scoreboard
				scoreboard.removeObjective(objective)
				return scoreboard.objectives.size
			}
		}

		class SetDisplay(val slot: Int, val name: String?) : ObjectiveCommand() {
			override fun execute(source: ServerCommandSource): Int {
				val objective = if (name == null) null else source.getScoreboard(name)
				val scoreboard = source.minecraftServer.scoreboard
				if (scoreboard.getObjectiveForSlot(slot) === objective) throw InvocationError()
				scoreboard.setObjectiveSlot(slot, objective)
				return 0
			}
		}

	}

	sealed class PlayerCommand : ScoreboardCommand() {
		class Add(private val score: Score, private val value: Int) : PlayerCommand() {
			override fun execute(source: ServerCommandSource): Int {
				return score.addValue(source, value)
			}

		}

		class Get(private val score: SingleScore) : PlayerCommand() {
			override fun execute(source: ServerCommandSource): Int {
				return score.getValue(source)
			}

		}

		class Set(private val score: Score, private val value: Int) : PlayerCommand() {
			override fun execute(source: ServerCommandSource): Int {
				return score.setValue(source, value)
			}

		}

		class Reset(private val score: Score) : PlayerCommand() {
			override fun execute(source: ServerCommandSource): Int {
				return score.resetValue(source)
			}

		}

		class Operation(val target: Score, val op: Op, val source: Score) : PlayerCommand() {
			override fun execute(source: ServerCommandSource): Int {
				var r = 0
				val scoreboard = source.minecraftServer.scoreboard

				val targets = target.scoreHolder.getEntities(source)
				val sources = this.source.scoreHolder.getEntities(source)

				val targetObjective = source.getScoreboard(target.name)
				val sourceObjective = source.getScoreboard(this.source.name)

				for (t in targets) {
					for (s in sources) {
						op.apply(scoreboard.getPlayerScore(t,targetObjective), scoreboard.getPlayerScore(s,sourceObjective))
						r += scoreboard.getPlayerScore(t,targetObjective).score
					}
				}
				return r
			}

			enum class Op {
				ADD {
					override fun apply(target: ScoreboardPlayerScore, source: ScoreboardPlayerScore) {
						target.incrementScore(source.score)
					}
				},
				SUB {
					override fun apply(target: ScoreboardPlayerScore, source: ScoreboardPlayerScore) {
						target.incrementScore(-source.score)
					}
				},
				MUL {
					override fun apply(target: ScoreboardPlayerScore, source: ScoreboardPlayerScore) {
						target.score = target.score * source.score
					}
				},
				DIV {
					override fun apply(target: ScoreboardPlayerScore, source: ScoreboardPlayerScore) {
						target.score = target.score / source.score
					}
				},
				MOD {
					override fun apply(target: ScoreboardPlayerScore, source: ScoreboardPlayerScore) {
						target.score = target.score % source.score
					}
				},
				ASS {
					override fun apply(target: ScoreboardPlayerScore, source: ScoreboardPlayerScore) {
						target.score = source.score
					}
				},
				MIN {
					override fun apply(target: ScoreboardPlayerScore, source: ScoreboardPlayerScore) {
						target.score = target.score.coerceAtMost(source.score)
					}
				},
				MAX {
					override fun apply(target: ScoreboardPlayerScore, source: ScoreboardPlayerScore) {
						target.score = target.score.coerceAtLeast(source.score)
					}
				},
				SWP {
					override fun apply(target: ScoreboardPlayerScore, source: ScoreboardPlayerScore) {
						val s = source.score
						val t = target.score
						source.score = t
						target.score = s
					}
				};

				//abstract void Apply(ScoreboardPlayerScore target, ScoreboardPlayerScore source);
				abstract fun apply(target: ScoreboardPlayerScore, source: ScoreboardPlayerScore)
			}

			companion object : ReadFactory<Operation> {
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
		override fun Reader.parse(): ScoreboardCommand {
			return when (val sub = Literal()) {
				"objectives" -> this.readObjective()
				"players" -> this.readPlayer()
				else -> throw ReaderException("Unexpected scoreboard literal: $sub")
			}
		}

		private fun Reader.readObjective(): ObjectiveCommand {
			when (val sub: String = Literal()) {
				"list" -> {
					return ObjectiveCommand.List
				}
				"add" -> {
					val name = Scoreboard()
					val criterionString = Literal()
					val criterion = createStatCriterion(criterionString).orElse(null)
						?: throw ReaderException("Invalid criterion: $criterionString")
					if (!canRead())
						return ObjectiveCommand.Add(name, criterion, LiteralText(name))
					val text = Text()
					return ObjectiveCommand.Add(name, criterion, text)
				}
				"modify" -> {
					val name = Scoreboard()
					return when (val mod = Literal()) {
						"displayname" -> {
							val text = Text()
							ObjectiveCommand.Modify.DisplayName(name, text)
						}
						"rendertype" -> {
							val type = when (val typeString = Literal()) {
								"hearts" -> ScoreboardCriterion.RenderType.HEARTS
								"integer" -> ScoreboardCriterion.RenderType.INTEGER
								else -> expected(
									"scoreboard objectives modify <name> rendertype",
									"(hearts|integer)",
									typeString)
							}
							ObjectiveCommand.Modify.RenderType(name, type)
						}
						else -> expected("scoreboard objectives modify <name>", "(displayname|rendertype)", mod)
					}
				}
				"remove" -> {
					val name = Scoreboard()
					return ObjectiveCommand.Remove(name)
				}
				"setdisplay" -> {
					val slotName = Literal()
					val slot = net.minecraft.scoreboard.Scoreboard.getDisplaySlotId(slotName)
					if (slot == -1) {
						throw ReaderException("Unknown objective slot: $slotName")
					}
					return ObjectiveCommand.SetDisplay(slot, if(canRead()) Literal() else null)
				}
				else -> expected("scoreboard objectives", "", sub)
			}
		}


		private fun Reader.readPlayer(): ScoreboardCommand {
			return when (val sub = Literal()) {
				"list" -> throw ReaderException("scoreboard players list isn't supported here atm, I'm lazy *cries*") // TODO
				"add" -> PlayerCommand.Add(Score(), Int())
				"remove" -> PlayerCommand.Add(Score(), -Int())
				"set" -> PlayerCommand.Set(Score(), Int())
				"reset" -> PlayerCommand.Reset(Score())
				"operation" -> PlayerCommand.Operation()
				"get" -> PlayerCommand.Get(SingleScore())
				else -> throw ReaderException("Unexpected scoreboard players literal: $sub")
			}
		}
	}
}


