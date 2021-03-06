/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package kroppeb.server.command

import kroppeb.server.command.reader.ReaderException
import net.minecraft.command.argument.ItemPredicateArgumentType
import net.minecraft.command.argument.ItemStackArgument
import net.minecraft.command.argument.PosArgument
import net.minecraft.scoreboard.ScoreboardObjective
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

// region asm loading
fun loadBoolean(clinit: MethodVisitor, value: Boolean) =
	clinit.visitInsn(if (value) Opcodes.ICONST_1 else Opcodes.ICONST_0)


fun loadInt(clinit: MethodVisitor, value: Int) = when (value) {
	in -1..5 -> clinit.visitInsn(Opcodes.ICONST_0 + value)
	value.toByte().toInt() -> clinit.visitIntInsn(Opcodes.BIPUSH, value)
	value.toShort().toInt() -> clinit.visitIntInsn(Opcodes.SIPUSH, value)
	else -> clinit.visitLdcInsn(value)
}


fun loadDouble(clinit: MethodVisitor, value: Double) = when (value) {
	0.0 -> clinit.visitInsn(Opcodes.DCONST_0)
	1.0 -> clinit.visitInsn(Opcodes.DCONST_1)
	else -> clinit.visitLdcInsn(value)
}


fun loadFloat(clinit: MethodVisitor, value: Float) = when (value) {
	0f -> clinit.visitInsn(Opcodes.FCONST_0)
	1f -> clinit.visitInsn(Opcodes.FCONST_1)
	2f -> clinit.visitInsn(Opcodes.FCONST_2)
	else -> clinit.visitLdcInsn(value)
}

fun loadLong(clinit: MethodVisitor, value: Float) = when (value) {
	0f -> clinit.visitInsn(Opcodes.LCONST_0)
	1f -> clinit.visitInsn(Opcodes.LCONST_1)
	else -> clinit.visitLdcInsn(value)
}
// endregion

@Deprecated("todo")
fun <T> todo(msg: String): T = throw ReaderException("Todo: $msg")

@Deprecated("todo")
fun getServer() = todo<MinecraftServer>("get server")

fun PosArgument.toLoadedBlockPosition(source: ServerCommandSource): BlockPos? {
	val blockPos = this.toAbsoluteBlockPos(source)
	val world = source.world
	return when {
		!world.isChunkLoaded(blockPos) -> null // TODO: should I throw?
		!ServerWorld.isInBuildLimit(blockPos) -> null // TODO: should i throw?
		else -> blockPos
	}
}

fun ItemPredicateArgumentType.ItemPredicate.toItemStackArgument(): ItemStackArgument {
	return ItemStackArgument(this.item, this.compound)
}

fun ServerCommandSource.getScoreboard(name: String): ScoreboardObjective {
	val scoreboard: net.minecraft.scoreboard.Scoreboard = minecraftServer.scoreboard
	val scoreboardObjective = scoreboard.getNullableObjective(name)
	return scoreboardObjective ?: throw InvocationError()
}
