/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package kroppeb.server.command.commands;

import com.google.common.collect.Iterables;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import kroppeb.server.command.Command;
import kroppeb.server.command.InvocationError;
import kroppeb.server.command.arguments.ArgumentParser;
import kroppeb.server.command.arguments.NbtDataContainer;
import kroppeb.server.command.arguments.NbtDataSource;
import kroppeb.server.command.reader.Reader;
import kroppeb.server.command.reader.ReaderException;
import net.minecraft.command.arguments.NbtPathArgumentType.NbtPath;
import net.minecraft.nbt.*;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.MathHelper;

import java.util.List;

abstract public class DataCommand implements Command {
	final NbtDataContainer target;
	
	protected DataCommand(NbtDataContainer target) {
		this.target = target;
	}
	
	public static DataCommand read(Reader reader) throws ReaderException {
		String type = reader.readLiteral();
		switch(type){
			case "get":
				NbtDataContainer target = NbtDataContainer.read(reader);
				if(reader.tryNext()){
					NbtPath path = ArgumentParser.readPath(reader);
					if(reader.tryNext()){
						return new Get(target, path, reader.readSimpleDouble());
					}
					return new Get(target, path, null);
				}
				return new Get(target, null, null);
			case "merge":
				target = NbtDataContainer.read(reader);
				reader.moveNext();
				CompoundTag tag = ArgumentParser.readCompoundTag(reader);
				return new Merge(target, tag);
			case "modify":
				target = NbtDataContainer.read(reader);
				reader.moveNext();
				NbtPath path = ArgumentParser.readPath(reader);
				reader.moveNext();
				String mode = reader.readLiteral();
				switch (mode){
					case "append":
						return new Modify.Append(target, path, NbtDataSource.read(reader));
					case "insert":
						int index = reader.readInt();
						reader.moveNext();
						return new Modify.Insert(target, path, NbtDataSource.read(reader), index);
					case "merge":
						return new Modify.Merge(target, path, NbtDataSource.read(reader));
					case "prepend":
						return new Modify.Prepend(target, path, NbtDataSource.read(reader));
					case "set":
						return new Modify.Set(target, path, NbtDataSource.read(reader));
					default:
						throw new ReaderException("Unknown nbt operation: " + mode);
				}
			case "remove":
				target = NbtDataContainer.read(reader);
				reader.moveNext();
				path = ArgumentParser.readPath(reader);
				return new Remove(target, path);
			default:
				throw new ReaderException("Unknown subcommand: " + type);
		}
	}
	
	static class Get extends DataCommand {
		final NbtDataSource.NbtDataPathSource dataSource;
		final Double scale;
		
		Get(NbtDataContainer target, NbtPath path, Double scale) {
			super(target);
			this.dataSource = new NbtDataSource.NbtDataPathSource(target, path);
			this.scale = scale;
		}
		
		
		@Override
		public int execute(ServerCommandSource source) throws InvocationError {
			if (dataSource.path == null) {
				target.getTag(source);
				return 1;
			}
			
			Tag tag = dataSource.getTag(source);
			if (scale == null) {
				int m;
				if (tag instanceof AbstractNumberTag) {
					m = MathHelper.floor(((AbstractNumberTag) tag).getDouble());
				} else if (tag instanceof AbstractListTag) {
					//noinspection rawtypes
					m = ((AbstractListTag) tag).size();
				} else if (tag instanceof CompoundTag) {
					m = ((CompoundTag) tag).getSize();
				} else {
					if (!(tag instanceof StringTag)) {
						throw new InvocationError();
					}
					
					m = tag.asString().length();
				}
				return m;
			} else {
				if (tag instanceof AbstractNumberTag) {
					return MathHelper.floor(((AbstractNumberTag) tag).getDouble() * scale);
				}
				throw new InvocationError();
			}
		}
	}
	
	static class Merge extends DataCommand {
		final CompoundTag tag;
		
		Merge(NbtDataContainer target, CompoundTag tag) {
			super(target);
			this.tag = tag;
		}
		
		
		@Override
		public int execute(ServerCommandSource source) throws InvocationError {
			CompoundTag data = target.getTag(source);
			CompoundTag result = data.copy().copyFrom(tag);
			if (data.equals(result)) {
				throw new InvocationError();
			}
			target.setTag(source, result);
			return 1;
		}
	}
	
