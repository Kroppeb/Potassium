/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package kroppeb.server.command.arguments;

import kroppeb.server.command.reader.Reader;
import kroppeb.server.command.reader.ReaderException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public interface Selector {
	static Selector read(Reader reader) throws ReaderException {
		reader.readChar('@');
		switch (reader.read()) {
			case 's':
				if (reader.tryRead('[')) {
					readBuilder(reader);
					return new SelfFiltered();
				} else {
					return SELF;
				}
			default:
				if (reader.tryRead('[')) {
					readBuilder(reader);
				}
				return new Complex();
		}
	}
	
	static SelectorBuilder readBuilder(Reader reader) throws ReaderException {
		SelectorBuilder sb = new SelectorBuilder();
		while (!reader.tryRead(']')) {
			String option = reader.readUnquotedString();
			reader.next();
			reader.readChar('=');
			reader.next();
			switch (option) {
				case "x":
					sb.setX(reader.readDouble());
					break;
				case "y":
					sb.setY(reader.readDouble());
					break;
				case "z":
					sb.setZ(reader.readDouble());
					break;
				case "distance":
					sb.setDistance(SimpleDoubleRange.read(reader));
					break;
				case "dx":
					sb.setDx(reader.readDouble());
					break;
				case "dy":
					sb.setDy(reader.readDouble());
					break;
				case "dz":
					sb.setDz(reader.readDouble());
					break;
				case "scores":
					Map<String, IntRange> map = new HashMap<>();
					reader.readChar('{');
					reader.next();
					while (!reader.tryRead('}')) {
						String key = reader.readUnquotedString();
						reader.next();
						reader.readChar('=');
						reader.next();
						map.put(key, IntRange.read(reader));
						reader.next();
						
						if (!reader.tryRead(',')) {
							reader.readChar('}');
							break;
						}
						reader.next();
					}
					sb.setScores(map);
					break;
				case "limit":
					sb.setLimit(reader.readInt());
					break;
				case "level":
					sb.setLevel(IntRange.read(reader));
					break;
				case "gamemode":
					if (sb.gamemode == null)
						sb.gamemode = new Group<>();
					
					if (reader.tryRead('!')) {
						reader.next();
						sb.gamemode.negative.add(reader.readUnquotedString());
					} else {
						sb.gamemode.positive.add(reader.readUnquotedString());
					}
					
					break;
				case "name":
					if (sb.name == null)
						sb.name = new Group<>();
					
					if (reader.tryRead('!')) {
						reader.next();
						sb.name.negative.add(reader.readUnquotedString());
					} else {
						sb.name.positive.add(reader.readUnquotedString());
					}
					
					break;
				case "team":
					if (sb.team == null)
						sb.team = new Group<>();
					
					if (reader.tryRead('!')) {
						reader.next();
						sb.team.negative.add(reader.readUnquotedString());
					} else {
						sb.team.positive.add(reader.readUnquotedString());
					}
					
					break;
				case "type":
					if (sb.type == null)
						sb.type = new Group<>();
					
					if (reader.tryRead('!')) {
						reader.next();
						if (reader.tryRead('#')) {
							reader.next();
							sb.type.negative.add(new Tagable<>(true, reader.readIdentifier()));
						} else {
							sb.type.negative.add(new Tagable<>(false, reader.readIdentifier()));
						}
					} else {
						if (reader.tryRead('#')) {
							reader.next();
							sb.type.positive.add(new Tagable<>(true, reader.readIdentifier()));
						} else {
							sb.type.positive.add(new Tagable<>(false, reader.readIdentifier()));
						}
					}
					
					break;
				case "tag":
					if (sb.tag == null)
						sb.tag = new Group<>();
					
					if (reader.tryRead('!')) {
						reader.next();
						sb.tag.negative.add(reader.readUnquotedString());
					} else {
						sb.tag.positive.add(reader.readUnquotedString());
					}
					
					break;
				case "nbt":
					if (sb.nbt == null)
						sb.nbt = new Group<>();
					
					if (reader.tryRead('!')) {
						reader.next();
						sb.nbt.negative.add(ArgumentParser.readCompoundTag(reader));
					} else {
						sb.nbt.positive.add(ArgumentParser.readCompoundTag(reader));
					}
					
					break;
				case "predicate":
					if (sb.predicate == null)
						sb.predicate = new Group<>();
					
					if (reader.tryRead('!')) {
						reader.next();
						sb.predicate.negative.add(reader.readIdentifier());
					} else {
						sb.predicate.positive.add(reader.readIdentifier());
					}
					
					break; //TODO add gamemode, name, team, type, tag, nbt, predicate;
				case "x_rotation":
					sb.setxRotation(SimpleDoubleRange.read(reader));
					break;
				case "y_rotation":
					sb.setyRotation(SimpleDoubleRange.read(reader));
					break;
				case "advancements":
					//TODO add advancements
					int i = 1;
					while (i > 0) {
						char c = reader.read();
						if (c == '}')
							i--;
						if (c == '{')
							i++;
					}
					break;
				default:
					throw new ReaderException("unknown selector option: " + option);
			}
			reader.next();
			if (!reader.tryRead(',')) {
				reader.readChar(']');
				break;
			}
			reader.next();
		}
		return sb;
	}
	
	
	class Group<T> { // TODO some of these can be parsed away from string sooner
		final Set<T> positive = new HashSet<>();
		final Set<T> negative = new HashSet<>();
	}
	
	class Tagable<T> {
		final boolean isTag;
		final T value;
		
		public Tagable(boolean isTag, T value) {
			this.isTag = isTag;
			this.value = value;
		}
	}
	Collection<? extends Entity> getEntities(ServerWorld world, Vec3d pos, Entity executor);
	
	default Collection<? extends Entity> getEntities(ServerCommandSource source) {
		return getEntities(source.getWorld(), source.getPosition(), source.getEntity());
	}
	
	interface SingleSelector extends Selector {
		@Override
		default Collection<? extends Entity> getEntities(ServerWorld world, Vec3d pos, Entity executor) {
			Entity entity = getEntity(world, pos, executor);
			if (entity == null)
				return Collections.EMPTY_LIST;
			return Collections.singleton(entity);
		}
		
		static SingleSelector read(Reader reader) throws ReaderException{
			Selector selector = Selector.read(reader);
			if(selector instanceof SingleSelector)
				return (SingleSelector) selector;
			throw new ReaderException("not limited to 1 entity"); // TODO check limit value why
		}
		
		Entity getEntity(ServerWorld world, Vec3d pos, Entity executor);
		default Entity getEntity(ServerCommandSource source){
			return getEntity(source.getWorld(), source.getPosition(), source.getEntity());
		}
	}
	
	interface PlayerSelector extends Selector {
		@Override
		default Collection<? extends Entity> getEntities(ServerWorld world, Vec3d pos, Entity executor) {
			return getPlayers(world, pos, executor);
		}
		
		Collection<? extends PlayerEntity> getPlayers(ServerWorld world, Vec3d pos, Entity executor);
		default Collection<? extends PlayerEntity> getPlayers(ServerCommandSource source){
			return getPlayers(source.getWorld(), source.getPosition(), source.getEntity());
		}
	}
	
	interface SinglePlayerSelector extends SingleSelector, PlayerSelector {
		@Override
		default Collection<? extends Entity> getEntities(ServerWorld world, Vec3d pos, Entity executor) {
			return getPlayers(world, pos, executor);
		}
		
		@Override
		default Entity getEntity(ServerWorld world, Vec3d pos, Entity executor) {
			return getPlayer(world, pos, executor);
		}
		
		@Override
		default Collection<? extends PlayerEntity> getPlayers(ServerWorld world, Vec3d pos, Entity executor) {
			PlayerEntity entity = getPlayer(world, pos, executor);
			if (entity == null)
				return Collections.EMPTY_LIST;
			return Collections.singleton(entity);
		}
		

		PlayerEntity getPlayer(ServerWorld world, Vec3d pos, Entity executor);
		
		default public PlayerEntity getPlayer(ServerCommandSource source){
			return getPlayer(source.getWorld(), source.getPosition(), source.getEntity());
		}
	}
	
	Self SELF = new Self();
	Players ALL_PLAYERS = new Players();
	Entities ALL_ENTITIES = new Entities();
	SingleRandomPlayer RANDOM_PLAYER = new SingleRandomPlayer();
	SingleClosestPlayer CLOSEST_PLAYER = new SingleClosestPlayer();
	
	class Self implements SinglePlayerSelector {
		
		private Self() {
		}
		
		@Override
		public PlayerEntity getPlayer(ServerWorld world, Vec3d pos, Entity executor) {
			if (executor instanceof PlayerEntity)
				return (PlayerEntity) executor;
			return null;
		}
	}
	
	class SelfFiltered implements SinglePlayerSelector {
		
		@Override
		public PlayerEntity getPlayer(ServerWorld world, Vec3d pos, Entity executor) {
			return null; // TODO implement
		}
	}
	
	class SinglePlayer implements SinglePlayerSelector {
		@Override
		public PlayerEntity getPlayer(ServerWorld world, Vec3d pos, Entity executor) {
			return null;
			// TODO implement
		}
	}
	
	class SingleClosestPlayer implements SinglePlayerSelector {
		@Override
		public PlayerEntity getPlayer(ServerWorld world, Vec3d pos, Entity executor) {
			PlayerEntity player = null;
			double distance = Double.NEGATIVE_INFINITY;
			
			for (ServerPlayerEntity worldPlayer : world.getPlayers()) {
				double d = worldPlayer.squaredDistanceTo(pos);
				if (d < distance) {
					player = worldPlayer;
					distance = d;
				}
				
			}
			return player;
		}
	}
	
	class SingleRandomPlayer implements SinglePlayerSelector {
		@Override
		public PlayerEntity getPlayer(ServerWorld world, Vec3d pos, Entity executor) {
			return world.getRandomAlivePlayer();
		}
	}
	
	class Players implements PlayerSelector {
		@Override
		public Collection<? extends PlayerEntity> getPlayers(ServerWorld world, Vec3d pos, Entity executor) {
			return world.getPlayers();
		}
	}
	
	class PlayersFiltred implements PlayerSelector {
		
		@Override
		public Collection<? extends PlayerEntity> getPlayers(ServerWorld world, Vec3d pos, Entity executor) {
			return null;
			// TODO implement
		}
	}
	
	
	class Entities implements Selector {
		@Override
		public Collection<? extends Entity> getEntities(ServerWorld world, Vec3d pos, Entity executor) {
			return null;
			// TODO implement
		}
	}
	
	class Complex implements Selector {
		
		@Override
		public Collection<Entity> getEntities(ServerWorld world, Vec3d pos, Entity executor) {
			return null;
		}
	}
}

