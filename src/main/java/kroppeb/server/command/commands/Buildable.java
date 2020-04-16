package kroppeb.server.command.commands;

import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.GETSTATIC;

public interface Buildable {
	String getDescriptor();
	
	int buildTo(MethodVisitor clinit);
	
	void setIndex(int size);
	
	String getName();
	
	default void loadTo(MethodVisitor mv, String owner){
		mv.visitFieldInsn(GETSTATIC, owner, getName(), getDescriptor());
	}
}