	static class Remove extends DataCommand {
		final NbtPath path;
		
		
		Remove(NbtDataContainer target, NbtPath path) {
			super(target);
			this.path = path;
		}
		
		
		@Override
		public int execute(ServerCommandSource source) throws InvocationError {
			CompoundTag data = target.getTag(source);
			int i = path.remove(data);
			
			if (i == 0) {
				throw new InvocationError();
			}
			target.setTag(source, data);
			return i;
		}
	}
	
	abstract static class Modify extends DataCommand {
		final NbtPath path;
		final NbtDataSource source;
		
		Modify(NbtDataContainer target, NbtPath path, NbtDataSource source) {
			super(target);
			this.path = path;
			this.source = source;
		}
		
		static int insert(int integer, CompoundTag sourceTag, NbtPath path, List<Tag> tags) throws InvocationError {
			// I was too lazy to copy
			try {
				int i = net.minecraft.server.command.DataCommand.executeInsert(integer, sourceTag, path, tags);
				if (i == 0) {
					throw new InvocationError();
				}
				return i;
				
			} catch (CommandSyntaxException e) {
				throw new InvocationError();
			}
		}
		
		static class Append extends Modify {
			Append(NbtDataContainer target, NbtPath path, NbtDataSource source) {
				super(target, path, source);
			}
			
			@Override
			public int execute(ServerCommandSource source) throws InvocationError {
				CompoundTag tag = target.getTag(source);
				int i = insert(-1,tag, path, this.source.getData(source)); // shouldn't be 0 if error is throw.
				target.setTag(source, tag);
				return i;
			}
		}
		
		static class Prepend extends Modify {
			Prepend(NbtDataContainer target, NbtPath path, NbtDataSource source) {
				super(target, path, source);
			}
			
			@Override
			public int execute(ServerCommandSource source) throws InvocationError {
				CompoundTag tag = target.getTag(source);
				int i = insert(0,tag, path, this.source.getData(source)); // shouldn't be 0 if error is throw.
				target.setTag(source, tag);
				return i;
			}
		}
		
		static class Insert extends Modify{
			final int index;
			
			Insert(NbtDataContainer target, NbtPath path, NbtDataSource source, int index) {
				super(target, path, source);
				this.index = index;
			}
			
			@Override
			public int execute(ServerCommandSource source) throws InvocationError {
				CompoundTag tag = target.getTag(source);
				int i = insert(index,tag, path, this.source.getData(source)); // shouldn't be 0 if error is throw.
				target.setTag(source, tag);
				return i;
			}
		}
		
		static class Set extends Modify{
			
			
			Set(NbtDataContainer target, NbtPath path, NbtDataSource source) {
				super(target, path, source);
			}
			
			@Override
			public int execute(ServerCommandSource source) throws InvocationError {
				CompoundTag tag = target.getTag(source);
				Tag last = Iterables.getLast(this.source.getData(source));
				try {
					int i = path.put(tag,last::copy);
					target.setTag(source, tag);
					return i;
				} catch (CommandSyntaxException e) {
					throw new InvocationError();
				}
				
			}
		}
		
		static class Merge extends Modify{
			
			Merge(NbtDataContainer target, NbtPath path, NbtDataSource source) {
				super(target, path, source);
			}
			
			@Override
			public int execute(ServerCommandSource source) throws InvocationError {
				List<Tag> tags;
				CompoundTag result = target.getTag(source);
				try {
					tags = path.getOrInit(result, CompoundTag::new);
				} catch (CommandSyntaxException e) {
					throw new InvocationError();
				}
				List<Tag> list = this.source.getData(source);
				
				
				int i = 0;
				
				CompoundTag compoundTag2;
				CompoundTag compoundTag3;
				for (Tag tag : tags) {
					if (!(tag instanceof CompoundTag)) {
						throw new InvocationError();
					}
					
					compoundTag2 = (CompoundTag) tag;
					compoundTag3 = compoundTag2.copy();
					
					for (Tag tag2 : list) {
						if (!(tag2 instanceof CompoundTag)) {
							throw new InvocationError();
						}
						
						compoundTag2.copyFrom((CompoundTag) tag2);
					}
					
					i += compoundTag3.equals(compoundTag2) ? 0 : 1;
				}
				if(i == 0)
					throw new InvocationError();
				target.setTag(source, result);
				return i;
			}
		}
	}
}
