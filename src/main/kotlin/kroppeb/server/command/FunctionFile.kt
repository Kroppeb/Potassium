/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package kroppeb.server.command

import kroppeb.server.command.arguments.Resource
import java.io.File
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.stream.Stream
import kotlin.streams.asSequence

class FunctionFile(val location: Resource, file: File) {
	val name: String?
	val commandName: String
	val file: File

	companion object {
		fun getNameSpace(ns: Path): Sequence<FunctionFile> {
			val namespace = ns.fileName.toString()
			val functions = ns.resolve("functions")
			return if (!Files.exists(functions)) emptySequence() else getFolder(namespace, functions, arrayOfNulls(0))
		}

		private fun getFolder(namespace: String, folder: Path, strings: Array<String?>): Sequence<FunctionFile> {
			return try {
				Files.list(folder).asSequence().flatMap { path: Path ->
					val file = path.toFile()
					if (file.isDirectory) {
						val sub = strings.copyOf(strings.size + 1)
						sub[strings.size] = file.name
						getFolder(namespace, path, sub)
					} else {
						val name = file.name
						if (!name.endsWith(".mcfunction")) emptySequence()
						else {
							val sub = Arrays.copyOf(strings, strings.size + 1)
							sub[strings.size] = name.substring(0, name.length - 11)
							sequenceOf(FunctionFile(Resource(namespace, sub), file))
						}
					}
				}
			} catch (e: IOException) {
				throw UncheckedIOException(e)
			}
		}
	}

	init {
		name = location.path[location.path.size - 1]

		// Can contain `.` which have to be replaced;
		val preName = location.namespace + "$$" + java.lang.String.join("$", *location.path)
		commandName = preName.replace('.', '!')
		this.file = file
	}
}