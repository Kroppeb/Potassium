/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package kroppeb.server.command.arguments;

import kroppeb.server.command.reader.Reader;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.Collection;
import java.util.Collections;

public abstract class Selector {
	public static Selector read(Reader reader) {
		return null;
	}
	
	abstract public Collection<Entity> getEntities(ServerWorld world, Vec3d pos, Entity executor);
	
	class Self extends Selector{
		@Override
		public Collection<Entity> getEntities(ServerWorld world, Vec3d pos, Entity executor) {
			return Collections.singleton(executor);
		}
	}
	class SelfFiltered extends Selector{
		
		@Override
		public Collection<Entity> getEntities(ServerWorld world, Vec3d pos, Entity executor) {
			return null;
		}
	}
	class Complex extends Selector{
		
		@Override
		public Collection<Entity> getEntities(ServerWorld world, Vec3d pos, Entity executor) {
			return null;
		}
	}
}
