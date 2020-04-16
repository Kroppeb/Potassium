package kroppeb.server.command;

import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.*;

public class Util {
	static public void loadBoolean(MethodVisitor clinit, boolean value) {
		clinit.visitInsn(value?ICONST_1:ICONST_0);
	}
	
	static public void loadInt(MethodVisitor clinit, int value) {
		if (value >= -1 && value <= 5)
			clinit.visitInsn(ICONST_0 + value);
		else if (value == (byte) value)
			clinit.visitIntInsn(BIPUSH, value);
		else if (value == (short) value)
			clinit.visitIntInsn(SIPUSH, value);
		else
			clinit.visitLdcInsn(value);
	}
	
	static public void loadDouble(MethodVisitor clinit, double value) {
		if (value == 0)
			clinit.visitInsn(DCONST_0);
		else if (value == 1)
			clinit.visitInsn(DCONST_1);
		else
			clinit.visitLdcInsn(value);
	}
	
	static public void loadFloat(MethodVisitor clinit, float value) {
		if (value == 0)
			clinit.visitInsn(FCONST_0);
		else if (value == 1)
			clinit.visitInsn(FCONST_1);
		else if (value == 2)
			clinit.visitInsn(FCONST_2);
		else
			clinit.visitLdcInsn(value);
	}
	
	static public void loadLong(MethodVisitor clinit, float value) {
		if (value == 0)
			clinit.visitInsn(LCONST_0);
		else if (value == 1)
			clinit.visitInsn(LCONST_1);
		else
			clinit.visitLdcInsn(value);
	}
}
