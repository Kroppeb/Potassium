/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package kroppeb.server.command.arguments

import com.mojang.brigadier.exceptions.CommandSyntaxException
import kroppeb.server.command.reader.*
import net.minecraft.command.arguments.NbtPathArgumentType.NbtPath
import net.minecraft.nbt.Tag
import net.minecraft.server.command.ServerCommandSource

abstract class NbtDataSource {
	abstract fun getData(source: ServerCommandSource): List<Tag?>
	open fun getTag(source: ServerCommandSource): Tag? {
		val iterator = getData(source).iterator()
		val tag = iterator.next()
		return if (iterator.hasNext()) {
			throw RuntimeException("too much data") // TODO decent error;
		} else {
			tag
		}
	}

	internal class Constant(val data: Tag) : NbtDataSource() {
		val singleton: List<Tag?>
		override fun getTag(source: ServerCommandSource): Tag? {
			return data
		}

		override fun getData(source: ServerCommandSource): List<Tag?> {
			return singleton
		}

		init {
			singleton = listOf(data)
		}
	}

	class NbtDataPathSource(val container: NbtDataContainer, val path: NbtPath?) : NbtDataSource() {
		override fun getData(source: ServerCommandSource): List<Tag?> {
			val tag: Tag? = container.getTag(source)
			return if (path == null) listOf(tag) else try {
				path[tag]
			} catch (e: CommandSyntaxException) {
				throw RuntimeException(e) // TODO decent error
			}
		}

		override fun getTag(source: ServerCommandSource): Tag? {
			return if (path == null) container.getTag(source) else super.getTag(source)
		}

	}

	@ReaderDslMarker
	companion object : ReadFactory<NbtDataSource> {
		@Throws(ReaderException::class)
		override fun Reader.parse(): NbtDataSource {
			return when (val type = Literal()) {
				"value" -> Constant(readTag())
				"from" -> {
					val source = NbtDataContainer()
					if (canRead()) NbtDataPathSource(source, Path())
					else NbtDataPathSource(source, null)
				}
				else -> throw ReaderException("Unknown nbt data source: $type")
			}
		}
	}
}