/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package kroppeb.server.command.commands;

import com.mojang.brigadier.ResultConsumer;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import kroppeb.server.command.Command;
import kroppeb.server.command.InvocationError;
import kroppeb.server.command.Parser;
import kroppeb.server.command.arguments.*;
import kroppeb.server.command.reader.Reader;
import kroppeb.server.command.reader.ReaderException;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.DataCommandStorage;
import net.minecraft.command.arguments.EntityAnchorArgumentType;
import net.minecraft.command.arguments.NbtPathArgumentType;
import net.minecraft.command.arguments.PosArgument;
import net.minecraft.entity.boss.BossBarManager;
import net.minecraft.entity.boss.CommandBossBar;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.world.dimension.DimensionType;
import org.apache.commons.lang3.NotImplementedException;

import java.util.EnumSet;
import java.util.function.Predicate;

public class ExecuteCommand implements Command {
	
	
	private CommandOutput output;
	private Vec3d pos;
	private Vec2f rot;
	private ServerWorld world;
	private int level;
	private String simpleName;
	private Text name;
	private MinecraftServer server;
	private net.minecraft.entity.Entity entity;
	private boolean silent;
	private ResultConsumer<ServerCommandSource> resultConsumer;
	private EntityAnchorArgumentType.EntityAnchor entityAnchor;
	
	Converter first;
	private StoreResolver store;
	
	
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
		
