/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package kroppeb.server.command.commands;

import com.mojang.brigadier.ResultConsumer;
import kroppeb.server.command.Command;
import kroppeb.server.command.Parser;
import kroppeb.server.command.Util;
import kroppeb.server.command.arguments.ArgumentParser;
import kroppeb.server.command.arguments.Selector;
import kroppeb.server.command.reader.Reader;
import kroppeb.server.command.reader.ReaderException;
import net.minecraft.command.arguments.EntityAnchorArgumentType;
import net.minecraft.command.arguments.PosArgument;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.dimension.DimensionType;
import org.apache.commons.lang3.NotImplementedException;

import java.util.EnumSet;

public class Execute implements Command {
	
	
	private CommandOutput output;
	private Vec3d pos;
	private Vec2f rot;
	private ServerWorld world;
	private int level;
	private String simpleName;
	private Text name;
	private MinecraftServer server;
	private Entity entity;
	private boolean silent;
	private ResultConsumer<ServerCommandSource> resultConsumer;
	private EntityAnchorArgumentType.EntityAnchor entityAnchor;
	
	Converter first;
	
	
	@Override
	public int execute(ServerCommandSource source) {
		
		output = source.output;
		pos = source.getPosition();
		rot = source.getRotation();
		world = source.getWorld();
		level = source.level;
		simpleName = source.getName();
		name = source.getDisplayName();
		server = source.getMinecraftServer();
		entity = source.getEntity();
		silent = source.silent;
		resultConsumer = source.resultConsumer;
		entityAnchor = source.getEntityAnchor();
		
		return first.apply();
	}
	
	private ServerCommandSource source(){
		return new ServerCommandSource(
				output,
				pos,
				rot,
				world,
				level,
				simpleName,
				name,
				server,
				entity);
	}
	
	public static Execute read(Reader reader) throws ReaderException{
		Execute res = new Execute();
		res.readConverter(reader);
		return res;
	}
	
	private Converter readConverter(Reader reader) throws ReaderException{
		String subCommand = reader.readLiteral();
		switch (subCommand){
			case "align":
				EnumSet<Direction.Axis> swizzle = reader.readSwizzle();
				reader.moveNext();
				Converter next = readConverter(reader);
				return new Align(next, swizzle);
			case "anchored":
				String type = reader.readLiteral();
				switch (type) {
					case "eyes": return new Anchored(readConverter(reader), EntityAnchorArgumentType.EntityAnchor.EYES);
					case "feet": return new Anchored(readConverter(reader), EntityAnchorArgumentType.EntityAnchor.FEET);
					default: throw new ReaderException("Expected <eyes/feet> but got: " + type);
				}
			case "as":
				Selector s = Selector.read(reader);
				reader.moveNext();
				next = readConverter(reader);
				return new As(next, s);
			case "at":
				s = Selector.read(reader);
				reader.moveNext();
				next = readConverter(reader);
				return new At(next, s);
			case "in":
				Identifier id = reader.readIdentifier();
				DimensionType dim = DimensionType.byId(id);
				if(dim == null)
					throw new ReaderException("Expected valid dimension, got: " + id);
					
				return new In(readConverter(reader), dim);
				
			// case "facing": TODO
			case "positioned":
				if(reader.tryReadLiteral("as")){
					s = Selector.read(reader);
					reader.moveNext();
					return new PositionedAs(readConverter(reader), s);
				}else{
					PosArgument pos = ArgumentParser.readPos(reader);
					reader.moveNext();
					return new Positioned(readConverter(reader), pos);
				}
				
				
			case "rotated":
				if(reader.tryReadLiteral("as")){
					s = Selector.read(reader);
					reader.moveNext();
					return new RotatedAs(readConverter(reader), s);
				}else{
					throw new NotImplementedException("todo");
					/*
					Vec2f
					reader.moveNext();
					return new Rotated(readConverter(reader), pos);*/
				}
			// case "store": TODO
			// case "if": TODO
			// case "unless": TODO
			case "run": return new Run(Parser.readFunction(reader));
			default:
				throw new ReaderException("Unknown subcommand: " + subCommand);
		}
	}
	
	abstract public static class Converter {
		abstract int apply();
	}
	
	public class Align extends Converter{
		final Converter next;
		final EnumSet<Direction.Axis> axes;
		Vec3d posCache;
		
