/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package kroppeb.server.command.reader;


import net.minecraft.util.Identifier;


public interface Reader {
	char peek();
	
	void skip();
	
	void skip(int n);
	
	boolean canRead();
	
	default boolean tryRead(char c) {
		if (canRead() && peek() == c) {
			skip();
			return true;
		}
		return false;
	}
	
	char read() throws ReaderException;
	
	/**
	 * skips whitespace
	 * else throws
	 */
	void moveNext() throws ReaderException;
	
	/**
	 * Read until "special" character
	 * Forbidden chars = any whitespace, '=' ':' '"' '\''
	 */
	String readWord() throws ReaderException;
	
	/**
	 * readUntilWhiteSpace and asserts that we are at the end or will skip ws and we are not at the end
	 */
	default String readLiteral() throws ReaderException {
		String res = readUntilWhitespace();
		if (canRead()) {
			moveNext();
			if(!canRead()){
				throw new ReaderException("trailing whitespace");
			}
		}
		return res;
	}
	
	/**
	 * reads until whitespace or EOL
	 *
	 * @throws ReaderException empty string
	 */
	String readUntilWhitespace() throws ReaderException;
	
	void readChar(char c) throws ReaderException;
	
	String readNumber() throws ReaderException;
	
	default int readInt() throws ReaderException {
		try {
			return Integer.parseInt(readNumber());
		} catch (NumberFormatException e) {
			throw new ReaderException("invalid int: " + e.getMessage(), e);
		}
	}
	
	default double readDouble() throws ReaderException {
		if (tryRead('.')) {
			String number = "." + readNumber();
			try {
				return Double.parseDouble(number);
			} catch (NumberFormatException e) {
				throw new ReaderException("Not a valid double: " + number);
			}
		} else {
			String pre = readNumber();
			if (tryRead('.')) {
				String number = pre + "." + readNumber();
				try {
					return Double.parseDouble(number);
				} catch (NumberFormatException e) {
					throw new ReaderException("Not a valid double: " + number);
				}
			}else{
				try {
					return Double.parseDouble(pre);
				} catch (NumberFormatException e) {
					throw new ReaderException("Not a valid double: " + pre);
				}
			}
		}
	}
	
	default double readSimpleDoubleIntOffset() throws ReaderException {
		if (tryRead('.')) {
			String number = "." + readNumber();
			try {
				return Double.parseDouble(number);
			} catch (NumberFormatException e) {
				throw new ReaderException("Not a valid double: " + number);
			}
		} else {
			String pre = readNumber();
			if (tryRead('.')) {
				String number = pre + "." + readNumber();
				try {
					return Double.parseDouble(number);
				} catch (NumberFormatException e) {
					throw new ReaderException("Not a valid double: " + number);
				}
			}else{
				try {
					return Double.parseDouble(pre) + .5D;
				} catch (NumberFormatException e) {
					throw new ReaderException("Not a valid double: " + pre);
				}
			}
		}
	}
	
	default boolean isQuotedStringStart() {
		char p = peek();
		return p == '"' || p == '\'';
	}
	
	/**
	 * read quoted or unquoted string
	 *
	 * @return
	 */
	String readString() throws ReaderException;
	
	String readQuotedString() throws ReaderException;
	
	String readUnquotedString() throws ReaderException;
	
	Identifier readIdentifier() throws ReaderException;
	
	default boolean isAllowedInUnquotedString() {
		return canRead() && isAllowedInUnquotedString(peek());
	}
	
	// make private?
	@Deprecated
	static boolean isAllowedInUnquotedString(final char c) {
		return c >= '0' && c <= '9'
				|| c >= 'A' && c <= 'Z'
				|| c >= 'a' && c <= 'z'
				|| c == '_' || c == '-'
				|| c == '.' || c == '+';
	}
	
	default boolean isAllowedInIdentifier() {
		return canRead() && isAllowedInIdentifier(peek());
	}
	
	// make this private?
	@Deprecated
	static boolean isAllowedInIdentifier(final char c) {
		return c >= '0' && c <= '9'
				|| c >= 'a' && c <= 'z'
				|| c == '_' || c == '-'
				|| c == '.' || c == '/'
				|| c == ':';
	}
	
	/**
	 * Not pure, calls `moveNext` if possible
	 *
	 * @return true if there is data to read
	 * @throws ReaderException if next char isn't whitespace
	 */
	default boolean hasNext() throws ReaderException {
		if (canRead()) {
			moveNext();
			return canRead();
		}
		return false;
	}
	
	String readLine() throws ReaderException;
	
	void endLine() throws ReaderException;
	
	default boolean isWhiteSpace() {
		if(!canRead())
			return false;
		char c = peek();
		return c == ' ';
	}
	
	/**
	 * move next if possible
	 *
	 * @throws ReaderException if the reader can't read;
	 */
	default void next() throws ReaderException {
		if (isWhiteSpace())
			moveNext();
	}
	
	/**
	 * move next if possible
	 *
	 * @return if there is new data to read.
	 */
	default boolean tryNext() {
		if (isWhiteSpace()) {
			try {
				moveNext();
			} catch (ReaderException e) {
				throw new RuntimeException("uh oh");
				// return false; // should not happen i think
			}
		}
		return canRead();
	}
	
	/**
	 * tries to read given literal
	 */
	boolean tryReadLiteral(String literal);
	
	boolean tryRead(String s);
	
	String readNamedPath() throws ReaderException;
	
	@SuppressWarnings("DeprecatedIsStillUsed")
	@Deprecated
	static boolean isAllowedInNamedPath(char c) {
		return c != ' ' && c != '"' && c != '[' && c != ']' && c != '.' && c != '{' && c != '}';
	}
	
	default boolean isAllowedInNamedPath(){
		return canRead() && Reader.isAllowedInNamedPath(peek());
	}
}
