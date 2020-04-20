package kroppeb.server.command;


import kroppeb.server.command.reader.ReaderException;
import net.minecraft.command.arguments.PosArgument;


public interface Reader {
	char peek();
	
	void skip();
	
	void skip(int n);
	
	boolean canRead();
	
	default boolean tryRead(char c) {
		if (peek() == c) {
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
	
	@Deprecated
	void readChar(char c) throws ReaderException;
	
	/**
	 * converts ints to double by adding `0.5D`
	 */
	double readDouble() throws ReaderException;
	
	default boolean isQuotedStringStart() {
		char p = peek();
		return p == '"' || p == '\'';
	}
	
	/**
	 * read quoted or unquoted string
	 * @return
	 */
	String readString() throws ReaderException;
	
	String readQuotedString() throws ReaderException;
	
	String readUnquotedString() throws ReaderException;
	
	String readIdentifier() throws ReaderException;
	
	static boolean isAllowedInUnquotedString(final char c) {
		return c >= '0' && c <= '9'
				|| c >= 'A' && c <= 'Z'
				|| c >= 'a' && c <= 'z'
				|| c == '_' || c == '-'
				|| c == '.' || c == '+';
	}
	
	static boolean isAllowedInIdentifier(final char c) {
		return c >= '0' && c <= '9'
				|| c >= 'a' && c <= 'z'
				|| c == '_' || c == '-'
				|| c == '.';
	}
	
	/**
	 * Not pure, calls `moveNext` if possible
	 * @return true if there is data to read
	 * @throws ReaderException if next char isn't whitespace
	 */
	default boolean hasNext() throws ReaderException {
		if(canRead()) {
			moveNext();
			return canRead();
		}
		return false;
	}
	
	@Deprecated
	PosArgument readPos() throws ReaderException;
	
	String readLine() throws ReaderException;
	void skipLine();
	void endLine() throws ReaderException;
}
