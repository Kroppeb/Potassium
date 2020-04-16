package kroppeb.server.command;

import org.objectweb.asm.MethodVisitor;

public abstract class Command {
	public void addFields(FunctionNamespaceBuilder.FunctionBuilder fb){}
	
	public abstract int buildTo(MethodVisitor mv, String className);
}
