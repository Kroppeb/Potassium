/*
 * Copyright (c) 2021 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package kroppeb.server.command.commands

import kroppeb.server.command.Command
import kroppeb.server.command.InvocationError
import kroppeb.server.command.arguments.selector.PlayerSelector
import kroppeb.server.command.arguments.selector.Selector
import kroppeb.server.command.arguments.selector.SinglePlayerSelector
import kroppeb.server.command.reader.*
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.server.command.ExperienceCommand.Component
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.TranslatableText
import net.minecraft.util.registry.Registry


sealed class EffectCommand(val selector: Selector) : Command {

	companion object : ReadFactory<EffectCommand> {

		override fun Reader.parse(): EffectCommand {
			when (val sub = Literal()) {
				"clear" -> {
					if (!canRead())
						return ClearAll(Selector.Self)
					val targets = Selector()
					if (!canRead())
						return ClearAll(targets)
					val id = Id()
					val effect = Registry.STATUS_EFFECT[id] ?: throw ReaderException("Not a valid effect: $id")
					return ClearEffect(targets, effect)
				}


				"give" -> {
					val targets = Selector()
					val id = Id()
					val effect = Registry.STATUS_EFFECT[id] ?: throw ReaderException("Not a valid effect: $id")
					if (!canRead())
						return GiveEffect(targets, effect, null, 0, true)
					val duration = Int()
					if (!canRead())
						return GiveEffect(targets, effect, duration, 0, true)
					val amplifier = Int()
					if (!canRead())
						return GiveEffect(targets, effect, duration, amplifier, true)
					val particles = Boolean()
					return GiveEffect(targets, effect, duration, amplifier, particles)

				}
				else -> expected("effect", "(clear|give)", sub)
			}
		}
	}

	class ClearAll(selector: Selector) : EffectCommand(selector) {
		override fun execute(source: ServerCommandSource): Int {
			val targets = selector.getEntities(source)
			val i = targets.count { it is LivingEntity && it.clearStatusEffects() }

			if (i == 0)
				throw InvocationError()
			return i
		}

	}

	class ClearEffect(selector: Selector, val effect: StatusEffect) : EffectCommand(selector) {
		override fun execute(source: ServerCommandSource): Int {
			val targets = selector.getEntities(source)
			val i = targets.count { it is LivingEntity && it.removeStatusEffect(effect) }
			if (i == 0)
				throw InvocationError()
			return i
		}

	}


	class GiveEffect private constructor(
		selector: Selector,
		val effect: StatusEffect,
		val duration: Int,
		val amplifier: Int,
		val showParticles: Boolean
	) : EffectCommand(selector) {
		constructor(
			selector: Selector,
			effect: StatusEffect,
			duration: Int?,
			amplifier: Int,
			showParticles: Boolean
		) : this(
			selector,
			effect,
			when {
				duration != null -> if (effect.isInstant) duration else duration * 20
				effect.isInstant -> 1
				else -> 600
			},
			amplifier,
			showParticles
		)

		override fun execute(source: ServerCommandSource): Int {
			val targets = selector.getEntities(source)
			val i = targets.count { entity ->
				entity is LivingEntity &&
					entity.addStatusEffect(StatusEffectInstance(effect, duration, amplifier, false, showParticles))
			}

			if (i == 0) throw InvocationError()

			return i
		}

	}

}
