/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package kroppeb.server.command.arguments.selector

import kroppeb.server.command.arguments.DoubleRange
import kroppeb.server.command.reader.ReaderException
import net.minecraft.entity.Entity
import net.minecraft.nbt.CompoundTag
import net.minecraft.scoreboard.Scoreboard
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier
import java.util.function.Predicate

class SelectorBuilder {
	fun assertSingle(value: Any?, name: String) {
		if (value != null) throw ReaderException("Duplicate key in selector $name")
	}

	//region setters
	fun setX(x: Double) {
		assertSingle(this.x, "x")
		this.x = x
	}

	fun setY(y: Double) {
		assertSingle(this.y, "y")
		this.y = y
	}

	fun setZ(z: Double) {
		assertSingle(this.z, "z")
		this.z = z
	}

	fun setDistance(distance: DoubleRange) {
		assertSingle(this.distance, "distance")
		this.distance = distance
	}

	fun setDx(dx: Double) {
		assertSingle(this.dx, "dx")
		this.dx = dx
	}

	fun setDy(dy: Double) {
		assertSingle(this.dy, "dy")
		this.dy = dy
	}

	fun setDz(dz: Double) {
		assertSingle(this.dz, "dz")
		this.dz = dz
	}

	fun setScores(scores: Map<String, kotlin.ranges.IntRange>) {
		assertSingle(this.scores, "scores")
		result += Predicate { entity ->
			val scoreboard: Scoreboard = entity.server!!.scoreboard
			val string = entity.entityName

			for (entry in scores.entries) {
				val scoreboardObjective = scoreboard.getNullableObjective(entry.key)
						?: return@Predicate false
				if (!scoreboard.playerHasObjective(string, scoreboardObjective))
					return@Predicate false

				val scoreboardPlayerScore = scoreboard.getPlayerScore(
						string,
						scoreboardObjective)
				if(scoreboardPlayerScore.score !in entry.value)
					return@Predicate false
			}
			return@Predicate true
		}
		this.scores = scores
	}

	fun setLimit(limit: Int) {
		onlyOne = onlyOne || limit == 1
		if(limit < 1)
			throw ReaderException("limit has to be at least 1")
		assertSingle(this.limit, "limit")
		this.limit = limit
	}

	fun setSort(sort: Sorter) {
		assertSingle(this.sort, "sort")
		this.sort = sort
	}

	fun setLevel(level: IntRange) {
		assertSingle(this.level, "level")
		if(level.first < 0 || level.last < 0)
			throw ReaderException("level can't be negative")

		result += Predicate { entity ->
			entity as ServerPlayerEntity
			entity.experienceLevel in level
		}
		this.level = level
	}

	fun setXRotation(xRotation: DoubleRange) {
		assertSingle(this.xRotation, "x_rotation")

		this.xRotation = xRotation
	}

	fun setYRotation(yRotation: DoubleRange) {
		assertSingle(this.yRotation, "y_rotation")
		this.yRotation = yRotation
	}

	fun setAdvancements(advancements: String?) {
		assertSingle(this.advancements, "advancements")
		this.advancements = advancements
	}

	fun build(): Selector {
		// TODO build
		return Selector.Self;
	}

	//endregion
	//region properties
	private var x: Double? = null
	private var y: Double? = null
	private var z: Double? = null
	private var distance: DoubleRange? = null
	private var dx: Double? = null
	private var dy: Double? = null
	private var dz: Double? = null
	private var scores: Map<String, IntRange>? = null
	var team: Selector.Group<String>? = null
	private var limit: Int? = null
	private var sort: Sorter? = null
	private var level: IntRange? = null
	var gamemode: Selector.Group<String>? = null
	var name: Selector.Group<String>? = null
	private var xRotation: DoubleRange? = null
	private var yRotation: DoubleRange? = null
	var type: Selector.Group<Selector.Tagable<Identifier>>? = null
	var tag: Selector.Group<String>? = null
	var nbt: Selector.Group<CompoundTag>? = null
	private var advancements: String? = null


	//endregion

	var predicate: Selector.Group<Identifier>? = null
	var onlyPlayers = false
	var onlyOne = false
	var onlySelf = false
	// TODO save order.

	val result: MutableList<Predicate<Entity>> = mutableListOf()
}

