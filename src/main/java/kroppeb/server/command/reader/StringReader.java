/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package kroppeb.server.command.reader;


import net.minecraft.command.arguments.PosArgument;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

import java.io.*;
import java.util.EnumSet;

public class StringReader implements Reader {
	String line;
	public int index = 0;
	
	public StringReader(String line) {
		this.line = line;
	}
	
	public StringReader() {
	}
	
	public void setLine(String s) {
		line = s;
		index = 0;
	}
	
	@Override
	public char peek() {
		return line.charAt(index);
	}
	
	@Override
	public void skip() {
		if (++index >= line.length()) {
			// idk;
		}
	}
	
	@Override
	public void skip(int n) {
		index += n;
		if (index >= line.length()) {
			// idk;
		}
	}
	
	@Override
	public boolean canRead() {
		return index < line.length();
	}
	
	@Override
	public char read() throws ReaderException {
		if (!canRead())
			eof("further");
		char c = peek();
		skip();
		return c;
	}
	
	
	private void eof(char c) throws ReaderException {
		eof("'" + c + "'");
	}
	
	private void eof(String s) throws ReaderException {
		throw new ReaderException(
				"Expected to read " + s + " but we are at the end of the file"
		);
	}
	
	/**
	 * skips whitespace
	 * else throws
	 */
	@Override
	public void moveNext() throws ReaderException {
		if (isWhiteSpace(peek())) {
			do {
				skip();
			} while (isWhiteSpace(peek()));
		} else
			expected("whitespace");
	}
	
	
	private void expected(String s) throws ReaderException {
		throw new ReaderException(
				"Expected to read " + s
		);
	}
	
	private boolean isWhiteSpace(char c) {
		return c == ' ' || c == '\t';
	}
	
	/**
	 * Read until "special" character
	 * Forbidden chars = any whitespace, EOL, '=' ':' '"' '\''
	 */
	@Override
	public String readWord() throws ReaderException {
		if (endWord(peek()))
			expected("word");
		int pos = index;
		do {
			skip();
		} while (!endWord(peek()));
		return line.substring(pos, index);
	}
	
	
	@Override
	public String readUntilWhitespace() throws ReaderException {
		int start = index;
		if (!isWhiteSpace()) {
			do {
				read();
			} while (canRead() && !isWhiteSpace());
			return line.substring(start, index);
		} else
			throw new ReaderException("Expected a literal");
	}
	
	boolean endWord(char c) {
		return isWhiteSpace(c) || c == '\n' || c == '\r' ||
				c == '=' || c == ':' || c == '"' || c == '\'';
	}
	
	@Override
	public void readChar(char c) throws ReaderException {
		if (read() != c)
			expected("'" + c + "'");
	}
	
	/**
	 * converts ints to double by adding `0.5D`
	 */
	@Override
	public double readDouble() throws ReaderException {
		return 0;
	}
	
	/**
	 * read quoted or unquoted string
	 *
	 * @return
	 */
	@Override
	public String readString() throws ReaderException {
		if (isQuotedStringStart())
			return readQuotedString();
		return readUnquotedString();
	}
	
	@Override
	public String readQuotedString() throws ReaderException {
		if (!isQuotedStringStart())
			expected("quote");
		int pos = index + 1;
		char start = read();
		while (read() != start) ;
		return line.substring(pos, index - 1);
	}
	
	@Override
	public String readUnquotedString() throws ReaderException {
		if (!isAllowedInUnquotedString())
			expected("unquoted string");
		int pos = index;
		while (isAllowedInUnquotedString())
			read();
		return line.substring(pos, index);
		
	}
	
	@Override
	public Identifier readIdentifier() throws ReaderException {
		if (!isAllowedInIdentifier())
			expected("identifier");
		int pos = index;
		
		do skip();
		while (isAllowedInIdentifier());
		
		String str = line.substring(pos, index);
		Identifier id = Identifier.tryParse(str);
		if (id == null)
			throw new ReaderException("Couldn't parse " + str + " as an identifier");
		return id;
	}
	
	@Override
	public String readLine() throws ReaderException {
		if (!canRead())
			eof("more text");
		String s = line.substring(index);
		index = line.length();
		return s;
	}
	
	
	@Override
	public void endLine() throws ReaderException {
		if (canRead())
			expected("end of line");
		index = line.length();
	}
	
	@Override
	public int readInt() throws ReaderException {
		char c = peek();
		int start = index;
		if(c == '-' || (c >= '0' && c <= '9')){
			do{
				skip();
				if(!canRead())
					break;
				c = peek();
			}while(c >= '0' && c <= '9');
			try {
				return Integer.parseInt(line.substring(start, index));
			}catch (NumberFormatException e){
				throw new ReaderException("invalid int: " + e.getMessage(), e);
			}
		}
		expected("an integer");
		return 0; // UNREACHABLE CODE
	}
	
	@Override
	public EnumSet<Direction.Axis> readSwizzle() {
		return null;
	}
	
	/**
	 * tries to read given literal
	 *
	 * @param literal
	 * @return
	 */
	@Override
	public boolean tryReadLiteral(String literal) {
		if(line.regionMatches(index,literal,0,literal.length())){
			skip(literal.length());
			tryNext();
			return true;
		}
		return false;
	}
}
