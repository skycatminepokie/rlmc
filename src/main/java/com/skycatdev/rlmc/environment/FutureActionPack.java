/* Licensed MIT 2025 */
package com.skycatdev.rlmc.environment;

import carpet.helpers.EntityPlayerActionPack;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.jetbrains.annotations.Contract;

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
        pack.stopAll(); // TODO Cannot actually do continuous actions because of this call
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
        ATTACK(pack -> pack.start(EntityPlayerActionPack.ActionType.ATTACK, EntityPlayerActionPack.Action.once())),
        USE(pack -> pack.start(EntityPlayerActionPack.ActionType.USE, EntityPlayerActionPack.Action.once())),
        FORWARD(pack -> pack.setForward(1)),
        LEFT(pack -> pack.setStrafing(1)),
        BACKWARD(pack -> pack.setForward(-1)),
        RIGHT(pack -> pack.setStrafing(-1)),
        SPRINT(pack -> pack.setSprinting(true)),
        SNEAK(pack -> pack.setSneaking(true)),
        JUMP(pack -> pack.start(EntityPlayerActionPack.ActionType.JUMP, EntityPlayerActionPack.Action.once()));

        private final Consumer<EntityPlayerActionPack> packModifier;

        ActionType(Consumer<EntityPlayerActionPack> packModifier) {
            this.packModifier = packModifier;
        }
    }

    public static class History {
        /**
         * Map of ActionType : Number of steps active in a row.
         * A negative number indicates it has been inactive for that many steps.
         * It maxes out at 1200 steps.
         */
        public Map<ActionType, Integer> actionHistory;
        public int yaw;
        public int pitch;

        public History() {
            this.actionHistory = new HashMap<>();
        }

        public Map<ActionType, Integer> getActionHistory() {
            return actionHistory;
        }

        public int getPitch() {
            return pitch;
        }

        public int getYaw() {
            return yaw;
        }

        @Contract("_->this")
        public History step(FutureActionPack futureActionPack) {
            for (ActionType actionType : ActionType.values()) {
                if (futureActionPack.getActions().contains(actionType)) {
                    actionHistory.computeIfPresent(actionType, (action, value) -> Math.min(value + 1, 1200));
                    actionHistory.putIfAbsent(actionType, 1);
                } else {
                    actionHistory.computeIfPresent(actionType, (action, value) -> Math.max(value - 1, -1200));
                    actionHistory.putIfAbsent(actionType, -1);
                }
            }
            yaw = (int) futureActionPack.getYaw();
            pitch = (int) futureActionPack.getPitch();
            return this;
        }
    }
}
