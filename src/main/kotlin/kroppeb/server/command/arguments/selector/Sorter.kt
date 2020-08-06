/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package kroppeb.server.command.arguments.selector

import kroppeb.server.command.reader.ReadFactory
import kroppeb.server.command.reader.Reader
import kroppeb.server.command.reader.ReaderException

enum class Sorter {
	NEAREST, FURTHEST, RANDOM, ARBITRARY;

	companion object : ReadFactory<Sorter> {
		override fun Reader.parse(): Sorter = when (val sort = readUnquotedString()) {
			"nearest" -> NEAREST
			"furthest" -> FURTHEST
			"random" -> RANDOM
			"arbitrary" -> ARBITRARY
			else -> throw ReaderException("Unknown sorting option `$sort`")
		}
	}
}
