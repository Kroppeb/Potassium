/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package kroppeb.server.command.commands;

import kroppeb.server.command.Command;
import kroppeb.server.command.InvocationError;
import kroppeb.server.command.arguments.Selector;
import kroppeb.server.command.reader.Reader;
import kroppeb.server.command.reader.ReaderException;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementCriterion;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.AdvancementCommand.Operation;
import net.minecraft.server.command.AdvancementCommand.Selection;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.List;

abstract public class AdvancementCommand implements Command {
	final Operation operation;
	final Selector.PlayerSelector targets;
	final Selection selection;
	final Identifier advancement;
	
	protected AdvancementCommand(Operation operation, Selector.PlayerSelector targets, Selection selection, Identifier advancement) {
		this.operation = operation;
		this.targets = targets;
		this.selection = selection;
		this.advancement = advancement;
	}
	
	public static AdvancementCommand read(Reader reader) throws ReaderException {
		Operation operation;
		String word = reader.readLiteral();
		switch (word) {
			case "grant":
				operation = Operation.GRANT;
				break;
			case "revoke":
				operation = Operation.REVOKE;
				break;
			default:
				throw new ReaderException("expected (grant/revoke), got " + word);
		}
		Selector.PlayerSelector targets = Selector.PlayerSelector.read(reader);
		reader.moveNext();
		Selection selection;
		word = reader.readLiteral();
		switch (word) {
			case "everything":
				return new AdvancementAdvancement(operation, targets, Selection.EVERYTHING, null);
			case "only":
				selection = Selection.ONLY;
				break;
			case "from":
				selection = Selection.FROM;
				break;
			case "through":
				selection = Selection.THROUGH;
				break;
			case "until":
				selection = Selection.UNTIL;
				break;
			default:
				throw new ReaderException("expected a mode of selection, got " + word);
		}
		
		Identifier advancement = reader.readIdentifier();
		
		if (selection == Selection.ONLY && reader.hasNext()) {
			return new AdvancementCriterion(operation, targets, advancement, reader.readIdentifier());
		}
		
		return new AdvancementAdvancement(operation, targets, selection, advancement);
	}
	
	static class AdvancementAdvancement extends AdvancementCommand {
		
		
		protected AdvancementAdvancement(Operation operation, Selector.PlayerSelector targets, Selection selection, Identifier advancement) {
			super(operation, targets, selection, advancement);
		}
		
		@Override
		public int execute(ServerCommandSource source) throws InvocationError {
			int i = 0;
			
			Collection<? extends PlayerEntity> entities = targets.getPlayers(source);
			List<Advancement> advancements = net.minecraft.server.command.AdvancementCommand
					.select(source.getMinecraftServer().getAdvancementLoader().get(advancement),
							selection);// TODO cache
			
			for (PlayerEntity entity : entities) {
				i += operation.processAll((ServerPlayerEntity) entity, advancements);
			}
			
			
			if (i == 0)
				throw new InvocationError();
			return i;
		}
	}
	
	static class AdvancementCriterion extends AdvancementCommand {
		final Identifier criterion;
		
		protected AdvancementCriterion(Operation operation, Selector.PlayerSelector targets, Identifier advancement, Identifier criterion) {
			super(operation, targets, Selection.ONLY, advancement);
			this.criterion = criterion;
		}
		
		@Override
		public int execute(ServerCommandSource source) throws InvocationError {
			int i = 0;
			
			Collection<? extends PlayerEntity> entities = targets.getPlayers(source);
			Advancement adv = source.getMinecraftServer()
					.getAdvancementLoader().get(advancement);// TODO cache
			for (PlayerEntity entity : entities) {
				
				if (operation.processEachCriterion((ServerPlayerEntity) entity, adv, criterion.toString()))
					i++;
			}
			
			
			if (i == 0)
				throw new InvocationError();
			return i;
		}
	}
	
}