		public Align(Converter next, EnumSet<Direction.Axis> axes) {
			this.next = next;
			this.axes = axes;
		}
		
		@Override
		int apply() {
			posCache = pos;
			pos = pos.floorAlongAxes(axes);
			int r = next.apply();
			pos = posCache;
			return r;
		}
	}
	public class Anchored extends Converter{
		final Converter next;
		final EntityAnchorArgumentType.EntityAnchor ea;
		EntityAnchorArgumentType.EntityAnchor entityAnchorCache;
		
		public Anchored(Converter next, EntityAnchorArgumentType.EntityAnchor entityAnchor) {
			this.next = next;
			this.ea = entityAnchor;
		}
		
		@Override
		int apply() {
			entityAnchorCache = entityAnchor;
			entityAnchor = ea;
			int r = next.apply();
			entityAnchor = entityAnchorCache;
			return r;
		}
	}
	public class As extends Converter{
		final Converter next;
		final Selector selector;
		
		
		public As(Converter next, Selector selector) {
			this.next = next;
			this.selector = selector;
		}
		
		@Override
		int apply() {
			Entity entityCache = entity;
			int r = 0;
			for (Entity selectorEntity : selector.getEntities(world, pos, entity)) {
				entity = selectorEntity;
				r += next.apply();
			}
			entity = entityCache;
			return r;
		}
	}
	public class At extends Converter{
		final Converter next;
		final Selector selector;
		
		
		public At(Converter next, Selector selector) {
			this.next = next;
			this.selector = selector;
		}
		
		@Override
		int apply() {
			Vec3d posCache = pos;
			Vec2f rotCache = rot;
			int r = 0;
			for (Entity selectorEntity : selector.getEntities(world, pos, entity)) {
				pos = selectorEntity.getPos();
				rot = selectorEntity.getRotationClient();
				r += next.apply();
			}
			pos = posCache;
			return r;
		}
	}
	// facing
	public class In extends Converter{
		final Converter next;
		final DimensionType dimension;
		
		public In(Converter next, DimensionType dimension) {
			this.next = next;
			this.dimension = dimension;
		}
		
		@Override
		int apply() {
			ServerWorld worldCache = world;
			world = server.getWorld(dimension);
			int r = next.apply();
			world = worldCache;
			return r;
		}
	}
	public class Positioned extends Converter{
		final Converter next;
		final PosArgument innerPos;
		
		public Positioned(Converter next, PosArgument pos) {
			this.next = next;
			this.innerPos = pos;
		}
		
		@Override
		int apply() {
			Vec3d posCache = pos;
			pos = innerPos.toAbsolutePos(source());
			int r = next.apply();
			pos = posCache;
			return r;
		}
	}
	public class PositionedAs extends Converter{
		final Converter next;
		final Selector selector;
		
		public PositionedAs(Converter next, Selector selector) {
			this.next = next;
			this.selector = selector;
		}
		
		@Override
		int apply() {
			Vec3d posCache = pos;
			int r = 0;
			for (Entity selectorEntity : selector.getEntities(world, pos, entity)) {
				pos = selectorEntity.getPos();
				r += next.apply();
			}
			pos = posCache;
			return r;
		}
	}
	public class Rotated extends Converter{
		final Converter next;
		final Vec2f innerRot;
		
		public Rotated(Converter next, Vec2f rot) {
			this.next = next;
			this.innerRot = rot;
		}
		
		@Override
		int apply() {
			Vec2f rotCache = rot;
			rot = innerRot;
			int r = next.apply();
			rot = rotCache;
			return r;
		}
	}
	public class RotatedAs extends Converter{
		final Converter next;
		final Selector selector;
		
		public RotatedAs(Converter next, Selector selector) {
			this.next = next;
			this.selector = selector;
		}
		
		@Override
		int apply() {
			Vec2f rotCache = rot;
			int r = 0;
			for (Entity selectorEntity : selector.getEntities(world, pos, entity)) {
				rot = selectorEntity.getRotationClient();
				r += next.apply();
			}
			rot = rotCache;
			return r;
		}
	}
	// store
	// if
	public class Run extends Converter {
		final Command cmd;
		
		public Run(Command cmd) {
			this.cmd = cmd;
		}
		
		@Override
		int apply() {
			return cmd.execute(source());
		}
		
	}
	
}
