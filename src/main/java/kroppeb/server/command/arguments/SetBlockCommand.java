/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package kroppeb.server.command.arguments;

import kroppeb.server.command.Command;
import kroppeb.server.command.reader.Reader;
import kroppeb.server.command.reader.ReaderException;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.arguments.BlockStateArgument;
import net.minecraft.command.arguments.PosArgument;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Clearable;
import net.minecraft.util.math.BlockPos;

public class SetBlockCommand implements Command {
	final PosArgument pos;
	final BlockStateArgument block;
	final boolean destroy;
	final boolean keep;
	
	public SetBlockCommand(PosArgument pos, BlockStateArgument block, boolean destroy, boolean keep) {
		this.pos = pos;
		this.block = block;
		this.destroy = destroy;
		this.keep = keep;
	}
	
	public static SetBlockCommand read(Reader reader) throws ReaderException {
		PosArgument pos = ArgumentParser.readPos(reader);
		reader.moveNext();
		BlockStateArgument block = ArgumentParser.readBlock(reader);
		if(reader.hasNext()){
			String mode = reader.readLiteral();
			switch (mode){
				case "replace": return new SetBlockCommand(pos, block, false, false);
				case "keep": return new SetBlockCommand(pos, block, false, true);
				case "destroy": return new SetBlockCommand(pos, block, true, false);
				default: throw new ReaderException("Unexpected mode value: " + mode);
			}
		}
		return new SetBlockCommand(pos, block, false, false);
	}
	
	@Override
	public int execute(ServerCommandSource source) {
		BlockPos blockPos = pos.toAbsoluteBlockPos(source);
		ServerWorld world = source.getWorld();
		if(!world.isChunkLoaded(blockPos))
			return 0; // TODO throw here?
		CachedBlockPosition sourceBlock = new CachedBlockPosition(world, blockPos, true);
		if(keep && sourceBlock.getBlockState().isAir())
			return 0; // TODO throw here?
		if(destroy){
			world.breakBlock(blockPos, true);
			if((block.getBlockState().isAir() && sourceBlock.getBlockState().isAir()) ||
					block.setBlockState(world, blockPos, 2)){
				world.updateNeighbors(blockPos, block.getBlockState().getBlock());
				return 1;
			}else{
				return 0; // TODO throw here?
			}
		}else{
			Clearable.clear(sourceBlock.getBlockEntity());
			if(block.setBlockState(world, blockPos, 2)){
				world.updateNeighbors(blockPos, block.getBlockState().getBlock());
				return 1;
			}else{
				return 0; // TODO throw here?
			}
		}
	}
}