		first.call();
		return 0; // we optimize `run execute` away. No need to return anything;
	}
	
	// TODO try to replace as many calls to this as possible
	@Deprecated
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
				EnumSet<Direction.Axis> swizzle = ArgumentParser.readSwizzle(reader);
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
			case "store":
				return readStore(reader);
			case "if":
				return readIf(reader, true);
			case "unless":
				return readIf(reader, false);
			case "run":
				return new Run(Parser.readFunction(reader));
			default:
				throw new ReaderException("Unknown subcommand: " + subCommand);
		}
	}
	
	Converter readIf(Reader reader, boolean positive) throws ReaderException {
		String type = reader.readLiteral();
		switch (type) {
			case "block":
				PosArgument pos = ArgumentParser.readPos(reader);
				reader.moveNext();
				Predicate<CachedBlockPosition> predicate = ArgumentParser.readBlockPredicate(reader, true);
				return new IfBlock(tryReadConverter(reader), pos, predicate, true);
			case "entity":
				Selector s = Selector.read(reader);
				return new IfEntity(tryReadConverter(reader), s, positive);
			case "score":
				Score score = Score.read(reader);
				reader.moveNext();
				ScoreComparator comparator = ScoreComparator.read(reader);
				return new IfScore(tryReadConverter(reader), score, comparator, positive);
			default:
				throw new ReaderException("Unknown if subcommand: " + type);
		}
	}
	
	Converter tryReadConverter(Reader reader) throws ReaderException{
		if(!reader.hasNext())
			return null;
		return readConverter(reader);
	}
	
	Store readStore(Reader reader) throws ReaderException {
		String save = reader.readLiteral();
		boolean result;
		switch (save) {
			case "result":
				result = true;
				break;
			case "success":
				result = false;
				break;
			default:throw new ReaderException("expected result/success, got: " + save);
		}
		
		String type = reader.readLiteral();
		switch (type) {
			case "block":
				PosArgument pos = ArgumentParser.readPos(reader);
				reader.moveNext();
				NbtPathArgumentType.NbtPath path = ArgumentParser.readPath(reader);
				reader.moveNext();
				Cast cast = Cast.read(reader);
				reader.moveNext();
				double scale = reader.readDouble();
				reader.moveNext();
				return new StoreBlock(readConverter(reader), result, pos, path, cast, scale);
			case "bossbar":
				Identifier id = reader.readIdentifier();
				reader.moveNext();
				String saveTo = reader.readLiteral();
				boolean value;
				switch (saveTo) {
					case "value":
						value = true;
						break;
					case "max":
						value = false;
						break;
					default:throw new ReaderException("expected result/success, got: " + save);
				}
				return new StoreBossBar(readConverter(reader), result, id,value);
			case "entity":
				Selector.SingleSelector singleSelector = Selector.SingleSelector.read(reader);
				reader.moveNext();
				path = ArgumentParser.readPath(reader);
				reader.moveNext();
				cast = Cast.read(reader);
				reader.moveNext();
				scale = reader.readDouble();
				reader.moveNext();
				return new StoreEntity(readConverter(reader), result, singleSelector, path, cast, scale);
			case "score":
				ScoreHolder holder = ScoreHolder.read(reader);
				reader.moveNext();
				String objective = reader.readUntilWhitespace(); // TODO fix store score
				reader.moveNext();
				return new StoreScore(readConverter(reader), result, holder, objective);
			case "storage":
				id = reader.readIdentifier();
				reader.moveNext();
				path = ArgumentParser.readPath(reader);
				reader.moveNext();
				cast = Cast.read(reader);
				reader.moveNext();
				scale = reader.readDouble();
				reader.moveNext();
				return new StoreStorage(readConverter(reader), result, id, path, cast, scale);
			default:
				throw new ReaderException("Unknown store subcommand: " + type);
		}
	}
	
	abstract public static class Converter {
		abstract void call();
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
		void call() {
			posCache = pos;
			pos = pos.floorAlongAxes(axes);
			next.call();
			pos = posCache;
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
		void call() {
			entityAnchorCache = entityAnchor;
			entityAnchor = ea;
			next.call();
			entityAnchor = entityAnchorCache;
			
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
		void call() {
			net.minecraft.entity.Entity entityCache = entity;
			for (net.minecraft.entity.Entity selectorEntity : selector.getEntities(world, pos, entity)) {
				entity = selectorEntity;
				next.call();
			}
			entity = entityCache;
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
		void call() {
			Vec3d posCache = pos;
			Vec2f rotCache = rot;
			for (net.minecraft.entity.Entity selectorEntity : selector.getEntities(world, pos, entity)) {
				pos = selectorEntity.getPos();
				rot = selectorEntity.getRotationClient();
				next.call();
			}
			pos = posCache;
			rot = rotCache;
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
		void call() {
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
			next.call();
			rot = rotCache;
		}
	}
	
	public class FacingEntity extends Converter {
		final Converter next;
		final Selector selector;
		private final EntityAnchorArgumentType.EntityAnchor anchor;
		
		
		public FacingEntity(Converter next, Selector selector, EntityAnchorArgumentType.EntityAnchor anchor) {
			this.next = next;
			this.selector = selector;
			this.anchor = anchor;
		}
		
		@Override
		void call() {
			Vec2f rotCache = rot;
			
			Vec3d start;
			if (entity == null)
				start = pos;
			else
				start = entityAnchor.offset.apply(pos, entity);
			
			for (net.minecraft.entity.Entity selectorEntity : selector.getEntities(world, pos, entity)) {
				Vec3d end = anchor.positionAt(selectorEntity);
				double d = end.x - start.x;
				double e = end.y - start.y;
				double f = end.z - start.z;
				double g = MathHelper.sqrt(d * d + f * f);
				float h = MathHelper
						.wrapDegrees((float) (-(MathHelper.atan2(e, g) * 57.2957763671875F))); // (180 - 1e-5) / pi
				float i = MathHelper.wrapDegrees((float) (MathHelper.atan2(f, d) * 57.2957763671875F) - 90.0F);
				rot = new Vec2f(h, i);
				next.call();
			}
			rot = rotCache;
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
		void call() {
			ServerWorld worldCache = world;
			world = server.getWorld(dimension);
			next.call();
			world = worldCache;
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
		void call() {
			Vec3d posCache = pos;
			pos = innerPos.toAbsolutePos(source());
			next.call();
			pos = posCache;
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
		void call() {
			Vec3d posCache = pos;
			int r = 0;
			for (net.minecraft.entity.Entity selectorEntity : selector.getEntities(world, pos, entity)) {
				pos = selectorEntity.getPos();
				next.call();
			}
			pos = posCache;
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
		void call() {
			Vec2f rotCache = rot;
			rot = innerRot;
			next.call();
			rot = rotCache;
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
		void call() {
			Vec2f rotCache = rot;
			int r = 0;
			for (net.minecraft.entity.Entity selectorEntity : selector.getEntities(world, pos, entity)) {
				rot = selectorEntity.getRotationClient();
				next.call();
			}
			rot = rotCache;
		}
	}
	
	enum Cast {
		BYTE {
			@Override
			AbstractNumberTag convert(double input) {
				return ByteTag.of((byte) (int) input);
			}
		}, SHORT {
			@Override
			AbstractNumberTag convert(double input) {
				return ShortTag.of((short) (int) input);
			}
		}, INT {
			@Override
			AbstractNumberTag convert(double input) {
				return IntTag.of((int) input);
			}
		}, LONG {
			@Override
			AbstractNumberTag convert(double input) {
				return LongTag.of((long) input);
			}
		}, FLOAT {
			@Override
			AbstractNumberTag convert(double input) {
				return FloatTag.of((float) input);
			}
		}, DOUBLE {
			@Override
			AbstractNumberTag convert(double input) {
				return DoubleTag.of(input);
			}
		};
		
		public static Cast read(Reader reader) throws ReaderException {
			String type = reader.readUntilWhitespace();
			switch (type) {
				case "byte": return BYTE;
				case "short": return SHORT;
				case "int": return INT;
				case "long": return LONG;
				case "float": return FLOAT;
				case "double": return DOUBLE;
				default: throw new ReaderException("Unknown datatype: " + type);
			}
		}
		
		abstract AbstractNumberTag convert(double input);
	}
	
	abstract public class StoreResolver {
		final StoreResolver prev;
		final boolean result;
		
		protected StoreResolver(boolean result) {
			this.result = result;
			this.prev = store;
			store = this;
		}
		
		final void fail() {
			set(0);
			if (prev != null)
				prev.fail();
		}
		
		final void success(int value) {
			set(result ? value : 1);
			if (prev != null)
				prev.success(value);
		}
		
		abstract void set(int value);
		
		final public void restore() {
			store = prev;
		}
	}
	
	abstract public class Store extends Converter {
		final Converter next;
		final boolean result;
		
		protected Store(Converter next, boolean result) {
			this.next = next;
			this.result = result;
		}
		
		
		
		@Override
		final void call() {
			StoreResolver resolver = this.resolve();
			if (resolver == null) {
				if (store != null)
					store.fail();
				return;
			}
			next.call();
			resolver.restore();
		}
		
		protected abstract StoreResolver resolve();
		
		
	}
	
	abstract class NbtContainer extends Store {
		final NbtPathArgumentType.NbtPath path;
		final Cast cast;
		final double scale;
		
		protected NbtContainer(Converter next, boolean result, NbtPathArgumentType.NbtPath path, Cast cast, double scale) {
			super(next, result);
			this.path = path;
			this.cast = cast;
			this.scale = scale;
		}
	}
	
	class StoreBlock extends NbtContainer {
		final PosArgument pos;
		
		protected StoreBlock(Converter next, boolean result, PosArgument pos, NbtPathArgumentType.NbtPath path, Cast cast, double scale) {
			super(next, result, path, cast, scale);
			this.pos = pos;
		}
		
		@Override
		protected StoreResolver resolve() {
			BlockPos blockPos = pos.toAbsoluteBlockPos(source());
			final BlockEntity block = world.getBlockEntity(blockPos);
			if (block == null)
				return null;
			return new StoreResolver(result) {
				@Override
				void set(int value) {
					CompoundTag tag = new CompoundTag();
					block.toTag(tag);
					try {
						path.put(tag, () -> cast.convert(value * scale));
					} catch (CommandSyntaxException e) {
						return;
					}
					block.fromTag(block.getCachedState(), tag);
				}
			};
		}
	}
	
	class StoreEntity extends NbtContainer {
		final Selector.SingleSelector selector;
		
		protected StoreEntity(Converter next, boolean result, Selector.SingleSelector selector, NbtPathArgumentType.NbtPath path, Cast cast, double scale) {
			super(next, result, path, cast, scale);
			this.selector = selector;
		}
		
		@Override
		protected StoreResolver resolve() {
			final net.minecraft.entity.Entity entity = selector.getEntity(source());
			if (entity == null)
				return null;
			return new StoreResolver(result) {
				@Override
				void set(int value) {
					CompoundTag tag = new CompoundTag();
					entity.toTag(tag);
					try {
						path.put(tag, () -> cast.convert(value * scale));
					} catch (CommandSyntaxException e) {
						return;
					}
					entity.fromTag(tag);
				}
			};
		}
	}
	
	class StoreStorage extends NbtContainer {
		final Identifier id;
		
		protected StoreStorage(Converter next, boolean result, Identifier id, NbtPathArgumentType.NbtPath path, Cast cast, double scale) {
			super(next, result, path, cast, scale);
			this.id = id;
		}
		
		@Override
		protected StoreResolver resolve() {
			final DataCommandStorage storage = server.getDataCommandStorage();
			
			return new StoreResolver(result) {
				@Override
				void set(int value) {
					CompoundTag tag = storage.get(id);
					try {
						path.put(tag, () -> cast.convert(value * scale));
					} catch (CommandSyntaxException e) {
						return;
					}
					storage.set(id, tag);
				}
			};
		}
	}
	
	class StoreBossBar extends Store {
		final Identifier id;
		final boolean value;
		
		protected StoreBossBar(Converter next, boolean result, Identifier id, boolean value) {
			super(next, result);
			this.id = id;
			this.value = value;
		}
		
		@Override
		protected StoreResolver resolve() {
			final BossBarManager manager = server.getBossBarManager();
			final CommandBossBar bar = manager.get(id);
			if (bar == null)
				return null;
			return new StoreResolver(result) {
				@Override
				void set(int value) {
					if (StoreBossBar.this.value) {
						bar.setValue(value);
					} else {
						bar.setMaxValue(value);
					}
				}
			};
		}
	}
	
	class StoreScore extends Store {
		final ScoreHolder holder;
		final String scoreboard;
		
		protected StoreScore(Converter next, boolean result, ScoreHolder holder, String scoreboard) {
			super(next, result);
			this.holder = holder;
			this.scoreboard = scoreboard;
		}
		
		@Override
		protected StoreResolver resolve() {
			// TODO implement store score.
			throw new RuntimeException();
		}
	}
	
	public class IfBlock extends Converter {
		final Converter next;
		final PosArgument pos;
		final Predicate<CachedBlockPosition> predicate;
		final boolean positive;
		
		public IfBlock(Converter next, PosArgument pos, Predicate<CachedBlockPosition> predicate, boolean positive) {
			this.next = next;
			this.pos = pos;
			this.predicate = predicate;
			this.positive = positive;
		}
		
		@Override
		void call() {
			BlockPos blockPos = pos.toAbsoluteBlockPos(source());
			if (!world.isChunkLoaded(blockPos))
				return;
			CachedBlockPosition cachedBlock = new CachedBlockPosition(world, blockPos, true);
			if (predicate.test(cachedBlock))
				next.call();
		}
	}
	
	public class IfEntity extends Converter {
		final Converter next;
		final Selector selector;
		final boolean positive;
		
		public IfEntity(Converter next, Selector selector, boolean positive) {
			this.next = next;
			this.selector = selector;
			this.positive = positive;
		}
		
		@Override
		void call() {
			// TODO if(selector.exists())
			if (!selector.getEntities(world, pos, entity).isEmpty() == positive) {
				next.call(); // TODO should be nullable? I think adding a virutal run at the end might be better if possible
			}
		}
	}
	
	public class IfScore extends Converter {
		final Converter next;
		final Score score;
		final ScoreComparator comparator;
		final boolean positive;
		
		public IfScore(Converter next, Score score, ScoreComparator comparator, boolean positive) {
			this.next = next;
			this.score = score;
			this.comparator = comparator;
			this.positive = positive;
		}
		
		@Override
		void call() {
			if (comparator.compareTo(score, world, pos, entity) == positive) {
				next.call(); // TODO should be nullable
			}
		}
	}
	
	public class Run extends Converter {
		final Command cmd;
		
		public Run(Command cmd) {
			this.cmd = cmd;
		}
		
		@Override
		void call() {
			try {
				cmd.execute(source());
			} catch (InvocationError invocationError) {
				// TODO save values.
			}
		}
		
	}
	
}
