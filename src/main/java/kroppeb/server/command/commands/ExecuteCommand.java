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
import kroppeb.server.command.arguments.ArgumentParser;
import kroppeb.server.command.arguments.Score;
import kroppeb.server.command.arguments.ScoreComparator;
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
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;
import org.apache.commons.lang3.NotImplementedException;

import java.util.EnumSet;

public class ExecuteCommand implements Command {
	
	
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
		
		return first.call();
	}
	
	private ServerCommandSource source() {
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
	
	public static ExecuteCommand read(Reader reader) throws ReaderException {
		ExecuteCommand res = new ExecuteCommand();
		res.readConverter(reader);
		return res;
	}
	
	private Converter readConverter(Reader reader) throws ReaderException {
		String subCommand = reader.readLiteral();
		switch (subCommand) {
			case "align":
				EnumSet<Direction.Axis> swizzle = reader.readSwizzle();
				reader.moveNext();
				Converter next = readConverter(reader);
				return new Align(next, swizzle);
			case "anchored":
				String type = reader.readLiteral();
				switch (type) {
					case "eyes":
						return new Anchored(readConverter(reader), EntityAnchorArgumentType.EntityAnchor.EYES);
					case "feet":
						return new Anchored(readConverter(reader), EntityAnchorArgumentType.EntityAnchor.FEET);
					default:
						throw new ReaderException("Expected <eyes/feet> but got: " + type);
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
				if (dim == null)
					throw new ReaderException("Expected valid dimension, got: " + id);
				
				return new In(readConverter(reader), dim);
			
			case "facing":
				if (reader.tryReadLiteral("entity")) {
					s = Selector.read(reader);
					reader.moveNext();
					if (reader.tryReadLiteral("eyes"))
						return new FacingEntity(readConverter(reader), s, EntityAnchorArgumentType.EntityAnchor.EYES);
					if (reader.tryReadLiteral("feet"))
						return new FacingEntity(readConverter(reader), s, EntityAnchorArgumentType.EntityAnchor.FEET);
					throw new ReaderException("unknown facing anchor: " + reader.readLiteral());
				} else {
					PosArgument pos = ArgumentParser.readPos(reader);
					reader.moveNext();
					return new FacingPos(readConverter(reader), pos);
				}
			
			case "positioned":
				if (reader.tryReadLiteral("as")) {
					s = Selector.read(reader);
					reader.moveNext();
					return new PositionedAs(readConverter(reader), s);
				} else {
					PosArgument pos = ArgumentParser.readPos(reader);
					reader.moveNext();
					return new Positioned(readConverter(reader), pos);
				}
			
			
			case "rotated":
				if (reader.tryReadLiteral("as")) {
					s = Selector.read(reader);
					reader.moveNext();
					return new RotatedAs(readConverter(reader), s);
				} else {
					throw new NotImplementedException("todo");
					/*
					Vec2f
					reader.moveNext();
					return new Rotated(readConverter(reader), pos);*/
				}
				// case "store": TODO
			case "if": return readIf(reader);
				// case "unless": TODO
			case "run":
				return new Run(Parser.readFunction(reader));
			default:
				throw new ReaderException("Unknown subcommand: " + subCommand);
		}
	}
	
	Converter readIf(Reader reader) throws ReaderException {
		String type = reader.readLiteral();
		switch (type) {
			case "entity":
				Selector s = Selector.read(reader);
				reader.moveNext();
				return new IfEntity(readConverter(reader), s);
			case "score":
				Score score = Score.read(reader);
				reader.moveNext();
				ScoreComparator comparator = ScoreComparator.read(reader);
				reader.moveNext();
				return new IfScore(readConverter(reader), score, comparator);
			default:
				throw new ReaderException("Unknown if subcommand: " + type);
		}
	}
	
	abstract public static class Converter {
		abstract int call();
	}
	
	public class Align extends Converter {
		final Converter next;
		final EnumSet<Direction.Axis> axes;
		Vec3d posCache;
		
		public Align(Converter next, EnumSet<Direction.Axis> axes) {
			this.next = next;
			this.axes = axes;
		}
		
		@Override
		int call() {
			posCache = pos;
			pos = pos.floorAlongAxes(axes);
			int r = next.call();
			pos = posCache;
			return r;
		}
	}
	
	public class Anchored extends Converter {
		final Converter next;
		final EntityAnchorArgumentType.EntityAnchor ea;
		EntityAnchorArgumentType.EntityAnchor entityAnchorCache;
		
		public Anchored(Converter next, EntityAnchorArgumentType.EntityAnchor entityAnchor) {
			this.next = next;
			this.ea = entityAnchor;
		}
		
		@Override
		int call() {
			entityAnchorCache = entityAnchor;
			entityAnchor = ea;
			int r = next.call();
			entityAnchor = entityAnchorCache;
			return r;
		}
	}
	
	public class As extends Converter {
		final Converter next;
		final Selector selector;
		
		
		public As(Converter next, Selector selector) {
			this.next = next;
			this.selector = selector;
		}
		
		@Override
		int call() {
			Entity entityCache = entity;
			int r = 0;
			for (Entity selectorEntity : selector.getEntities(world, pos, entity)) {
				entity = selectorEntity;
				r += next.call();
			}
			entity = entityCache;
			return r;
		}
	}
	
	public class At extends Converter {
		final Converter next;
		final Selector selector;
		
		
		public At(Converter next, Selector selector) {
			this.next = next;
			this.selector = selector;
		}
		
		@Override
		int call() {
			Vec3d posCache = pos;
			Vec2f rotCache = rot;
			int r = 0;
			for (Entity selectorEntity : selector.getEntities(world, pos, entity)) {
				pos = selectorEntity.getPos();
				rot = selectorEntity.getRotationClient();
				r += next.call();
			}
			pos = posCache;
			rot = rotCache;
			return r;
		}
	}
	
	public class FacingPos extends Converter {
		final Converter next;
		final PosArgument posArgument;
		
		
		public FacingPos(Converter next, PosArgument posArgument) {
			this.next = next;
			this.posArgument = posArgument;
		}
		
		@Override
		int call() {
			Vec2f rotCache = rot;
			Vec3d start;
			if (entity == null)
				start = pos;
			else
				start = entityAnchor.offset.apply(pos, entity);
			
			Vec3d end = posArgument.toAbsolutePos(source());
			double d = end.x - start.x;
			double e = end.y - start.y;
			double f = end.z - start.z;
			double g = MathHelper.sqrt(d * d + f * f);
			float h = MathHelper
					.wrapDegrees((float) (-(MathHelper.atan2(e, g) * 57.2957763671875D))); // almost 180 / pi
			float i = MathHelper.wrapDegrees((float) (MathHelper.atan2(f, d) * 57.2957763671875D) - 90.0F);
			rot = new Vec2f(h, i);
			int r = next.call();
			rot = rotCache;
			return r;
		}
	}
	
	public class FacingEntity extends Converter {
		final Converter next;
		final Selector selector;
		private EntityAnchorArgumentType.EntityAnchor anchor;
		
		
		public FacingEntity(Converter next, Selector selector, EntityAnchorArgumentType.EntityAnchor anchor) {
			this.next = next;
			this.selector = selector;
			this.anchor = anchor;
		}
		
		@Override
		int call() {
			Vec2f rotCache = rot;
			int r = 0;
			
			Vec3d start;
			if (entity == null)
				start = pos;
			else
				start = entityAnchor.offset.apply(pos, entity);
			
			for (Entity selectorEntity : selector.getEntities(world, pos, entity)) {
				Vec3d end = anchor.positionAt(selectorEntity);
				double d = end.x - start.x;
				double e = end.y - start.y;
				double f = end.z - start.z;
				double g = MathHelper.sqrt(d * d + f * f);
				float h = MathHelper
						.wrapDegrees((float) (-(MathHelper.atan2(e, g) * 57.2957763671875F))); // (180 - 1e-5) / pi
				float i = MathHelper.wrapDegrees((float) (MathHelper.atan2(f, d) * 57.2957763671875F) - 90.0F);
				rot = new Vec2f(h, i);
				r += next.call();
			}
			rot = rotCache;
			return r;
		}
	}
	
	public class In extends Converter {
		final Converter next;
		final DimensionType dimension;
		
		public In(Converter next, DimensionType dimension) {
			this.next = next;
			this.dimension = dimension;
		}
		
		@Override
		int call() {
			ServerWorld worldCache = world;
			world = server.getWorld(dimension);
			int r = next.call();
			world = worldCache;
			return r;
		}
	}
	
	public class Positioned extends Converter {
		final Converter next;
		final PosArgument innerPos;
		
		public Positioned(Converter next, PosArgument pos) {
			this.next = next;
			this.innerPos = pos;
		}
		
		@Override
		int call() {
			Vec3d posCache = pos;
			pos = innerPos.toAbsolutePos(source());
			int r = next.call();
			pos = posCache;
			return r;
		}
	}
	
	public class PositionedAs extends Converter {
		final Converter next;
		final Selector selector;
		
		public PositionedAs(Converter next, Selector selector) {
			this.next = next;
			this.selector = selector;
		}
		
		@Override
		int call() {
			Vec3d posCache = pos;
			int r = 0;
			for (Entity selectorEntity : selector.getEntities(world, pos, entity)) {
				pos = selectorEntity.getPos();
				r += next.call();
			}
			pos = posCache;
			return r;
		}
	}
	
	public class Rotated extends Converter {
		final Converter next;
		final Vec2f innerRot;
		
		public Rotated(Converter next, Vec2f rot) {
			this.next = next;
			this.innerRot = rot;
		}
		
		@Override
		int call() {
			Vec2f rotCache = rot;
			rot = innerRot;
			int r = next.call();
			rot = rotCache;
			return r;
		}
	}
	
	public class RotatedAs extends Converter {
		final Converter next;
		final Selector selector;
		
		public RotatedAs(Converter next, Selector selector) {
			this.next = next;
			this.selector = selector;
		}
		
		@Override
		int call() {
			Vec2f rotCache = rot;
			int r = 0;
			for (Entity selectorEntity : selector.getEntities(world, pos, entity)) {
				rot = selectorEntity.getRotationClient();
				r += next.call();
			}
			rot = rotCache;
			return r;
		}
	}
	
	// store
	
	
	public class IfEntity extends Converter {
		final Converter next;
		final Selector selector;
		
		public IfEntity(Converter next, Selector selector) {
			this.next = next;
			this.selector = selector;
		}
		
		@Override
		int call() {
			//if(selector.exists())
			if (!selector.getEntities(world, pos, entity).isEmpty()) {
				return next.call(); // TODO should be nullable
			} else {
				return 0;
			}
		}
	}
	
	public class IfScore extends Converter {
		final Converter next;
		final Score score;
		final ScoreComparator comparator;
		
		public IfScore(Converter next, Score score, ScoreComparator comparator) {
			this.next = next;
			this.score = score;
			this.comparator = comparator;
		}
		
		@Override
		int call() {
			//if(selector.exists())
			if (comparator.compareTo(score, world, pos, entity)) {
				return next.call(); // TODO should be nullable
			} else {
				return 0;
			}
		}
	}
	
	public class Run extends Converter {
		final Command cmd;
		
		public Run(Command cmd) {
			this.cmd = cmd;
		}
		
		@Override
		int call() {
			return cmd.execute(source());
		}
		
	}
	
}
