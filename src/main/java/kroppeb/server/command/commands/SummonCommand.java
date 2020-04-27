/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package kroppeb.server.command.commands;

import kroppeb.server.command.*;
import kroppeb.server.command.arguments.ArgumentParser;
import kroppeb.server.command.arguments.Resource;
import kroppeb.server.command.reader.Reader;
import kroppeb.server.command.reader.ReaderException;
import net.minecraft.command.arguments.PosArgument;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public abstract class SummonCommand implements Command {
	public static class SummonDefault extends SummonCommand {
		final PosArgument pos;
		final CompoundTag tag;
		final boolean initialize;
		
		public SummonDefault(PosArgument pos, CompoundTag tag, boolean initialize) {
			this.pos = pos;
			this.tag = tag;
			this.initialize = initialize;
		}
		
		@Override
		public int execute(ServerCommandSource source) throws InvocationError {
			ServerWorld world = source.getWorld();
			Vec3d pos = this.pos.toAbsolutePos(source);
			if (!World.method_25953(new BlockPos(pos))) throw new IllegalArgumentException(); // TODO better errors
			Entity entity2 = EntityType.loadEntityWithPassengers(this.tag, world, (entityx) -> {
				entityx.refreshPositionAndAngles(pos.x, pos.y, pos.z, entityx.yaw, entityx.pitch);
				return world.tryLoadEntity(entityx) ? entityx : null;
			});
			if (entity2 == null) {
				throw new InvocationError();
			} else {
				if (initialize && entity2 instanceof MobEntity) {
					((MobEntity) entity2).initialize(world, world.getLocalDifficulty(entity2
							.getBlockPos()), SpawnType.COMMAND, (EntityData) null, (CompoundTag) null);
				}
			}
			return 1;
		}
	}
	
	public static class SummonLightning extends SummonCommand {
		final PosArgument pos;
		
		public SummonLightning(PosArgument pos) {
			this.pos = pos;
		}
		
		@Override
		public int execute(ServerCommandSource source) throws InvocationError {
			ServerWorld world = source.getWorld();
			Vec3d pos = this.pos.toAbsolutePos(source);
			if (!World.method_25953(new BlockPos(pos)))
				throw new InvocationError();
			LightningEntity lightningEntity = new LightningEntity(world, pos.x, pos.y, pos.z, false);
			world.addLightning(lightningEntity);
			return 1;
		}
	}
	
	public static SummonCommand of(Resource type, PosArgument pos, CompoundTag tag) {
		if (!(type.namespace == null || type.namespace.equals("minecraft")) && type.path.length == 1 && type.path[0]
				.equals("lightning_bolt")) {
			boolean init = tag == null;
			if (init) tag = new CompoundTag();
			tag.putString("id", type.toString());
			return new SummonDefault(pos, tag, init);
		} else {
			return new SummonLightning(pos);
		}
	}
	
	
	static public SummonCommand read(Reader reader) throws ReaderException {
		Resource entityType = Resource.read(reader);
		PosArgument pos = null;
		CompoundTag tag = null;
		if (reader.hasNext()) {
			pos = ArgumentParser.readPos(reader);
			if (reader.hasNext()) tag = ArgumentParser.readCompoundTag(reader);
		}
		return SummonCommand.of(entityType, pos, tag);
	}
}
