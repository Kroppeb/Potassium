/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package kroppeb.server.command.reader

import net.minecraft.util.Identifier

class StringReader : Reader {
	var line: String = ""
	set(value){
		field = value
		index = 0
	}
	var index = 0

	constructor(line: String) {
		this.line = line
	}

	constructor()


	override fun peek(): Char {
		return line[index]
	}

	override fun skip() {
		if (++index >= line.length) {
			// idk;
		}
	}

	override fun skip(n: Int) {
		index += n
		if (index >= line.length) {
			// idk;
		}
	}

	override fun canRead(): Boolean {
		return index < line.length
	}

	@Throws(ReaderException::class)
	override fun read(): Char {
		if (!canRead()) eof("further")
		val c = peek()
		skip()
		return c
	}

	@Throws(ReaderException::class)
	private fun eof(c: Char) {
		eof("'$c'")
	}

	@Throws(ReaderException::class)
	private fun eof(s: String) {
		throw ReaderException(
				"Expected to read $s but we are at the end of the file"
		)
	}

	/**
	 * skips whitespace
	 * else throws
	 */
	@Throws(ReaderException::class)
	override fun moveNext() {
		if (isWhiteSpace) {
			do {
				skip()
			} while (isWhiteSpace(peek()))
		} else expected("whitespace")
	}

	override fun last(): Char {
		return line[index-1]
	}

	@Throws(ReaderException::class)
	private fun expected(s: String) {
		/*if(s == "whitespace")
			error("hi: + $line + ${index+1}")*/
		throw ReaderException(
				"Expected to read $s"
		)
	}

	private fun isWhiteSpace(c: Char): Boolean {
		return c == ' ' || c == '\t'
	}

	/**
	 * Read until "special" character
	 * Forbidden chars = any whitespace, EOL, '=' ':' '"' '\''
	 */
	@Throws(ReaderException::class)
	override fun readWord(): String {
		if (endWord(peek())) expected("word")
		val pos = index
		do {
			skip()
		} while (!endWord(peek()))
		return line!!.substring(pos, index)
	}

	@Throws(ReaderException::class)
	override fun readUntilWhitespace(): String {
		val start = index
		return if (!isWhiteSpace) {
			do {
				read()
			} while (canRead() && !isWhiteSpace)
			line!!.substring(start, index)
		} else throw ReaderException("Expected a literal")
	}

	fun endWord(c: Char): Boolean {
		return isWhiteSpace(c) || c == '\n' || c == '\r' || c == '=' || c == ':' || c == '"' || c == '\''
	}

	@Throws(ReaderException::class)
	override fun readChar(c: Char) {
		if (read() != c) expected("'$c'")
	}

	/**
	 * read quoted or unquoted string
	 *
	 * @return
	 */
	@Throws(ReaderException::class)
	override fun readString(): String {
		return if (isQuotedStringStart) readQuotedString() else readUnquotedString()
	}

	@Throws(ReaderException::class)
	override fun readQuotedString(): String {
		if (!isQuotedStringStart) expected("quote")
		val pos = index + 1
		val start = read()
		var c: Char
		do {
			c = read()
			if (c == '/') skip()
		} while (c != start)
		return line!!.substring(pos, index - 1)
	}

	@Throws(ReaderException::class)
	override fun readUnquotedString(): String {
		if (!isAllowedInUnquotedString) expected("unquoted string")
		val pos = index
		while (isAllowedInUnquotedString) read()
		return line!!.substring(pos, index)
	}

	@Throws(ReaderException::class)
	override fun readIdentifier(): Identifier {
		if (!isAllowedInIdentifier) expected("identifier")
		val pos = index
		do skip() while (isAllowedInIdentifier)
		val str = line!!.substring(pos, index)
		return Identifier.tryParse(str) ?: throw ReaderException("Couldn't parse $str as an identifier")
	}

	@Throws(ReaderException::class)
	override fun readNamedPath(): String {
		if (!isAllowedInNamedPath) expected("identifier")
		val pos = index
		do skip() while (isAllowedInNamedPath)
		return line!!.substring(pos, index)
	}

	@Throws(ReaderException::class)
	override fun readLine(): String {
		if (!canRead()) eof("more text")
		val s = line.substring(index)
		index = line.length
		return s
	}

	@Throws(ReaderException::class)
	override fun endLine() {
		if (canRead()) expected("end of line")
		index = line.length
	}

	@Throws(ReaderException::class)
	override fun readNumber(): String? {
		var c = peek()
		val start = index
		if (c == '-' || c in '0'..'9') {
			do {
				skip()
				if (!canRead()) break
				c = peek()
			} while (c in '0'..'9')
			return line.substring(start, index)
		}
		expected("a number")
		return null // UNREACHABLE CODE
	}

	/**
	 * tries to read given literal
	 *
	 * @param literal
	 * @return
	 */
	override fun tryReadLiteral(literal: String): Boolean {
		if (line.regionMatches(index, literal, 0, literal.length)) {
			skip(literal.length)
			tryNext() // TODO this feels incorrect
			return true
		}
		return false
	}

	override fun tryRead(s: String): Boolean {
		if (line.regionMatches(index, s, 0, s.length)) {
			skip(s.length)
			return true
		}
		return false
	}
}