/* Licensed MIT 2025 */
package com.skycatdev.rlmc.environment;

import carpet.helpers.EntityPlayerActionPack;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

@SuppressWarnings("unused") // Python uses it
public class FutureActionPack {
	protected Set<ActionType> actions;
	protected float yaw;
	protected float pitch;
	protected int hotbar;

	public FutureActionPack() {
		this.actions = new HashSet<>();
	}

	public void add(ActionType actionType) {
		actions.add(actionType);
	}

	public void copyTo(EntityPlayerActionPack pack) {
		pack.stopAll();
		for (ActionType actionType : actions) {
			actionType.packModifier.accept(pack);
		}
		pack.look(yaw, pitch);
		pack.setSlot(hotbar + 1);
	}

	public Set<ActionType> getActions() {
		return actions;
	}

	public int getHotbar() {
		return hotbar;
	}

	public void setHotbar(int hotbar) {
		this.hotbar = hotbar;
	}

	public float getPitch() {
		return pitch;
	}

	public void setPitch(float pitch) {
		this.pitch = pitch;
	}

	public float getYaw() {
		return yaw;
	}

	public void setYaw(float yaw) {
		this.yaw = yaw;
	}

	public void remove(ActionType type) {
		actions.remove(type);
	}

	@SuppressWarnings("unused") // Python uses it
	public enum ActionType {
		ATTACK(pack -> pack.start(EntityPlayerActionPack.ActionType.ATTACK, EntityPlayerActionPack.Action.continuous())),
		USE(pack -> pack.start(EntityPlayerActionPack.ActionType.USE, EntityPlayerActionPack.Action.continuous())),
		FORWARD(pack -> pack.setForward(1)),
		LEFT(pack -> pack.setStrafing(1)),
		BACKWARD(pack -> pack.setForward(-1)),
		RIGHT(pack -> pack.setStrafing(-1)),
		SPRINT(pack -> pack.setSprinting(true)),
		SNEAK(pack -> pack.setSneaking(true)),
		JUMP(pack -> pack.start(EntityPlayerActionPack.ActionType.JUMP, EntityPlayerActionPack.Action.continuous()));

		private final Consumer<EntityPlayerActionPack> packModifier;

		ActionType(Consumer<EntityPlayerActionPack> packModifier) {
			this.packModifier = packModifier;
		}
	}
}
