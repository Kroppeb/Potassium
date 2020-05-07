/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package kroppeb.server.command.arguments

import kroppeb.server.command.reader.Reader
import kroppeb.server.command.reader.ReaderException

class SimpleDoubleRange(val minValue: Double, val maxValue: Double) {

	companion object {
		@Throws(ReaderException::class)
		fun read(reader: Reader): SimpleDoubleRange {
			val minValue: Double
			val maxValue: Double
			if (reader.tryRead("..")) {
				minValue = Double.NEGATIVE_INFINITY
				maxValue = reader.readDouble()
			} else {
				minValue = reader.readDouble()
				maxValue = if (reader.tryRead("..")) {
					if (reader.canRead() && !reader.isWhiteSpace) {
						reader.readDouble()
					} else {
						Double.POSITIVE_INFINITY
					}
				} else {
					minValue
				}
			}
			return SimpleDoubleRange(minValue, maxValue)
		}
	}

}