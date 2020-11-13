/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package kroppeb.server.command.arguments

import kroppeb.server.command.reader.ReadFactory
import kroppeb.server.command.reader.Reader
import kroppeb.server.command.reader.ReaderException

class Resource(val namespace: String?, val path: Array<String?>) {
	override fun toString(): String {
		return (namespace ?: "minecraft") + ':' + java.lang.String.join("/", *path)
	}

	companion object:ReadFactory<Resource> {
		override fun Reader.parse(): Resource {
			val ns = readIdentifier()
			return Resource(ns.namespace, ns.path.split("/").toTypedArray())
		}
	}

}
