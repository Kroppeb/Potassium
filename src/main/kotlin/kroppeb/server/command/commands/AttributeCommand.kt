/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package kroppeb.server.command.commands

import com.mojang.brigadier.exceptions.CommandSyntaxException
import kroppeb.server.command.Command
import kroppeb.server.command.InvocationError
import kroppeb.server.command.arguments.selector.Selector
import kroppeb.server.command.reader.*
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.attribute.EntityAttribute
import net.minecraft.entity.attribute.EntityAttributeInstance
import net.minecraft.entity.attribute.EntityAttributeModifier
import net.minecraft.entity.attribute.EntityAttributeModifier.Operation
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.TranslatableText
import net.minecraft.util.registry.Registry
import java.util.*


sealed class AttributeCommand(val target: Selector.SingleSelector, val attribute: EntityAttribute)
	: Command {
	companion object : ReadFactory<AttributeCommand> {

		override fun Reader.parse(): AttributeCommand {
			fun readScale() = if (canRead()) Double() else 1.0
			val target = Selector.SingleSelector()
			val id = Id()
			val attribute = Registry.ATTRIBUTE.get(id) ?: throw ReaderException("unknown atribute: $id")
			return when (val sub = Literal()) {
				"get" -> GetTotal(target, attribute, readScale())
				"base" -> when (val set = Literal()) {
					"get" -> GetBase(target, attribute, readScale())
					"set" -> SetBase(target, attribute, Double())
					else -> expected("attribute <target> <attribute> base", "(get|set)", set)
				}
				"modifier" -> when (val action = Literal()) {
					"add" -> Add(
						target, attribute, UUID(), String(), Double(),
						when (val opp = Literal()) {
							"add" -> Operation.ADDITION
							"multiply" -> Operation.MULTIPLY_TOTAL
							"multiply_base" -> Operation.MULTIPLY_BASE
							else -> expected(
								"attribute <target> <attribute> modifier add <uuid> <name> <value>",
								"(add|multiply|multiply_base)",
								opp)
						})
					"remove" -> Remove(target, attribute, UUID())
					"value" -> {
						if (!tryReadLiteral("get")) {
							expected("attribute <target> <attribute> modifier value", "get", readLine())
						}
						ValueGet(target, attribute, UUID(), if (canRead()) Double() else 1.0)
					}
					else -> expected("attribute <target> <attribute> modifier", "(add|remove|value)", action)
				}
				else -> expected("attribute <target> <attribute>", "(set|base|modifier)", sub)
			}
		}
	}

	class GetTotal(target: Selector.SingleSelector, attribute: EntityAttribute, val scale: Double)
		: AttributeCommand(target, attribute) {
		override fun execute(source: ServerCommandSource): Int =
			(getLivingEntityWithAttribute(source).getAttributeValue(attribute) * scale).toInt()
	}

	class GetBase(target: Selector.SingleSelector, attribute: EntityAttribute, val scale: Double)
		: AttributeCommand(target, attribute) {
		override fun execute(source: ServerCommandSource): Int =
			(getLivingEntityWithAttribute(source).getAttributeBaseValue(attribute) * scale).toInt()
	}

	class SetBase(target: Selector.SingleSelector, attribute: EntityAttribute, val value: Double)
		: AttributeCommand(target, attribute) {
		override fun execute(source: ServerCommandSource): Int {
			getAttributeInstance(source).baseValue = value
			return 1
		}
	}

	class Add(
		target: Selector.SingleSelector,
		attribute: EntityAttribute,
		val uuid: UUID,
		val name: String,
		val value: Double,
		val operation: Operation)
		: AttributeCommand(target, attribute) {
		override fun execute(source: ServerCommandSource): Int {
			val instance = getAttributeInstance(source)
			val new = EntityAttributeModifier(uuid, name, value, operation)
			if(instance.hasModifier(new))
				throw InvocationError()
			instance.addPersistentModifier(new)
			return 1
		}
	}

	class Remove(target: Selector.SingleSelector, attribute: EntityAttribute, val uuid: UUID)
		: AttributeCommand(target, attribute) {
		override fun execute(source: ServerCommandSource): Int {
			if(getAttributeInstance(source).tryRemoveModifier(uuid)){
				return 1
			}
			throw InvocationError()
		}
	}

	class ValueGet(target: Selector.SingleSelector, attribute: EntityAttribute, val uuid: UUID, val scale: Double)
		: AttributeCommand(target, attribute) {
		override fun execute(source: ServerCommandSource): Int {
			val entity = getLivingEntityWithAttribute(source)
			val attributes = entity.attributes
			if(!attributes.hasModifierForAttribute(attribute, uuid))
				throw InvocationError()
			return (attributes.getModifierValue(attribute, uuid) * scale).toInt()

		}
	}


	@Throws(CommandSyntaxException::class)
	protected fun getAttributeInstance(source: ServerCommandSource): EntityAttributeInstance {
		val entity = target.getEntity(source)
		if(entity !is LivingEntity) throw InvocationError()
		return entity.attributes.getCustomInstance(attribute) ?: throw InvocationError()
	}

	@Throws(CommandSyntaxException::class)
	protected fun getLivingEntityWithAttribute(source: ServerCommandSource): LivingEntity {
		val entity = target.getEntity(source)
		if(entity !is LivingEntity) throw InvocationError()

		if (!entity.attributes.hasAttribute(attribute)) {
			throw InvocationError()
		}
		return entity
	}
}
