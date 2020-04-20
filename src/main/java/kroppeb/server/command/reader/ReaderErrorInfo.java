package kroppeb.server.command.reader;

public class ReaderErrorInfo {
	final String source;
	final int line;
	final int pos;
	final String text;
	
	public ReaderErrorInfo(String source, int line, int pos, String text) {
		this.source = source;
		this.line = line;
		this.pos = pos;
		this.text = text;
	}
}
