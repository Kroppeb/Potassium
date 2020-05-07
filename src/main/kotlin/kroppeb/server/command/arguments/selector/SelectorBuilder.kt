/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package kroppeb.server.command.arguments.selector

import kroppeb.server.command.arguments.IntRange
import kroppeb.server.command.arguments.SimpleDoubleRange
import kroppeb.server.command.reader.ReaderException
import net.minecraft.nbt.CompoundTag
import net.minecraft.util.Identifier

class SelectorBuilder {
	@Throws(ReaderException::class)
	fun assertSingle(value: Any?, name: String) {
		if (value != null) throw ReaderException("Duplicate key in selector $name")
	}

	//region setters
	@Throws(ReaderException::class)
	fun setX(x: Double?) {
		assertSingle(this.x, "x")
		this.x = x
	}

	@Throws(ReaderException::class)
	fun setY(y: Double?) {
		assertSingle(this.y, "y")
		this.y = y
	}

	@Throws(ReaderException::class)
	fun setZ(z: Double?) {
		assertSingle(this.z, "z")
		this.z = z
	}

	@Throws(ReaderException::class)
	fun setDistance(distance: SimpleDoubleRange?) {
		assertSingle(this.distance, "distance")
		this.distance = distance
	}

	@Throws(ReaderException::class)
	fun setDx(dx: Double?) {
		assertSingle(this.dx, "dx")
		this.dx = dx
	}

	@Throws(ReaderException::class)
	fun setDy(dy: Double?) {
		assertSingle(this.dy, "dy")
		this.dy = dy
	}

	@Throws(ReaderException::class)
	fun setDz(dz: Double?) {
		assertSingle(this.dz, "dz")
		this.dz = dz
	}

	@Throws(ReaderException::class)
	fun setScores(scores: Map<String, IntRange>?) {
		assertSingle(this.scores, "scores")
		this.scores = scores
	}

	@Throws(ReaderException::class)
	fun setLimit(limit: Int?) {
		assertSingle(this.limit, "limit")
		this.limit = limit
	}

	@Throws(ReaderException::class)
	fun setSort(sort: String?) {
		assertSingle(this.sort, "sort")
		this.sort = sort
	}

	@Throws(ReaderException::class)
	fun setLevel(level: IntRange?) {
		assertSingle(this.level, "level")
		this.level = level
	}

	@Throws(ReaderException::class)
	fun setxRotation(xRotation: SimpleDoubleRange?) {
		assertSingle(this.xRotation, "x_rotation")
		this.xRotation = xRotation
	}

	@Throws(ReaderException::class)
	fun setyRotation(yRotation: SimpleDoubleRange?) {
		assertSingle(this.yRotation, "y_rotation")
		this.yRotation = yRotation
	}

	@Throws(ReaderException::class)
	fun setAdvancements(advancements: String?) {
		assertSingle(this.advancements, "advancements")
		this.advancements = advancements
	}

	//endregion
	//region properties
	private var x: Double? = null
	private var y: Double? = null
	private var z: Double? = null
	private var distance: SimpleDoubleRange? = null
	private var dx: Double? = null
	private var dy: Double? = null
	private var dz: Double? = null
	private var scores: Map<String, IntRange>? = null
	var team: Selector.Group<String>? = null
	private var limit: Int? = null
	private var sort: String? = null
	private var level: IntRange? = null
	var gamemode: Selector.Group<String>? = null
	var name: Selector.Group<String>? = null
	private var xRotation: SimpleDoubleRange? = null
	private var yRotation: SimpleDoubleRange? = null
	var type: Selector.Group<Selector.Tagable<Identifier>>? = null
	var tag: Selector.Group<String>? = null
	var nbt: Selector.Group<CompoundTag>? = null
	private var advancements: String? = null
	var predicate: Selector.Group<Identifier>? = null //endregion
	// TODO save order.
}