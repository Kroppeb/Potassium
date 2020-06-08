/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package kroppeb.server.command.arguments

import kroppeb.server.command.reader.Reader
import kroppeb.server.command.reader.ReaderException

class DoubleRange(val minValue: Double, val maxValue: Double) {

	companion object {
		@Throws(ReaderException::class)
		fun Reader.readDoubleRange(): DoubleRange {
			val minValue: Double
			val maxValue: Double
			if (tryRead("..")) {
				minValue = Double.NEGATIVE_INFINITY
				maxValue = readDouble()
			} else {
				minValue = readDouble()
				maxValue = if (tryRead("..")) {
					if (canRead() && !isWhiteSpace) {
						readDouble()
					} else {
						Double.POSITIVE_INFINITY
					}
				} else {
					minValue
				}
			}
			return DoubleRange(minValue, maxValue)
		}
	}

}