/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package kroppeb.server.command

import org.objectweb.asm.*
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque

class FunctionNamespaceBuilder(private val writer: ClassVisitor) {
	private val name = "GeneratedFunctions"
	private val fullName: String
	private val descriptor: String

	// we'll be adding to these concurrently adding
	private val functions: MutableCollection<FunctionBuilder> = ConcurrentLinkedDeque()
	fun addFunction(name: String): FunctionBuilder {
		val fb = FunctionBuilder(name)
		functions.add(fb)
		return fb
	}

	/**
	 * NOT THREAD SAFE
	 */
	inner class FunctionBuilder(val name: String) {
		val commands: MutableList<CommandData> = ArrayList()
		fun addCommand(cmd: Command) {
			val cd = CommandData(cmd, name + "$$$" + commands.size)
			commands.add(cd)
		}

		fun build() {
			val mv = writer.visitMethod(Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC, name, "(Lnet/minecraft/server/command/ServerCommandSource;)I", null, null)
			mv.visitCode()

			// debug info
			val start = Label()
			val end = Label()
			mv.visitLabel(start)
			for (cd in commands) {
				mv.visitFieldInsn(Opcodes.GETSTATIC, this@FunctionNamespaceBuilder.name, cd.name, "Lkroppeb/server/command/Command;")
				mv.visitVarInsn(Opcodes.ALOAD, 0)
				mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "kroppeb/server/command/commands/Command", "execute", "(Lnet/minecraft/server/command/ServerCommandSource;)V", true)
			}
			Util.loadInt(mv, commands.size)
			mv.visitInsn(Opcodes.IRETURN)

			// debug info
			mv.visitLabel(end)
			run { mv.visitLocalVariable("source", "Lnet/minecraft/server/command/ServerCommandSource;", null, start, end, 0) }
			mv.visitMaxs(2, 1)
			mv.visitEnd()
		}

	}

	fun build() {
		val fields = ArrayList<CommandData>()
		for (fb in functions) {
			fields.addAll(fb.commands)
		}

		// fields
		val i = 0
		for (cd in fields) writer
				.visitField(Opcodes.ACC_STATIC, cd.name, "Lkroppeb/server/command/Command;", null, null)
				.visitEnd()
		for (fb in functions) {
			fb.build()
		}
		val clinit = writer.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null)
		clinit.visitCode()
		val start = Label()
		clinit.visitLabel(start)
		clinit.visitFieldInsn(Opcodes.GETSTATIC, "kroppeb/server/command/CommandLoader", "functions", "Ljava/util/Map;")
		clinit.visitVarInsn(Opcodes.ASTORE, 0)
		for (function in functions) {
			clinit.visitVarInsn(Opcodes.ALOAD, 0)
			clinit.visitLdcInsn(function.name)
			clinit.visitInvokeDynamicInsn(
					"execute",
					"()Lkroppeb/server/command/Command;",
					Handle(Opcodes.H_INVOKESTATIC,
							"java/lang/invoke/LambdaMetafactory",
							"metafactory",
							"(Ljava/lang/invoke/MethodHandles\$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",
							false),
					Type.getType(
							"(Lnet/minecraft/server/command/ServerCommandSource;)I"
					),
					Handle(
							Opcodes.H_INVOKESTATIC,
							fullName,
							function.name,
							"(Lnet/minecraft/server/command/ServerCommandSource;)I",
							false
					),
					Type.getType(
							"(Lnet/minecraft/server/command/ServerCommandSource;)I"
					))
			clinit.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true)
			clinit.visitInsn(Opcodes.POP)
		}
		clinit.visitMethodInsn(Opcodes.INVOKESTATIC, "kroppeb/server/command/CommandLoader", "loadAll", "()V", false)
		val part2 = Label()
		clinit.visitLabel(part2)
		clinit.visitFieldInsn(Opcodes.GETSTATIC, "kroppeb/server/command/CommandLoader", "commands", "[Lkroppeb/server/command/Command;")
		clinit.visitVarInsn(Opcodes.ASTORE, 0)
		for (j in fields.indices) {
			clinit.visitVarInsn(Opcodes.ALOAD, 0)
			Util.loadInt(clinit, j)
			clinit.visitInsn(Opcodes.AALOAD)
			clinit.visitFieldInsn(Opcodes.PUTSTATIC, fullName, fields[j].name, "Lkroppeb/server/command/Command;")
		}
		val end = Label()
		clinit.visitLabel(end)
		clinit.visitInsn(Opcodes.RETURN)
		clinit.visitLocalVariable("functions", "Ljava/util/Map;", "Ljava/util/Map<Ljava/lang/String;Lkroppeb/server/command/Command;>;", start, part2, 0)
		clinit.visitLocalVariable("commands", "[Lkroppeb/server/command/Command;", null, part2, end, 0)
		clinit.visitMaxs(3, 1)
		clinit.visitEnd()
		writer.visitEnd()
	}

	init {
		fullName = "kroppeb/potassium/generated/" + name
		descriptor = "L$fullName;"
		writer.visit(Opcodes.V1_8, Opcodes.ACC_FINAL or Opcodes.ACC_PUBLIC or Opcodes.ACC_SUPER, fullName, null, "java/lang/Object", null)
		// I'm assuming this is to make accessors or something?
		// This allows the nested class to access private variables?
		// Don't know if we actually need it
		// TODO test what happens if we remove it
		writer.visitInnerClass("java/lang/invoke/MethodHandles\$Lookup", "java/lang/invoke/MethodHandles", "Lookup", Opcodes.ACC_PUBLIC or Opcodes.ACC_FINAL or Opcodes.ACC_STATIC)
	}
}