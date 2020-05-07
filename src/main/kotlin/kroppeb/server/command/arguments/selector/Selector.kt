/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package kroppeb.server.command.arguments.selector

import kroppeb.server.command.arguments.IntRange
import kroppeb.server.command.arguments.SimpleDoubleRange
import kroppeb.server.command.arguments.readCompoundTag
import kroppeb.server.command.reader.ReadFactory
import kroppeb.server.command.reader.Reader
import kroppeb.server.command.reader.ReaderException
import net.minecraft.entity.Entity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.server.command.ServerCommandSource
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

	fun getEntities(world: ServerWorld, pos: Vec3d?, executor: Entity?): Collection<Entity>
	fun getEntities(source: ServerCommandSource): Collection<Entity?>? {
		return getEntities(source.world, source.position, source.entity)
	}

	interface SingleSelector : Selector {
		override fun getEntities(world: ServerWorld, pos: Vec3d?, executor: Entity?): Collection<Entity> {
			val entity = getEntity(world, pos, executor) ?: return emptySet()
			return setOf(entity)
		}

		fun getEntity(world: ServerWorld, pos: Vec3d?, executor: Entity?): Entity?
		fun getEntity(source: ServerCommandSource): Entity? {
			return getEntity(source.world, source.position, source.entity)
		}

		companion object:ReadFactory<SingleSelector> {
			@Throws(ReaderException::class)
			override fun Reader.parse(): SingleSelector {
				val selector = Selector.read(this)
				if (selector is SingleSelector) return selector
				throw ReaderException("not limited to 1 entity") // TODO check limit value why
			}
		}
	}

	object Self : SinglePlayerSelector {
		override fun getPlayer(world: ServerWorld, pos: Vec3d?, executor: Entity?): PlayerEntity? {
			return if (executor is PlayerEntity) executor else null
		}
	}

	class SelfFiltered : SinglePlayerSelector {
		override fun getPlayer(world: ServerWorld, pos: Vec3d?, executor: Entity?): PlayerEntity? {
			return null // TODO implement
		}
	}

	class SinglePlayer : SinglePlayerSelector {
		override fun getPlayer(world: ServerWorld, pos: Vec3d?, executor: Entity?): PlayerEntity? {
			return null
			// TODO implement
		}
	}

	object ClosestPlayer : SinglePlayerSelector {
		override fun getPlayer(world: ServerWorld, pos: Vec3d?, executor: Entity?): PlayerEntity? {
			var player: PlayerEntity? = null
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
		override fun getPlayer(world: ServerWorld, pos: Vec3d?, executor: Entity?): PlayerEntity? {
			return world.randomAlivePlayer
		}
	}

	object AllPlayers : PlayerSelector {
		override fun getPlayers(world: ServerWorld, pos: Vec3d?, executor: Entity?): Collection<PlayerEntity> {
			return world.players
		}
	}

	class PlayersFiltred : PlayerSelector {
		override fun getPlayers(world: ServerWorld, pos: Vec3d?, executor: Entity?): Collection<PlayerEntity> {
			return emptySet()
			// TODO implement
		}
	}

	object AllEntities : Selector {
		override fun getEntities(world: ServerWorld, pos: Vec3d?, executor: Entity?): Collection<Entity> {
			return emptySet()
			// TODO implement
		}
	}

	class Complex : Selector {
		override fun getEntities(world: ServerWorld, pos: Vec3d?, executor: Entity?): Collection<Entity> {
			return emptySet()
		}
	}

	companion object : ReadFactory<Selector> {
		@Throws(ReaderException::class)
		override fun Reader.parse(): Selector {
			readChar('@')
			val kind = read()
			return if (tryRead('[')) {
				when (kind) {
				}
				readBuilder()
				throw ReaderException("Selector parsing isn't implemented")
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
		}

		@Throws(ReaderException::class)
		fun Reader.readBuilder(): SelectorBuilder? {
			val sb = SelectorBuilder()
			while (!tryRead(']')) {
				val option = readUnquotedString()
				next()
				readChar('=')
				next()
				when (option) {
					"x" -> sb.setX(readDouble())
					"y" -> sb.setY(readDouble())
					"z" -> sb.setZ(readDouble())
					"distance" -> sb.setDistance(SimpleDoubleRange.read(this))
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
							map[key] = IntRange.read(this)
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
					"level" -> sb.setLevel(IntRange.read(this))
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
					"x_rotation" -> sb.setxRotation(SimpleDoubleRange.read(this))
					"y_rotation" -> sb.setyRotation(SimpleDoubleRange.read(this))
					"advancements" -> {
						//TODO add advancements
						var i = 1
						while (i > 0) {
							val c = read()
							if (c == '}') i--
							if (c == '{') i++
						}
					}
					"sort" -> sb.setSort(readUnquotedString())
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

