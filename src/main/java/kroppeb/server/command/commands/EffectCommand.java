/*
* Copyright (c) 2020 MadHau5
* This Source Code Form is subject to the terms of the Mozilla Public
* License, v. 2.0. If a copy of the MPL was not distributed with this
* file, You can obtain one at http://mozilla.org/MPL/2.0/.
*/

package kroppeb.server.command.commands;

import kroppeb.server.command.Command;
import kroppeb.server.command.arguments.Selector;
import kroppeb.server.command.reader.Reader;
import kroppeb.server.command.reader.ReaderException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.registry.Registry;

import java.util.Collection;

abstract public class EffectCommand implements Command {
	final Selector targets;
	final StatusEffect effect;
	
	public static EffectCommand read(Reader reader) throws ReaderException {
		String sub = reader.readLiteral();
		switch (sub) {
			case "give":
				return GiveCommand.read(reader);
			case "clear":
				return ClearCommand.read(reader);
			default:
				throw new ReaderException("Unexpected effect literal: " + sub);
		}
		
	}
	
	public static class GiveCommand extends EffectCommand {
		final int duration;
		final int amplifier;
		final boolean hideParticles;
		
		public GiveCommand(Selector targets, StatusEffect effect, int duration, int amplifier, boolean hideParticles) {
			this.targets = targets;
			this.effect = effect;
			this.duration = duration;
			this.amplifier = amplifier;
			this.hideParticles = hideParticles;
		}
		
		public static GiveCommand read(Reader reader) throws ReaderException {
			Selector targets = Selector.read(reader);
			StatusEffect effect = Registry.STATUS_EFFECT.get(reader.readIdentifier());
			int duration;
			int amplifier = 0;
			boolean hideParticles = false;
			if (reader.hasNext()) {
				reader.moveNext();
				duration = reader.readInt();
			} else {
				duration = -1;
			}
			if (reader.hasNext()) {
				reader.moveNext();
				amplifier = reader.readInt();
			}
			if (reader.hasNext()) {
				String bool = reader.readLiteral();
				switch (bool) {
					case "true":
						hideParticles = true;
						break;
					case "false":
						hideParticles = false;
						break;
					default:
						throw new ReaderException("Unexpected boolean literal: " + bool);
				}
			}
			return new GiveCommand(targets, effect, duration, amplifier, hideParticles);
		}
		
		public int execute(ServerCommandSource source) {
			int e = 0;
			Collection<Entity> entities = targets.getEntities(source);
			for (Entity entity : entities) {
				if (entity instanceof LivingEntity) {
					LivingEntity livingEntity = (LivingEntity) entity;
					if (effect.isInstant()) {
						if (duration == -1) {
							duration = 1;
						}
					} else {
						if (duration == -1) {
							duration = 30;
						}
						duration *= 20;
					}
					livingEntity.addStatusEffect(new StatusEffectInstance(effect,
							duration, amplifier, false, hideParticles));
					e++;
				}
			}
			if (e == 0) {
				// TODO: throw error on failed command
			}
			return e;
		}
	}
	
	public static class ClearCommand extends EffectCommand {
		
		public ClearCommand(Selector targets, StatusEffect effect) {
			this.targets = targets;
			this.effect = null;
		}
		
		public static ClearCommand read(Reader reader) throws ReaderException {
			Selector targets = Selector.read(reader);
			StatusEffect effect;
			if (reader.hasNext()) {
				effect = Registry.STATUS_EFFECT.get(reader.readIdentifier());
			}
			return new ClearCommand(targets, effect);
		}
		
		public int execute(ServerCommandSource source) {
			int e = 0;
			Collection<Entity> entities = targets.getEntities(source);
			if (effect == null) {
				for (Entity entity : entities) {
					if (entity instanceof LivingEntity) {
						((LivingEntity) entity).clearStatusEffects();
						e++;
					}
				}
			} else {
				for (Entity entity : entities) {
					if (entity instanceof LivingEntity) {
						((LivingEntity) entity).removeStatusEffect(effect);
						e++;
					}
				}
			}
			if (e == 0) {
				// TODO: throw error on failed command
			}
			return e;
		}
	}
}
