/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package kroppeb.server.command.arguments;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.sun.org.apache.xpath.internal.Arg;
import kroppeb.server.command.reader.Reader;
import kroppeb.server.command.reader.ReaderException;
import net.minecraft.command.arguments.NbtPathArgumentType;
import net.minecraft.nbt.Tag;
import net.minecraft.server.command.ServerCommandSource;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public abstract class NbtDataSource {
	public static NbtDataSource read(Reader reader) throws ReaderException {
		String type = reader.readLiteral();
		switch (type){
			case "value":
				return new Constant(ArgumentParser.readTag(reader));
			case "from":
				NbtDataContainer source = NbtDataContainer.read(reader);
				if(reader.tryNext())
					return new NbtDataPathSource(source, ArgumentParser.readPath(reader));
				return new NbtDataPathSource(source, null);
			default:
				throw new ReaderException("Unknown nbt data source: " + type);
		}
	}
	
	public abstract List<Tag> getData(ServerCommandSource source);
	
	public Tag getTag(ServerCommandSource source){
		Iterator<Tag> iterator = getData(source).iterator();
		Tag tag = iterator.next();
		if (iterator.hasNext()) {
			throw new RuntimeException("too much data"); // TODO decent error;
		} else {
			return tag;
		}
	}
	
	
	static class Constant extends  NbtDataSource{
		final List<Tag> singleton;
		final Tag data;
		
		Constant(Tag data) {
			this.data = data;
			this.singleton = Collections.singletonList(data);
		}
		
		@Override
		public Tag getTag(ServerCommandSource source) {
			return data;
		}
		
		@Override
		public List<Tag> getData(ServerCommandSource source) {
			return singleton;
		}
	}
	
	public static class NbtDataPathSource extends NbtDataSource{
		final NbtDataContainer container;
		public final NbtPathArgumentType.NbtPath path;
		
		public NbtDataPathSource(NbtDataContainer container, NbtPathArgumentType.NbtPath path) {
			this.container = container;
			this.path = path;
		}
		
		@Override
		public List<Tag> getData(ServerCommandSource source) {
			Tag tag = container.getTag(source);
			
			if(path == null)
				return Collections.singletonList(tag);
			
			try {
				return path.get(tag);
			} catch (CommandSyntaxException e) {
				throw new RuntimeException(e); // TODO decent error
			}
		}
		
		@Override
		public Tag getTag(ServerCommandSource source) {
			if(path == null)
				return container.getTag(source);
			return super.getTag(source);
		}
	}
}
