package kroppeb.server.command.commands;

import kroppeb.server.command.*;
import kroppeb.server.command.Arguments.Resource;
import net.minecraft.command.arguments.PosArgument;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public abstract class Summon implements Command {
	public static class SummonDefault extends Summon{
		final PosArgument pos;
		final CompoundTag tag;
		final boolean initialize;
		
		public SummonDefault(PosArgument pos, CompoundTag tag, boolean initialize) {
			this.pos = pos;
			this.tag = tag;
			this.initialize = initialize;
		}
		
		@Override
		public int execute(ServerCommandSource source) {
			ServerWorld world = source.getWorld();
			Vec3d pos = this.pos.toAbsolutePos(source);
			if(!World.method_25953(new BlockPos(pos)))
				throw new IllegalArgumentException(); // TODO better errors
			Entity entity2 = EntityType.loadEntityWithPassengers(this.tag, world, (entityx) -> {
				entityx.refreshPositionAndAngles(pos.x, pos.y, pos.z, entityx.yaw, entityx.pitch);
				return world.tryLoadEntity(entityx) ? entityx : null;
			});
			if (entity2 == null) {
				throw new RuntimeException();//FAILED_EXCEPTION.create();
			} else {
				if (initialize && entity2 instanceof MobEntity) {
					((MobEntity) entity2).initialize(world, world.getLocalDifficulty(entity2.getBlockPos()), SpawnType.COMMAND, (EntityData) null, (CompoundTag) null);
				}
			}
			return 1;
		}
	}
	public static class SummonLightning extends Summon {
		final PosArgument pos;
		
		public SummonLightning(PosArgument pos) {
			this.pos = pos;
		}
		
		@Override
		public int execute(ServerCommandSource source) {
			ServerWorld world = source.getWorld();
			Vec3d pos = this.pos.toAbsolutePos(source);
			if(!World.method_25953(new BlockPos(pos)))
				throw new IllegalArgumentException(); // TODO better errors
			LightningEntity lightningEntity = new LightningEntity(world, pos.x, pos.y, pos.z, false);
			world.addLightning(lightningEntity);
			return 1;
		}
	}
	
	public static Summon of(Resource type, PosArgument pos, CompoundTag tag) {
		if(!(type.namespace == null || type.namespace.equals("minecraft")) && type.path.length == 1 && type.path[0].equals("lightning_bolt")) {
			boolean init = tag == null;
			if(init)
				tag = new CompoundTag();
			tag.putString("id", type.toString());
			return new SummonDefault(pos, tag,init );
		} else {
			return new SummonLightning(pos);
		}
	}
	
	
	static public Summon read(Reader reader){
		Resource entityType = Resource.read(reader);
		PosArgument pos = null;
		CompoundTag tag = null;
		if(reader.hasNext()){
			pos = reader.readPos();
			if(reader.hasNext())
				tag = BuildableTag.readCompoundTag(reader);
		}
		return Summon.of(entityType, pos, tag);
	}
}
