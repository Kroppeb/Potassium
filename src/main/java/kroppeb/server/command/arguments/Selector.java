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
