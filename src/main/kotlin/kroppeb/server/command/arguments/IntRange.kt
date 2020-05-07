/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package kroppeb.server.command.arguments

import kroppeb.server.command.reader.Reader
import kroppeb.server.command.reader.ReaderException

class IntRange(val minValue: Int, val maxValue: Int) {

	companion object {
		@Throws(ReaderException::class)
		fun read(reader: Reader): IntRange {
			val minValue: Int
			val maxValue: Int
			if (reader.tryRead("..")) {
				minValue = Int.MIN_VALUE
				maxValue = reader.readInt()
			} else {
				minValue = reader.readInt()
				maxValue = if (reader.tryRead("..")) {
					if (reader.canRead() && !reader.isWhiteSpace) {
						reader.readInt()
					} else {
						Int.MAX_VALUE
					}
				} else {
					minValue
				}
			}
			return IntRange(minValue, maxValue)
		}
	}

}