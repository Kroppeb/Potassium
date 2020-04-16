package kroppeb.server.command.commands;

import kroppeb.server.command.*;
import kroppeb.server.command.Arguments.Resource;
import net.minecraft.command.arguments.PosArgument;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.objectweb.asm.MethodVisitor;

import static kroppeb.server.command.Util.loadBoolean;
import static org.objectweb.asm.Opcodes.*;

final public class Summon extends Command {
	final Resource type;
	final PosArgument pos;
	final CompoundTag tag;
	final boolean isLigthning;
	final boolean initialize;
	
	public Summon(Resource type, PosArgument pos, CompoundTag tag) {
		this.type = type;
		this.pos = pos;
		isLigthning = (type.namespace == null || type.namespace.equals("minecraft")) && type.path.length == 1 && type.path[0].equals("lightning_bolt");
		if(!isLigthning) {
			initialize = tag == null;
			if(initialize)
				this.tag = new CompoundTag();
			else
				this.tag = tag;
			
			this.tag.putString("id", type.toString());
		} else {
			this.tag = null;
			this.initialize = false;
		}
	}
	
	
	private Buildable bTag;
	private Buildable bPos;
	@Override
	public void addFields(FunctionNamespaceBuilder.FunctionBuilder fb){
		if(!isLigthning)
			fb.addField(bTag = new BuildableTag(tag));
		if(pos != null)
			fb.addField(bPos = new BuildablePos(pos));
	}
	
	
	@Override
	public int buildTo(MethodVisitor mv, String className) {
		if(isLigthning){
			if(bPos == null)
				mv.visitInsn(ACONST_NULL);
			else
				bPos.loadTo(mv, className);
			mv.visitMethodInsn(INVOKESTATIC, "kroppeb/server/command/commands/Summon", "summonLightning", "(Lnet/minecraft/server/command;Lnet/minecraft/command/arguments/PosArgument;)V", false);
			return 1;
		}
		else{
			if(bPos == null)
				mv.visitInsn(ACONST_NULL);
			else
				bPos.loadTo(mv, className);
			if(bTag == null)
				mv.visitInsn(ACONST_NULL);
			else
				bTag.loadTo(mv, className);
			loadBoolean(mv, initialize);
			mv.visitMethodInsn(INVOKESTATIC, "kroppeb/server/command/commands/Summon", "summon", "(Lnet/minecraft/server/command;Lnet/minecraft/command/arguments/PosArgument;Lnet/minecraft/nbt/CompoundTag;Z)V", false);
			return 3;
		}
	}
	
	
	static public Summon read(StringReader reader){
		Resource entityType = Resource.read(reader);
		PosArgument pos = null;
		CompoundTag tag = null;
		if(reader.hasNext()){
			pos = reader.readPos();
			if(reader.hasNext())
				tag = BuildableTag.readCompoundTag(reader);
		}
		return new Summon(entityType, pos, tag);
	}
	
	/**
	 * Used to summon lighting
	 * @param source
	 * @param posArgument
	 */
	static public void summonLightning(ServerCommandSource source, PosArgument posArgument) {
		ServerWorld world = source.getWorld();
		Vec3d pos = posArgument.toAbsolutePos(source);
		LightningEntity lightningEntity = new LightningEntity(world, pos.x, pos.y, pos.z, false);
		world.addLightning(lightningEntity);
	}
	
	/**
	 * Should not be used to summon lighting
	 * @param source
	 * @param posArgument
	 * @param nbt
	 * @param initialize
	 */
	static public void summon(ServerCommandSource source, PosArgument posArgument, CompoundTag nbt, boolean initialize) {
		ServerWorld world = source.getWorld();
		Vec3d pos = posArgument.toAbsolutePos(source);
		Entity entity2 = EntityType.loadEntityWithPassengers(nbt, world, (entityx) -> {
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
	}
}
