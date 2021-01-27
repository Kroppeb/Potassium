/*
 * Copyright (c) 2021 Kroppeb
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
				mv.visitFieldInsn(
					Opcodes.GETSTATIC,
					this@FunctionNamespaceBuilder.name,
					cd.name,
					"Lkroppeb/server/command/Command;"
				)
				mv.visitVarInsn(Opcodes.ALOAD, 0)
				mv.visitMethodInsn(
					Opcodes.INVOKEINTERFACE,
					"kroppeb/server/command/commands/Command",
					"execute",
					"(Lnet/minecraft/server/command/ServerCommandSource;)V",
					true
				)
			}
			loadInt(mv, commands.size)
			mv.visitInsn(Opcodes.IRETURN)

			// debug info
			mv.visitLabel(end)
			run {
				mv.visitLocalVariable(
					"source",
					"Lnet/minecraft/server/command/ServerCommandSource;",
					null,
					start,
					end,
					0
				)
			}
			mv.visitMaxs(2, 1)
			mv.visitEnd()
		}

	}

	private fun buildStoreCommands() {
		var current = writer.visitMethod(Opcodes.ACC_STATIC, "__clinit_BuildStoreCommand_0", "()V", null, null)
		current.visitCode()
		var start = Label()
		current.visitLabel(start)
		current.visitFieldInsn(
			Opcodes.GETSTATIC,
			"kroppeb/server/command/CommandLoader",
			"functions",
			"Ljava/util/Map;"
		)
		current.visitVarInsn(Opcodes.ASTORE, 0)

		var estSize = 0
		var functionIndex = 0

		for (function in functions) {
			if (estSize > 15_000) {

				functionIndex++
				estSize = 0

				current.visitMethodInsn(
					Opcodes.INVOKESTATIC,
					"kroppeb/potassium/generated/GeneratedFunctions",
					"__clinit_BuildStoreCommand_$functionIndex",
					"()V",
					false
				)
				val end = Label()
				current.visitLabel(end)
				current.visitInsn(Opcodes.RETURN)
				current.visitLocalVariable(
					"functions",
					"Ljava/util/Map;",
					"Ljava/util/Map<Ljava/lang/String;Lkroppeb/server/command/Command;>;",
					start,
					end,
					0
				)
				current.visitMaxs(3, 1)
				current.visitEnd()


				current = writer.visitMethod(
					Opcodes.ACC_STATIC,
					"__clinit_BuildStoreCommand_$functionIndex",
					"()V",
					null,
					null
				)
				current.visitCode()
				start = Label()
				current.visitLabel(start)
				current.visitFieldInsn(
					Opcodes.GETSTATIC,
					"kroppeb/server/command/CommandLoader",
					"functions",
					"Ljava/util/Map;"
				)
				current.visitVarInsn(Opcodes.ASTORE, 0)

			}
			current.visitVarInsn(Opcodes.ALOAD, 0)
			current.visitLdcInsn(function.name)
			current.visitInvokeDynamicInsn(
				"execute",
				"()Lkroppeb/server/command/Command;",
				Handle(
					Opcodes.H_INVOKESTATIC,
					"java/lang/invoke/LambdaMetafactory",
					"metafactory",
					"(Ljava/lang/invoke/MethodHandles\$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",
					false
				),
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
				)
			)
			current.visitMethodInsn(
				Opcodes.INVOKEINTERFACE,
				"java/util/Map",
				"put",
				"(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
				true
			)
			current.visitInsn(Opcodes.POP)
			estSize += 64 // too big but i'm too lazy to check
		}

		val end = Label()
		current.visitLabel(end)
		current.visitInsn(Opcodes.RETURN)
		current.visitLocalVariable(
			"functions",
			"Ljava/util/Map;",
			"Ljava/util/Map<Ljava/lang/String;Lkroppeb/server/command/Command;>;",
			start,
			end,
			0
		)
		current.visitMaxs(3, 1)
		current.visitEnd()
	}

	private fun buildStore(fields: ArrayList<CommandData>) {
		var current = writer.visitMethod(Opcodes.ACC_STATIC, "__clinit_BuildStore_0", "()V", null, null)
		current.visitCode()
		var start = Label()
		current.visitLabel(start)

		current.visitFieldInsn(
			Opcodes.GETSTATIC,
			"kroppeb/server/command/CommandLoader",
			"commands",
			"[Lkroppeb/server/command/Command;"
		)
		current.visitVarInsn(Opcodes.ASTORE, 0)

		var estSize = 0
		var functionIndex = 0

		for (j in fields.indices) {
			if (estSize > 15_000) {

				functionIndex++
				estSize = 0


				current.visitMethodInsn(
					Opcodes.INVOKESTATIC,
					"kroppeb/potassium/generated/GeneratedFunctions",
					"__clinit_BuildStore_$functionIndex",
					"()V",
					false
				)
				val end = Label()
				current.visitLabel(end)
				current.visitInsn(Opcodes.RETURN)
				current.visitLocalVariable("commands", "[Lkroppeb/server/command/Command;", null, start, end, 0)
				current.visitMaxs(3, 1)
				current.visitEnd()


				current =
					writer.visitMethod(Opcodes.ACC_STATIC, "__clinit_BuildStore_$functionIndex", "()V", null, null)
				current.visitCode()
				start = Label()
				current.visitLabel(start)

				current.visitFieldInsn(
					Opcodes.GETSTATIC,
					"kroppeb/server/command/CommandLoader",
					"commands",
					"[Lkroppeb/server/command/Command;"
				)
				current.visitVarInsn(Opcodes.ASTORE, 0)

			}

			current.visitVarInsn(Opcodes.ALOAD, 0)
			loadInt(current, j)
			current.visitInsn(Opcodes.AALOAD)
			current.visitFieldInsn(Opcodes.PUTSTATIC, fullName, fields[j].name, "Lkroppeb/server/command/Command;")

			estSize += 32 // idk
		}

		val end = Label()
		current.visitLabel(end)
		current.visitInsn(Opcodes.RETURN)
		current.visitLocalVariable("commands", "[Lkroppeb/server/command/Command;", null, start, end, 0)
		current.visitMaxs(3, 1)
		current.visitEnd()
	}

	private fun buildClinit(fields: ArrayList<CommandData>) {
		buildStoreCommands()
		buildStore(fields)
		val clinit = writer.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null)
		clinit.visitCode()
		clinit.visitMethodInsn(
			Opcodes.INVOKESTATIC,
			"kroppeb/potassium/generated/GeneratedFunctions",
			"__clinit_BuildStoreCommand_0",
			"()V",
			false
		)

		clinit.visitMethodInsn(Opcodes.INVOKESTATIC, "kroppeb/server/command/CommandLoader", "loadAll", "()V", false)
		clinit.visitMethodInsn(
			Opcodes.INVOKESTATIC,
			"kroppeb/potassium/generated/GeneratedFunctions",
			"__clinit_BuildStore_0",
			"()V",
			false
		)
		clinit.visitInsn(Opcodes.RETURN)

		clinit.visitMaxs(0, 0)
		clinit.visitEnd()
		writer.visitEnd()
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
		buildClinit(fields)
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
