package com.skycatdev.rlmc.environment;

import carpet.helpers.EntityPlayerActionPack;

import java.util.*;
import java.util.function.Consumer;

public class FutureActionPack {
	protected Set<ActionType> actions;
	protected float yaw;
	protected float pitch;
	protected int hotbar;

	public Set<ActionType> getActions() {
		return actions;
	}

	public float getYaw() {
		return yaw;
	}

	public float getPitch() {
		return pitch;
	}

	public int getHotbar() {
		return hotbar;
	}

	public FutureActionPack() {
		this.actions = new HashSet<>();
	}

	public void add(ActionType actionType) {
		actions.add(actionType);
	}

	public void remove(ActionType type) {
		actions.remove(type);
	}

	public void copyTo(EntityPlayerActionPack pack) {
		pack.stopAll();
		for (ActionType actionType : actions) {
			actionType.packModifier.accept(pack);
		}
		pack.look(yaw, pitch);
		pack.setSlot(hotbar);
	}

	public void setYaw(float yaw) {
		this.yaw = yaw;
	}

	public void setPitch(float pitch) {
		this.pitch = pitch;
	}

	public void setHotbar(int hotbar) {
		this.hotbar = hotbar;
	}

	public enum ActionType {
		ATTACK(pack -> pack.start(EntityPlayerActionPack.ActionType.ATTACK, EntityPlayerActionPack.Action.continuous())),
		USE(pack -> pack.start(EntityPlayerActionPack.ActionType.USE, EntityPlayerActionPack.Action.continuous())),
		FORWARD(pack -> pack.setForward(1)),
		LEFT(pack -> pack.setStrafing(1)),
		BACKWARD(pack -> pack.setForward(-1)),
		RIGHT(pack -> pack.setStrafing(-1)),
		SPRINT(pack -> pack.setSprinting(true)),
		SNEAK(pack -> pack.setSneaking(true))
		;

		private final Consumer<EntityPlayerActionPack> packModifier;
		ActionType(Consumer<EntityPlayerActionPack> packModifier) {
			this.packModifier = packModifier;
		}
	}
}
