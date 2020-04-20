/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package kroppeb.server.command;

import kroppeb.server.command.arguments.Resource;
import kroppeb.server.command.commands.Summon;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import org.objectweb.asm.*;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import static kroppeb.server.command.Util.loadInt;
import static org.objectweb.asm.Opcodes.*;

public class FunctionNamespaceBuilder {
	final private ClassVisitor writer;
	final private String name;
	final private String fullName;
	final private String descriptor;
	final private List<CommandData> fields = new ArrayList<>();
	final private List<FunctionBuilder> functions = new ArrayList<>();
	
	public FunctionNamespaceBuilder(ClassVisitor writer, String name) {
		this.writer = writer;
		this.name = name;
		this.fullName = "kroppeb/potassium/generated/" + name;
		this.descriptor = "L" + fullName + ";";
		writer.visit(V1_8,  ACC_FINAL | ACC_PUBLIC | ACC_SUPER, this.name, null, "java/lang/Object", null);
		// I'm assuming this is to make accessors or something?
		// This allows the nested class to access private variables?
		// Don't know if we actually need it
		// TODO test what happens if we remove it
		writer.visitInnerClass("java/lang/invoke/MethodHandles$Lookup", "java/lang/invoke/MethodHandles", "Lookup", ACC_PUBLIC | ACC_FINAL | ACC_STATIC);
	}
	
	public FunctionBuilder addFunction(String name) {
		FunctionBuilder fb = new FunctionBuilder(name);
		functions.add(fb);
		return fb;
	}
	
	public class FunctionBuilder {
		final String name;
		final List<CommandData> commands = new ArrayList<>();
		
		public FunctionBuilder(String name) {
			this.name = name;
		}
		
		public void addCommand(Command cmd) {
			CommandData cd = new CommandData(cmd, fields.size());
			commands.add(cd);
			fields.add(cd);
		}
		
		public void build(){
			MethodVisitor mv = writer.visitMethod(ACC_PUBLIC | ACC_STATIC, name, "(Lnet/minecraft/server/command/ServerCommandSource;)V", null, null);
			mv.visitCode();
			
			// debug info
			Label start = new Label();
			Label end = new Label();
			
			mv.visitLabel(start);
			
			for (CommandData cd : commands) {
				mv.visitFieldInsn(GETSTATIC, FunctionNamespaceBuilder.this.name, cd.name, "Lkroppeb/server/command/Command;");
				mv.visitVarInsn(ALOAD, 0);
				mv.visitMethodInsn(INVOKEINTERFACE, "kroppeb/server/command/commands/Command", "execute", "(Lnet/minecraft/server/command/ServerCommandSource;)V", true);
			}
			mv.visitInsn(RETURN);
			
			// debug info
			mv.visitLabel(end);
			{
				mv.visitLocalVariable("source", "Lnet/minecraft/server/command/ServerCommandSource;", null, start, end, 0);
			}
			
			mv.visitMaxs(2, 1);
			mv.visitEnd();
		}
	}
	
	
	
	public void build() {
		// fields
		int i = 0;
		for (CommandData cd : fields)
			writer
					.visitField(ACC_STATIC, cd.name, "Lkroppeb/server/command/Command;", null, null)
					.visitEnd();
				
		for(FunctionBuilder fb : functions){
			fb.build();
		}
		
		
		MethodVisitor clinit = writer.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
		clinit.visitCode();
		Label start = new Label();
		clinit.visitLabel(start);
		
		clinit.visitFieldInsn(GETSTATIC, "kroppeb/server/command/CommandLoader", "functions", "Ljava/util/Map;");
		clinit.visitVarInsn(ASTORE, 0);
		
		for (FunctionBuilder function : functions) {
			clinit.visitVarInsn(ALOAD, 0);
			clinit.visitLdcInsn(function.name);
			clinit.visitInvokeDynamicInsn(
					"execute",
					"()Lkroppeb/server/command/Command;",
					new Handle(Opcodes.H_INVOKESTATIC,
							"java/lang/invoke/LambdaMetafactory",
							"metafactory",
							"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",
							false),
					Type.getType(
							"(Lnet/minecraft/server/command/ServerCommandSource;)I"
					),
					new Handle(
							Opcodes.H_INVOKESTATIC,
							"kroppeb/test/Generated",
							function.name,
							"(Lnet/minecraft/server/command/ServerCommandSource;)I",
							false
					),
					Type.getType(
							"(Lnet/minecraft/server/command/ServerCommandSource;)I"
					));
			clinit.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
			clinit.visitInsn(POP);
		}
		
		
		
		
		clinit.visitMethodInsn(INVOKESTATIC, "kroppeb/server/command/CommandLoader", "loadAll", "()V", false);
		
		Label part2 = new Label();
		clinit.visitLabel(part2);
		
		clinit.visitFieldInsn(GETSTATIC, "kroppeb/server/command/CommandLoader", "commands", "[Lkroppeb/server/command/Command;");
		clinit.visitVarInsn(ASTORE, 0);
		
		for(int j = 0; j < this.fields.size(); j++){
			clinit.visitVarInsn(ALOAD, 0);
			loadInt(clinit, j);
			clinit.visitInsn(AALOAD);
			clinit.visitFieldInsn(PUTSTATIC, "kroppeb/test/Generated", "command$" + j , "Lkroppeb/server/command/Command;");
		}
		
		Label end = new Label();
		clinit.visitLabel(end);
		clinit.visitInsn(RETURN);
		clinit.visitLocalVariable("functions", "Ljava/util/Map;", null, start, part2, 0);
		clinit.visitLocalVariable("commands", "[Lkroppeb/server/command/Command;", null, part2, end, 0);
		
		clinit.visitMaxs(3, 1);
		clinit.visitEnd();
		
		writer.visitEnd();
	}
}
