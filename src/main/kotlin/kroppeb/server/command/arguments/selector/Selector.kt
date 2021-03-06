/*
 * Copyright (c) 2021 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package kroppeb.server.command.arguments.selector

import kroppeb.server.command.arguments.DoubleRange.Companion.readDoubleRange
import kroppeb.server.command.arguments.readCompoundTag
import kroppeb.server.command.arguments.readIntRange
import kroppeb.server.command.arguments.selector.Sorter.Companion.parse
import kroppeb.server.command.reader.ReadFactory
import kroppeb.server.command.reader.Reader
import kroppeb.server.command.reader.ReaderException
import kroppeb.server.command.reader.read
import net.minecraft.entity.Entity
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.Vec3d
import java.util.*

interface Selector {
	class Group<T> {
		// TODO some of these can be parsed away from string sooner
		val positive: MutableSet<T> = HashSet()
		val negative: MutableSet<T> = HashSet()
	}

	class Tagable<T>(val isTag: Boolean, val value: T)

	fun getEntities(world: ServerWorld, pos: Vec3d, executor: Entity?): Collection<Entity>
	fun getEntities(source: ServerCommandSource): Collection<Entity> {
		return getEntities(source.world, source.position, source.entity)
	}

	interface SingleSelector : Selector {
		override fun getEntities(world: ServerWorld, pos: Vec3d, executor: Entity?): Collection<Entity> {
			val entity = getEntity(world, pos, executor) ?: return emptySet()
			return setOf(entity)
		}

		fun getEntity(world: ServerWorld, pos: Vec3d, executor: Entity?): Entity?
		fun getEntity(source: ServerCommandSource): Entity? {
			return getEntity(source.world, source.position, source.entity)
		}

		companion object:ReadFactory<SingleSelector> {
			override fun Reader.parse(): SingleSelector {
				val selector = Selector.read(this)
				if (selector is SingleSelector) return selector
				throw ReaderException("not limited to 1 entity") // TODO check limit value why
			}
		}
	}

	object Self : SinglePlayerSelector {
		override fun getPlayer(world: ServerWorld, pos: Vec3d, executor: Entity?): ServerPlayerEntity? {
			return if (executor is ServerPlayerEntity) executor else null
		}
	}

	class SelfFiltered : SinglePlayerSelector {
		override fun getPlayer(world: ServerWorld, pos: Vec3d, executor: Entity?): ServerPlayerEntity? {
			return null // TODO implement
		}
	}

	class SinglePlayer : SinglePlayerSelector {
		override fun getPlayer(world: ServerWorld, pos: Vec3d, executor: Entity?): ServerPlayerEntity? {
			return null
			// TODO implement
		}
	}

	object ClosestPlayer : SinglePlayerSelector {
		override fun getPlayer(world: ServerWorld, pos: Vec3d, executor: Entity?): ServerPlayerEntity? {
			var player: ServerPlayerEntity? = null
			var distance = Double.NEGATIVE_INFINITY
			for (worldPlayer in world.players) {
				val d = worldPlayer.squaredDistanceTo(pos)
				if (d < distance) {
					player = worldPlayer
					distance = d
				}
			}
			return player
		}
	}

	object RandomPlayer : SinglePlayerSelector {
		override fun getPlayer(world: ServerWorld, pos: Vec3d, executor: Entity?): ServerPlayerEntity? {
			return world.randomAlivePlayer
		}
	}

	object AllPlayers : PlayerSelector {
		override fun getPlayers(world: ServerWorld, pos: Vec3d, executor: Entity?): Collection<ServerPlayerEntity> {
			return world.players
		}
	}

	class PlayersFiltred : PlayerSelector {
		override fun getPlayers(world: ServerWorld, pos: Vec3d, executor: Entity?): Collection<ServerPlayerEntity> {
			return emptySet()
			// TODO implement
		}
	}

	object AllEntities : Selector {
		override fun getEntities(world: ServerWorld, pos: Vec3d, executor: Entity?): Collection<Entity> {
			return emptySet()
			// TODO implement
		}
	}

	class Complex : Selector {
		override fun getEntities(world: ServerWorld, pos: Vec3d, executor: Entity?): Collection<Entity> {
			return emptySet()
		}
	}

	companion object : ReadFactory<Selector> {
		override fun Reader.parse(): Selector {
			try {
				readChar('@')
				val kind = read()
				return if (tryRead('[')) {
					val sb = SelectorBuilder()
					when (kind) {
						's' -> sb.onlySelf = true
						'a' -> sb.onlyPlayers = true
						'e' -> {
						}
						'p' -> {
							sb.onlyPlayers = true
							sb.onlyOne = true
							sb.setLimit(1)
							sb.setSort(Sorter.NEAREST)
						}
						'r' -> {
							sb.onlyPlayers = true
							sb.onlyOne = true
							sb.setLimit(1)
							sb.setSort(Sorter.RANDOM)
						}
						else -> throw ReaderException("Unknown selector: @$kind")
					}
					readBuilder(sb).build()
				} else {
					when (kind) {
						's' -> Self
						'a' -> AllPlayers
						'e' -> AllEntities
						'p' -> ClosestPlayer
						'r' -> RandomPlayer
						else -> throw ReaderException("Unknown selector: @$kind")
					}
				}
			}catch (err:ReaderException){
				throw ReaderException("error while reading selector", err)
			}
		}

		fun Reader.readBuilder(sb : SelectorBuilder): SelectorBuilder {
			while (!tryRead(']')) {
				val option = readUnquotedString()
				next()
				readChar('=')
				next()
				when (option) {
					"x" -> sb.setX(readDouble())
					"y" -> sb.setY(readDouble())
					"z" -> sb.setZ(readDouble())
					"distance" -> sb.setDistance(readDoubleRange())
					"dx" -> sb.setDx(readDouble())
					"dy" -> sb.setDy(readDouble())
					"dz" -> sb.setDz(readDouble())
					"scores" -> {
						val map: MutableMap<String, IntRange> = HashMap()
						readChar('{')
						next()
						while (!tryRead('}')) {
							val key = readUnquotedString()
							next()
							readChar('=')
							next()
							map[key] = readIntRange()
							next()
							if (!tryRead(',')) {
								readChar('}')
								break
							}
							next()
						}
						sb.setScores(map)
					}
					"limit" -> sb.setLimit(readInt())
					"level" -> sb.setLevel(readIntRange())
					"gamemode" -> {
						if (sb.gamemode == null) sb.gamemode = Group()
						if (tryRead('!')) {
							next()
							sb.gamemode!!.negative.add(readUnquotedString())
						} else {
							sb.gamemode!!.positive.add(readUnquotedString())
						}
					}
					"name" -> {
						if (sb.name == null) sb.name = Group()
						if (tryRead('!')) {
							next()
							sb.name!!.negative.add(readUnquotedString())
						} else {
							sb.name!!.positive.add(readUnquotedString())
						}
					}
					"team" -> {
						if (sb.team == null) sb.team = Group()
						if (tryRead('!')) {
							next()
							sb.team!!.negative.add(readUnquotedString())
						} else {
							sb.team!!.positive.add(readUnquotedString())
						}
					}
					"type" -> {
						if (sb.type == null) sb.type = Group()
						if (tryRead('!')) {
							next()
							if (tryRead('#')) {
								next()
								sb.type!!.negative.add(Tagable(true, readIdentifier()))
							} else {
								sb.type!!.negative.add(Tagable(false, readIdentifier()))
							}
						} else {
							if (tryRead('#')) {
								next()
								sb.type!!.positive.add(Tagable(true, readIdentifier()))
							} else {
								sb.type!!.positive.add(Tagable(false, readIdentifier()))
							}
						}
					}
					"tag" -> {
						if (sb.tag == null) sb.tag = Group()
						if (tryRead('!')) {
							next()
							sb.tag!!.negative.add(readUnquotedString())
						} else {
							sb.tag!!.positive.add(readUnquotedString())
						}
					}
					"nbt" -> {
						if (sb.nbt == null) sb.nbt = Group()
						if (tryRead('!')) {
							next()
							sb.nbt!!.negative.add(readCompoundTag())
						} else {
							sb.nbt!!.positive.add(readCompoundTag())
						}
					}
					"predicate" -> {
						if (sb.predicate == null) sb.predicate = Group()
						if (tryRead('!')) {
							next()
							sb.predicate!!.negative.add(readIdentifier())
						} else {
							sb.predicate!!.positive.add(readIdentifier())
						}
					}
					"x_rotation" -> sb.setXRotation(readDoubleRange())
					"y_rotation" -> sb.setYRotation(readDoubleRange())
					"advancements" -> {
						//TODO add advancements
						var i = 1
						while (i > 0) {
							val c = read()
							if (c == '}') i--
							if (c == '{') i++
						}
					}
					"sort" -> sb.setSort(read(Sorter))
					else -> throw ReaderException("unknown selector option: $option")
				}
				next()
				if (!tryRead(',')) {
					readChar(']')
					break
				}
				next()
			}
			return sb
		}

	}
}

