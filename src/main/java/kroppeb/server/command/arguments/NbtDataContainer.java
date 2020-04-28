/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package kroppeb.server.command.arguments;

import kroppeb.server.command.reader.Reader;
import kroppeb.server.command.reader.ReaderException;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.command.arguments.PosArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public interface NbtDataContainer {
	
	static NbtDataContainer read(Reader reader) throws ReaderException {
		String type = reader.readLiteral();
		switch (type){
			case "block":
				return new Block(ArgumentParser.readPos(reader));
			case "entity":
				return new Entity(Selector.SingleSelector.read(reader));
			case "storage":
				return new Storage(reader.readIdentifier());
			default:
				throw new ReaderException("Unknown bt storage: " + type);
		}
	}
	
	CompoundTag getTag(ServerCommandSource source);
	
	void setTag(ServerCommandSource source, CompoundTag tag);
	
	class Block implements NbtDataContainer {
		final PosArgument pos;
		
		public Block(PosArgument pos) {
			this.pos = pos;
		}
		
		@Override
		public CompoundTag getTag(ServerCommandSource source) {
			BlockPos blockPos = pos.toAbsoluteBlockPos(source);
			ServerWorld world = source.getWorld();
			if(!world.isChunkLoaded(blockPos)){
				throw new RuntimeException("not loaded"); // TODO decent error
			}
			
			CompoundTag tag = new CompoundTag();
			world.getBlockEntity(blockPos).toTag(tag); // TODO check null
			return tag;
		}
		
		@Override
		public void setTag(ServerCommandSource source, CompoundTag tag) {
			BlockPos blockPos = pos.toAbsoluteBlockPos(source);
			ServerWorld world = source.getWorld();
			if(!world.isChunkLoaded(blockPos)){
				throw new RuntimeException("not loaded"); // TODO decent error
			}
			BlockEntity blockEntity = world.getBlockEntity(blockPos);
			blockEntity.fromTag(blockEntity.getCachedState(), tag); //TODO check null
		}
	}
	
	class Entity implements NbtDataContainer {
		final Selector.SingleSelector selector;
		
		Entity(Selector.SingleSelector selector) {
			this.selector = selector;
		}
		
		@Override
		public CompoundTag getTag(ServerCommandSource source) {
			CompoundTag tag = new CompoundTag();
			selector.getEntity(source).toTag(tag); // TODO check null?
			return tag;
		}
		
		@Override
		public void setTag(ServerCommandSource source, CompoundTag tag) {
			selector.getEntity(source).fromTag(tag);
		}
	}
	
	static class Storage implements NbtDataContainer {
		final Identifier id;
		
		Storage(Identifier id) {
			this.id = id;
		}
		
		@Override
		public CompoundTag getTag(ServerCommandSource source) {
			return source.getMinecraftServer().getDataCommandStorage().get(id);
		}
		
		@Override
		public void setTag(ServerCommandSource source, CompoundTag tag) {
			source.getMinecraftServer().getDataCommandStorage().set(id, tag);
		}
	}
}
