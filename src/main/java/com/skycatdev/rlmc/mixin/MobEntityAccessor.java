/* Licensed MIT 2025 */
package com.skycatdev.rlmc.mixin;

import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MobEntity.class)
public interface MobEntityAccessor {
    @Accessor
    GoalSelector getGoalSelector();
    @Accessor
    void setGoalSelector(GoalSelector goalSelector);
}
