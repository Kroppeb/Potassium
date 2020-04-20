package kroppeb.server.command;


public class CommandData {
	public final Command cmd;
	public final String name;
	
	public CommandData(Command cmd, int index) {
		this.cmd = cmd;
		name = "command" + "$" + index;
	}
}