class SelectorBuilder {
	void assertSingle(Object value, String name) throws ReaderException {
		if (value != null)
			throw new ReaderException("Duplicate key in selector " + name);
	}
	
	//region setters
	public void setX(Double x) throws ReaderException {
		assertSingle(this.x, "x");
		this.x = x;
	}
	
	public void setY(Double y) throws ReaderException {
		assertSingle(this.y, "y");
		this.y = y;
	}
	
	public void setZ(Double z) throws ReaderException {
		assertSingle(this.z, "z");
		this.z = z;
	}
	
	public void setDistance(SimpleDoubleRange distance) throws ReaderException {
		assertSingle(this.distance, "distance");
		this.distance = distance;
	}
	
	public void setDx(Double dx) throws ReaderException {
		assertSingle(this.dx, "dx");
		this.dx = dx;
	}
	
	public void setDy(Double dy) throws ReaderException {
		assertSingle(this.dy, "dy");
		this.dy = dy;
	}
	
	public void setDz(Double dz) throws ReaderException {
		assertSingle(this.dz, "dz");
		this.dz = dz;
	}
	
	public void setScores(Map<String, IntRange> scores) throws ReaderException {
		assertSingle(this.scores, "scores");
		this.scores = scores;
	}
	
	public void setLimit(Integer limit) throws ReaderException {
		assertSingle(this.limit, "limit");
		this.limit = limit;
	}
	
