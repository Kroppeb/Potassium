/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package kroppeb.server.command.arguments

import kroppeb.server.command.reader.Reader
import kroppeb.server.command.reader.ReaderException

enum class IntComparator {
	LT {
		override fun compare(a: Int, b: Int): Boolean {
			return a < b
		}
	},
	LTE {
		override fun compare(a: Int, b: Int): Boolean {
			return a <= b
		}
	},
	EQ {
		override fun compare(a: Int, b: Int): Boolean {
			return a == b
		}
	},
	GTE {
		override fun compare(a: Int, b: Int): Boolean {
			return a >= b
		}
	},
	GT {
		override fun compare(a: Int, b: Int): Boolean {
			return a > b
		}
	};

	abstract fun compare(a: Int, b: Int): Boolean

	companion object {
		fun read(reader: Reader): IntComparator {
			val type = reader.readUntilWhitespace()
			return when (type) {
				"<" -> LT
				"<=" -> LTE
				"=" -> EQ
				">=" -> GTE
				">" -> GT
				else -> throw ReaderException("unknown comparison: $type")
			}
		}
	}
}
