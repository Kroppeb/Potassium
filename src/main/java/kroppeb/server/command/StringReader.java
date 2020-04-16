package kroppeb.server.command;


import net.minecraft.command.arguments.PosArgument;

/**
 * Every `readXXXX` method *should* act like `skipWS()` has been called.
 */
public interface StringReader {
	char peek();
	
	void skip();
	
	String peek(int n);
	
	void skip(int n);
	
	boolean canRead();
	
	default boolean tryRead(char c) {
		if (peek() == c) {
			skip();
			return true;
		}
		return false;
	}
	
	void read(char c);
	
	void moveNext();
	
	String readWord();
	
	void readChar(char c);
	
	double readDouble();
	
	default boolean isQuotedStringStart() {
		char p = peek();
		return p == '"' || p == '\'';
	}
	
	
	String readString();
	
	String readQuotedString();
	
	String readUnquotedString();
	
	String readIdentifier();
	
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
	
	default boolean hasNext(){
		moveNext();
		return canRead();
	}
	
	PosArgument readPos();
}
