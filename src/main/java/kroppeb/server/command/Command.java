package kroppeb.server.command;

import net.minecraft.server.command.ServerCommandSource;
import org.objectweb.asm.MethodVisitor;

public interface Command {
	public abstract int execute(ServerCommandSource source);
}
