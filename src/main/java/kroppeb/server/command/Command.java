package kroppeb.server.command;

import net.minecraft.server.command.ServerCommandSource;
import org.objectweb.asm.MethodVisitor;

public interface Command {
	public abstract void execute(ServerCommandSource source);
}
