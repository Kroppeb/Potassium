package kroppeb.server.command;

import kroppeb.server.command.commands.Buildable;
import net.minecraft.command.arguments.CoordinateArgument;
import net.minecraft.command.arguments.DefaultPosArgument;
import net.minecraft.command.arguments.LookingPosArgument;
import net.minecraft.command.arguments.PosArgument;
import net.minecraft.nbt.*;
import org.objectweb.asm.MethodVisitor;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.Map;

import static kroppeb.server.command.Util.*;
import static kroppeb.server.command.Util.loadInt;
import static org.objectweb.asm.Opcodes.*;


public class BuildablePos implements Buildable {
	public final PosArgument item;
	public String name;
	
	
	public BuildablePos(PosArgument item) {
		this.item = item;
	}
	
	
	@Override
	public String getDescriptor() {
		return "Lnet/minecraft/command/arguments/PosArgument;";
	}
	
	@Override
	public int buildTo(MethodVisitor clinit) {
		if (item instanceof LookingPosArgument)
			return buildLookingTo(clinit, (LookingPosArgument) item);
		else if (item instanceof DefaultPosArgument)
			return buildDefaultTo(clinit, (DefaultPosArgument) item);
		else
			throw new IllegalArgumentException();
	}
	
	private int buildDefaultTo(MethodVisitor clinit, DefaultPosArgument item) {
		clinit.visitTypeInsn(NEW, "net/minecraft/command/arguments/DefaultPosArgument");
		clinit.visitInsn(DUP);
		// 2
		
		clinit.visitTypeInsn(NEW, "net/minecraft/command/arguments/CoordinateArgument");
		clinit.visitInsn(DUP);
		loadBoolean(clinit, item.isXRelative());
		loadDouble(clinit, item.x.value); // 2 + 4
		clinit.visitMethodInsn(INVOKESPECIAL, "net/minecraft/command/arguments/CoordinateArgument", "<init>", "(ZD)V", false);
		// 3
		clinit.visitTypeInsn(NEW, "net/minecraft/command/arguments/CoordinateArgument");
		clinit.visitInsn(DUP);
		loadBoolean(clinit, item.isXRelative());
		loadDouble(clinit, item.x.value); // 3 + 4
		clinit.visitMethodInsn(INVOKESPECIAL, "net/minecraft/command/arguments/CoordinateArgument", "<init>", "(ZD)V", false);
		// 4
		clinit.visitTypeInsn(NEW, "net/minecraft/command/arguments/CoordinateArgument");
		clinit.visitInsn(DUP);
		loadBoolean(clinit, item.isXRelative());
		loadDouble(clinit, item.x.value); // 4 + 4
		clinit.visitMethodInsn(INVOKESPECIAL, "net/minecraft/command/arguments/CoordinateArgument", "<init>", "(ZD)V", false);
		// 5
		clinit.visitMethodInsn(INVOKESPECIAL, "net/minecraft/command/arguments/DefaultPosArgument", "<init>", "(Lnet/minecraft/command/arguments/CoordinateArgument;Lnet/minecraft/command/arguments/CoordinateArgument;Lnet/minecraft/command/arguments/CoordinateArgument;)V", false);
		// 1
		
		return 8;
	}
	
	private int buildLookingTo(MethodVisitor clinit, LookingPosArgument item) {
		clinit.visitTypeInsn(NEW, "net/minecraft/command/arguments/LookingPosArgument");
		clinit.visitInsn(DUP);
		loadDouble(clinit, item.x);
		loadDouble(clinit, item.y);
		loadDouble(clinit, item.z);
		clinit.visitMethodInsn(INVOKESPECIAL, "net/minecraft/command/arguments/LookingPosArgument", "<init>", "(DDD)V", false);
		return 5;
	}
	
	@Override
	public void setIndex(int index) {
		name = "pos$" + index;
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	public static PosArgument SELF = new DefaultPosArgument(
			new CoordinateArgument(true, 0),
			new CoordinateArgument(true, 0),
			new CoordinateArgument(true, 0)
	);
}
