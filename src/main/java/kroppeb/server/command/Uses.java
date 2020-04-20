/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package kroppeb.server.command;

import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.*;
/*
public abstract class Uses {
	public abstract int load(MethodVisitor mv);
	
	
	public static final Uses WORLD = new World();
	static class World extends Uses{
		@Override
		public int load(MethodVisitor mv) {
			mv.visitVarInsn(ALOAD, 1);
			mv.visitMethodInsn(INVOKEVIRTUAL, "net/minecraft/server/command/ServerCommandSource", "getWorld", "()Lnet/minecraft/server/world/ServerWorld;", false);
			return 1;
		}
	}
	
	public static final Uses POS = new Pos();
	static class Pos extends Uses{
		@Override
		public int load(MethodVisitor mv) {
			mv.visitVarInsn(ALOAD, 1);
			mv.visitMethodInsn(INVOKEVIRTUAL, "net/minecraft/server/command/ServerCommandSource", "getPos", "()Lnet/minecraft/util/math/Vec3d;", false);
			return 1;
		}
		
	};
	
	public static class PreBuild<T extends Buildable>extends Uses{
		public final T item;
		public String name;
		public PreBuild(T item) {
			this.item = item;
		}
		
		@Override
		public int load(MethodVisitor mv) {
			mv.visitFieldInsn(GETSTATIC, "kroppeb/test/BatGrenades", item.getName(), "Lnet/minecraft/nbt/CompoundTag;");
			return 1;
		}
	}
	
	public static class PrimitiveConst extends Uses{
		public final Object item;
		
		public PrimitiveConst(Object item) {
			this.item = item;
		}
		
		@Override
		public int load(MethodVisitor mv) {
			
			return 1;
		}
	}
}
*/