/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package kroppeb.server.command.reader

import net.minecraft.util.Identifier
import kotlin.reflect.KProperty

interface Reader {
	fun peek(): Char
	fun skip()
	fun skip(n: Int)
	fun canRead(): Boolean
	fun tryRead(c: Char): Boolean {
		if (canRead() && peek() == c) {
			skip()
			return true
		}
		return false
	}

	@Throws(ReaderException::class)
	fun read(): Char

	/**
	 * skips whitespace
	 * else throws
	 */
	@Throws(ReaderException::class)
	fun moveNext()

	/**
	 * Read until "special" character
	 * Forbidden chars = any whitespace, '=' ':' '"' '\''
	 */
	@Throws(ReaderException::class)
	fun readWord(): String

	/**
	 * readUntilWhiteSpace and asserts that we are at the end or will skip ws and we are not at the end
	 */
	@Throws(ReaderException::class)
	@Deprecated(replaceWith = ReplaceWith("Literal()"), message = "plz")
	fun readLiteral(): String {
		val res = readUntilWhitespace()
		if (canRead()) {
			moveNext()
			if (!canRead()) {
				throw ReaderException("trailing whitespace")
			}
		}
		return res
	}

	/**
	 * reads until whitespace or EOL
	 *
	 * @throws ReaderException empty string
	 */
	@Throws(ReaderException::class)
	fun readUntilWhitespace(): String

	@Throws(ReaderException::class)
	fun readChar(c: Char)

	@Throws(ReaderException::class)
	fun readNumber(): String?

	@Throws(ReaderException::class)
	fun readInt(): Int {
		return try {
			readNumber()!!.toInt()
		} catch (e: NumberFormatException) {
			throw ReaderException("invalid int: " + e.message, e)
		}
	}

	@Throws(ReaderException::class)
	fun readDouble(): Double {
		return if (tryRead('.')) {
			val number = "." + readNumber()
			try {
				number.toDouble()
			} catch (e: NumberFormatException) {
				throw ReaderException("Not a valid double: $number")
			}
		} else {
			val pre = readNumber()
			if (tryRead('.')) {
				val number = pre + "." + readNumber()
				try {
					number.toDouble()
				} catch (e: NumberFormatException) {
					throw ReaderException("Not a valid double: $number")
				}
			} else {
				try {
					pre!!.toDouble()
				} catch (e: NumberFormatException) {
					throw ReaderException("Not a valid double: $pre")
				}
			}
		}
	}

	@Throws(ReaderException::class)
	fun readSimpleDoubleIntOffset(): Double {
		return if (tryRead('.')) {
			val number = "." + readNumber()
			try {
				number.toDouble()
			} catch (e: NumberFormatException) {
				throw ReaderException("Not a valid double: $number")
			}
		} else {
			val pre = readNumber()
			if (tryRead('.')) {
				val number = pre + "." + readNumber()
				try {
					number.toDouble()
				} catch (e: NumberFormatException) {
					throw ReaderException("Not a valid double: $number")
				}
			} else {
				try {
					pre!!.toDouble() + .5
				} catch (e: NumberFormatException) {
					throw ReaderException("Not a valid double: $pre")
				}
			}
		}
	}

	val isQuotedStringStart: Boolean
		get() {
			val p = peek()
			return p == '"' || p == '\''
		}

	/**
	 * read quoted or unquoted string
	 *
	 * @return
	 */
	@Throws(ReaderException::class)
	fun readString(): String

	@Throws(ReaderException::class)
	fun readQuotedString(): String

	@Throws(ReaderException::class)
	fun readUnquotedString(): String

	@Throws(ReaderException::class)
	fun readIdentifier(): Identifier
	val isAllowedInUnquotedString: Boolean
		get() = canRead() && isAllowedInUnquotedString(peek())

	val isAllowedInIdentifier: Boolean
		get() = canRead() && isAllowedInIdentifier(peek())

	/**
	 * Not pure, calls `moveNext` if possible
	 *
	 * @return true if there is data to read
	 * @throws ReaderException if next char isn't whitespace
	 */
	@Throws(ReaderException::class)
	operator fun hasNext(): Boolean {
		if (canRead()) {
			moveNext()
			return canRead()
		}
		return false
	}

	@Throws(ReaderException::class)
	fun readLine(): String

	@Throws(ReaderException::class)
	fun endLine()
	val isWhiteSpace: Boolean
		get() {
			if (!canRead()) return false
			val c = peek()
			return c == ' '
		}

	/**
	 * move next if possible
	 *
	 * @throws ReaderException if the reader can't read;
	 */
	@Throws(ReaderException::class)
	operator fun next() {
		if (isWhiteSpace) moveNext()
	}

	/**
	 * move next if possible
	 *
	 * @return if there is new data to read.
	 */
	fun tryNext(): Boolean {
		if (isWhiteSpace) {
			try {
				moveNext()
			} catch (e: ReaderException) {
				throw RuntimeException("uh oh")
				// return false; // should not happen i think
			}
		}
		return canRead()
	}

	/**
	 * tries to read given literal
	 */
	fun tryReadLiteral(literal: String): Boolean
	fun tryRead(s: String): Boolean

	@Throws(ReaderException::class)
	fun readNamedPath(): String
	val isAllowedInNamedPath: Boolean
		get() = canRead() && isAllowedInNamedPath(peek())

	companion object {
		// make private?
		@Deprecated("")
		fun isAllowedInUnquotedString(c: Char): Boolean {
			return c in '0'..'9' || c in 'A'..'Z' || c in 'a'..'z' || c == '_' || c == '-' || c == '.' || c == '+'
		}

		// make this private?
		@Deprecated("")
		fun isAllowedInIdentifier(c: Char): Boolean {
			return c in '0'..'9' || c in 'a'..'z' || c == '_' || c == '-' || c == '.' || c == '/' || c == ':'
		}

		@Deprecated("")
		fun isAllowedInNamedPath(c: Char): Boolean {
			return c != ' ' && c != '"' && c != '[' && c != ']' && c != '.' && c != '{' && c != '}'
		}


	}

	fun moveOrEnd() {
		if (last() == ' ')
			return // Multiple calls to this won't do anything
		if (canRead()) {
			moveNext()
			if (!canRead()) {
				throw ReaderException("trailing whitespace")
			}
		}
	}

	fun last(): Char


	@Throws(ReaderException::class)
	@ReaderDslMarker
	operator fun <T> ReadFactory<T>.invoke(): T = readAndMove { parse() }

	@ReaderDslMarker
	operator fun <T> ReadFactory<T>.provideDelegate(thisRef: Any?, property: KProperty<*>): ItemDelegate<T> =
		ItemDelegate(readAndMove { parse() })
}

@ReaderDslMarker
inline fun <T> Reader.readAndMove(block: Reader.() -> T): T = block().also { moveOrEnd() }

fun Reader.expected(prefix: String, expected: String, got: String): Nothing =
	throw ReaderException("Expected `$prefix $expected` but got `$prefix $got` instead.")