	public void setSort(String sort) throws ReaderException {
		assertSingle(this.sort, "sort");
		this.sort = sort;
	}
	
	public void setLevel(IntRange level) throws ReaderException {
		assertSingle(this.level, "level");
		this.level = level;
	}
	
	public void setxRotation(SimpleDoubleRange xRotation) throws ReaderException {
		assertSingle(this.xRotation, "x_rotation");
		this.xRotation = xRotation;
	}
	
	public void setyRotation(SimpleDoubleRange yRotation) throws ReaderException {
		assertSingle(this.yRotation, "y_rotation");
		this.yRotation = yRotation;
	}
	
	public void setAdvancements(String advancements) throws ReaderException {
		assertSingle(this.advancements, "advancements");
		this.advancements = advancements;
	}
	
	//endregion
	//region properties
	private Double x;
	private Double y;
	private Double z;
	
	private SimpleDoubleRange distance;
	
	private Double dx;
	private Double dy;
	private Double dz;
	
	private Map<String, IntRange> scores;
	Selector.Group<String> team;
	
	private Integer limit;
	private String sort;
	
	private IntRange level;
	
	Selector.Group<String> gamemode;
	Selector.Group<String> name;
	
	private SimpleDoubleRange xRotation;
	private SimpleDoubleRange yRotation;
	
	Selector.Group<Selector.Tagable<Identifier>> type;
	Selector.Group<String> tag;
	Selector.Group<CompoundTag> nbt;
	
	private String advancements;
	
	Selector.Group<Identifier> predicate;
	//endregion
	// TODO save order.
	
}
