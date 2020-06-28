/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package kroppeb.server.command.commands

import kroppeb.server.command.Command
import kroppeb.server.command.InvocationError
import kroppeb.server.command.arguments.selector.PlayerSelector
import kroppeb.server.command.reader.*
import net.minecraft.entity.boss.BossBar
import net.minecraft.entity.boss.BossBar.Color.*
import net.minecraft.entity.boss.BossBar.Color.BLUE
import net.minecraft.entity.boss.BossBar.Style
import net.minecraft.entity.boss.BossBar.Style.*
import net.minecraft.entity.boss.CommandBossBar
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.minecraft.text.Texts
import net.minecraft.util.Identifier


sealed class BossBarCommand : Command{
	class Add(val id: Identifier, val name: Text): BossBarCommand() {
		override fun execute(source: ServerCommandSource): Int {
			val manager = source.minecraftServer.bossBarManager
			if(manager.get(id) != null)
				throw InvocationError()
			manager.add(id, Texts.parse(source, name, null, 0))
			return manager.all.size
		}
	}

	sealed class Get(val id:Identifier):BossBarCommand(){
		override fun execute(source: ServerCommandSource): Int {
			val manager = source.minecraftServer.bossBarManager
			val bossBar = manager.get(id) ?: throw InvocationError()
			return get(bossBar)
		}
		abstract fun get(bossBar: CommandBossBar): Int
		class Max(id: Identifier):Get(id) {
			override fun get(bossBar: CommandBossBar): Int {
				return bossBar.maxValue
			}
		}

		class Players(id: Identifier):Get(id) {
			override fun get(bossBar: CommandBossBar): Int {
				return bossBar.players.size
			}
		}
		class Value(id: Identifier):Get(id) {
			override fun get(bossBar: CommandBossBar): Int {
				return bossBar.value
			}
		}
		class Visible(id: Identifier):Get(id) {
			override fun get(bossBar: CommandBossBar): Int {
				return if(bossBar.isVisible) 1 else 0
			}
		}

		companion object:ReadFactory<Get>{
			override fun Reader.parse(): Get {
				val id = Id()
				return when(val sub = Literal()){
					"max" -> Max(id)
					"players" -> Players(id)
					"value" -> Value(id)
					"visible" -> Visible(id)
					else -> expected("bossbar get", "(max|players|value|visible)", sub)
				}
			}

		}
	}

	object List:BossBarCommand() {
		override fun execute(source: ServerCommandSource): Int {
			return source.minecraftServer.bossBarManager.all.size
		}
	}

	class Remove(val id: Identifier):BossBarCommand() {
		override fun execute(source: ServerCommandSource): Int {
			val manager = source.minecraftServer.bossBarManager
			val bossBar = manager.get(id) ?: throw InvocationError()
			bossBar.clearPlayers()
			manager.remove(bossBar)
			return manager.all.size
		}
	}

	sealed class Set(val id:Identifier):BossBarCommand(){
		override fun execute(source: ServerCommandSource): Int {
			val manager = source.minecraftServer.bossBarManager
			val bossBar = manager.get(id) ?: throw InvocationError()
			return apply(bossBar, source)
		}
		abstract fun apply(bossBar: CommandBossBar, source:ServerCommandSource): Int
		class Color(id: Identifier, val color:BossBar.Color):Set(id) {
			override fun apply(
				bossBar: CommandBossBar,
				source: ServerCommandSource): Int {
				if(bossBar.color == color)
					throw InvocationError()
				bossBar.color = color
				return 0
			}
		}

		class Max(id: Identifier, val value:Int):Set(id) {
			override fun apply(
				bossBar: CommandBossBar,
				source: ServerCommandSource): Int {
				if(bossBar.maxValue == value)
					throw InvocationError()
				bossBar.maxValue = value
				return value
			}
		}

		class Name(id: Identifier, val name: Text):Set(id) {
			override fun apply(
				bossBar: CommandBossBar,
				source: ServerCommandSource): Int {
				val text = Texts.parse(source, name, null, 0)
				if(bossBar.name == text)
					throw InvocationError()
				bossBar.name = text
				return 0
			}
		}

		class Players(id: Identifier, val players: PlayerSelector): Set(id) {
			override fun apply(
				bossBar: CommandBossBar,
				source: ServerCommandSource): Int {
				if(!bossBar.addPlayers(players.getPlayers(source))) // TODO: this function is shit
					throw InvocationError()
				return bossBar.players.size
			}
		}

		class PlayersClear(id: Identifier): Set(id) {
			override fun apply(
				bossBar: CommandBossBar,
				source: ServerCommandSource): Int {
				if(bossBar.players.isEmpty())
					throw InvocationError()
				bossBar.clearPlayers()
				return 0
			}
		}

		class Style(id: Identifier, val style:BossBar.Style):Set(id) {
			override fun apply(
				bossBar: CommandBossBar,
				source: ServerCommandSource): Int {
				if(bossBar.overlay == style)
					throw InvocationError()
				bossBar.overlay = style
				return 0
			}
		}

		class Value(id: Identifier, val value:Int):Set(id) {
			override fun apply(
				bossBar: CommandBossBar,
				source: ServerCommandSource): Int {
				if(bossBar.value == value)
					throw InvocationError()
				bossBar.value = value
				return value
			}
		}
		class Visible(id: Identifier, val visible: Boolean):Set(id) {
			override fun apply(
				bossBar: CommandBossBar,
				source: ServerCommandSource): Int {
				if(bossBar.isVisible == visible)
					throw InvocationError()
				bossBar.isVisible = visible
				return 0
			}
		}

		companion object:ReadFactory<Set>{
			override fun Reader.parse(): Set {
				val id = Id()
				return when(val sub = Literal()){
					"color" -> Color(id, when(val color = Literal()){
						"blue" -> BLUE
						"green" -> GREEN
						"pink" -> PINK
						"purple" -> PURPLE
						"red" -> RED
						"white" -> WHITE
						"yellow" -> YELLOW
						else -> expected("bossbar set color", "(blue|green|pink|purple|red|white|yellow)", color)
					})
					"max" -> Max(id, Int())
					"name" -> Name(id, Text())
					"players" -> if(canRead()) Players(id, PlayerSelector()) else PlayersClear(id)
					"style" -> Style(id, when(val style = Literal()){
						"notched_6" -> NOTCHED_6
						"notched_10" -> NOTCHED_10
						"notched_12" -> NOTCHED_12
						"notched_20" -> NOTCHED_20
						"progress" -> PROGRESS
						else -> expected("bossbar set style", "(notched_6|notched_10|notched_12|notched_20|progress)", style)
					})
					"value" -> Value(id, Int())
					"visible" -> Visible(id, Boolean())
					else -> expected("bossbar set", "(color|max|name|players|style|value|visible)", sub)
				}
			}

		}
	}

	companion object : ReadFactory<BossBarCommand>{
		override fun Reader.parse(): BossBarCommand =
			when(val sub = Literal()){
				"add" -> Add(Id(), Text())
				"get" -> Get()
				"list" -> List
				"remove" -> Remove(Id())
				"set" -> Set()
				else -> expected("bossbar", "(add|get|list|remove|set)",sub)
			}

	}
}
