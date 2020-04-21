/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package kroppeb.server.command.arguments;

import kroppeb.server.command.reader.Reader;
import kroppeb.server.command.reader.ReaderException;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.Collection;
import java.util.Collections;

public abstract class Selector {
	public static Selector read(Reader reader) throws ReaderException {
		reader.readChar('@');
		switch (reader.read()) {
			case 's':
				if (reader.tryRead('[')) {
					while (reader.read() !=']');
					return new SelfFiltered();
				} else {
					return SELF;
				}
			default:
				if (reader.tryRead('[')) {
					while (reader.read() !=']');
				}
				return new Complex();
		}
	}
	
	abstract public Collection<Entity> getEntities(ServerWorld world, Vec3d pos, Entity executor);
	
	abstract public static class SingleSelector extends  Selector{
		@Override
		public Collection<Entity> getEntities(ServerWorld world, Vec3d pos, Entity executor) {
			return Collections.singleton(getEntity(world, pos, executor));
		}
		
		public abstract Entity getEntity(ServerWorld world, Vec3d pos, Entity executor);
	}
	
	static Self SELF = new Self();
	
	static class Self extends SingleSelector {
		
		private Self() {}
		
		@Override
		public Entity getEntity(ServerWorld world, Vec3d pos, Entity executor) {
			return executor;
		}
	}
	
	static class SelfFiltered extends SingleSelector {
		
		@Override
		public Entity getEntity(ServerWorld world, Vec3d pos, Entity executor) {
			return null;
		}
	}
	
	static class Complex extends Selector {
		
		@Override
		public Collection<Entity> getEntities(ServerWorld world, Vec3d pos, Entity executor) {
			return null;
		}
	}
}
