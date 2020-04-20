package kroppeb.server.command;

import net.minecraft.server.command.ServerCommandSource;

public interface Command {
	public abstract int execute(ServerCommandSource source);
}
