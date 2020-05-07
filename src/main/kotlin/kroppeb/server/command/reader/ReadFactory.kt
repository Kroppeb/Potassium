/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package kroppeb.server.command.reader

import kotlin.reflect.KProperty

interface ReadFactory<T> {
	fun Reader.parse():T
	fun read(reader: Reader) : T = reader.parse()
}

class ItemDelegate<T>(val item:T){
	operator fun getValue(thisRef: Any?, property: KProperty<*>) : T = item
}