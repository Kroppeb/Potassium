/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package kroppeb.server.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.DataCommandObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Collection;
import java.util.Iterator;

public class Execute {
	private Execute(){
		throw new IllegalStateException();
	}
	
	static public void dataMerge(DataCommandObject object, CompoundTag tag) {
		try {
			CompoundTag compoundTag = object.getTag();
			CompoundTag compoundTag2 = compoundTag.copy().copyFrom(tag);
			if (compoundTag.equals(compoundTag2)) {
				throw new RuntimeException(); //MERGE_FAILED_EXCEPTION.create();
			} else {
				object.setTag(compoundTag2);
				//source.sendFeedback(object.feedbackModify(), true);
				//return 1;
			}
		} catch (CommandSyntaxException e) {
			throw new RuntimeException(e);
		}
	}
	
	static public void playSound(Collection<ServerPlayerEntity> targets, Identifier sound, SoundCategory category, Vec3d pos, float volume, float pitch, float minVolume) {
		double d = Math.pow(volume > 1.0F ? (double) (volume * 16.0F) : 16.0D, 2.0D);
		int i = 0;
		Iterator<ServerPlayerEntity> var11 = targets.iterator();
		
		while (true) {
			ServerPlayerEntity serverPlayerEntity;
			Vec3d vec3d;
			float j;
			while (true) {
				if (!var11.hasNext()) {
					if (i == 0) {
						throw new RuntimeException(); //FAILED_EXCEPTION.create();
					}
					
					if (targets.size() == 1) {
						//source.sendFeedback(new TranslatableText("commands.playsound.success.single", new Object[]{sound, ((ServerPlayerEntity)targets.iterator().next()).getDisplayName()}), true);
					} else {
						//source.sendFeedback(new TranslatableText("commands.playsound.success.multiple", new Object[]{sound, targets.size()}), true);
					}
					
					//return i;
				}
				
				serverPlayerEntity = (ServerPlayerEntity) var11.next();
				double e = pos.x - serverPlayerEntity.getX();
				double f = pos.y - serverPlayerEntity.getY();
				double g = pos.z - serverPlayerEntity.getZ();
				double h = e * e + f * f + g * g;
				vec3d = pos;
				j = volume;
				if (h <= d) {
					break;
				}
				
				if (minVolume > 0.0F) {
					double k = (double) MathHelper.sqrt(h);
					vec3d = new Vec3d(serverPlayerEntity.getX() + e / k * 2.0D, serverPlayerEntity.getY() + f / k * 2.0D, serverPlayerEntity.getZ() + g / k * 2.0D);
					j = minVolume;
					break;
				}
			}
			
			//serverPlayerEntity.networkHandler.sendPacket(new PlaySoundIdS2CPacket(sound, category, vec3d, j, pitch));
			++i;
		}
	}
	
	static public int playSoundCount(Collection<ServerPlayerEntity> targets, Identifier sound, SoundCategory category, Vec3d pos, float volume, float pitch, float minVolume) {
		double d = Math.pow(volume > 1.0F ? (double) (volume * 16.0F) : 16.0D, 2.0D);
		int i = 0;
		Iterator<ServerPlayerEntity> var11 = targets.iterator();
		
		while (true) {
			ServerPlayerEntity serverPlayerEntity;
			Vec3d vec3d;
			float j;
			while (true) {
				if (!var11.hasNext()) {
					if (i == 0) {
						throw new RuntimeException(); //FAILED_EXCEPTION.create();
					}
					
					if (targets.size() == 1) {
						//source.sendFeedback(new TranslatableText("commands.playsound.success.single", new Object[]{sound, ((ServerPlayerEntity)targets.iterator().next()).getDisplayName()}), true);
					} else {
						//source.sendFeedback(new TranslatableText("commands.playsound.success.multiple", new Object[]{sound, targets.size()}), true);
					}
					
					return i;
				}
				
				serverPlayerEntity = (ServerPlayerEntity) var11.next();
				double e = pos.x - serverPlayerEntity.getX();
				double f = pos.y - serverPlayerEntity.getY();
				double g = pos.z - serverPlayerEntity.getZ();
				double h = e * e + f * f + g * g;
				vec3d = pos;
				j = volume;
				if (h <= d) {
					break;
				}
				
				if (minVolume > 0.0F) {
					double k = (double) MathHelper.sqrt(h);
					vec3d = new Vec3d(serverPlayerEntity.getX() + e / k * 2.0D, serverPlayerEntity.getY() + f / k * 2.0D, serverPlayerEntity.getZ() + g / k * 2.0D);
					j = minVolume;
					break;
				}
			}
			
			//serverPlayerEntity.networkHandler.sendPacket(new PlaySoundIdS2CPacket(sound, category, vec3d, j, pitch));
			++i;
		}
	}
}
