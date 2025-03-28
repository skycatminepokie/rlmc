/* Licensed MIT 2025 */
package com.skycatdev.rlmc.mixin;

import com.skycatdev.rlmc.environment.RlGoal;
import net.minecraft.entity.CrossbowUser;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.InventoryOwner;
import net.minecraft.entity.mob.IllagerEntity;
import net.minecraft.entity.mob.PillagerEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Contract;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PillagerEntity.class)
public abstract class PillagerEntityMixin extends IllagerEntity implements CrossbowUser, InventoryOwner {
    @Contract("_,_->fail")
    protected PillagerEntityMixin(EntityType<? extends IllagerEntity> entityType, World world) {
        super(entityType, world);
        throw new IllegalStateException("Implemented in a mixin");
    }

    @Inject(method = "initGoals", at = @At("HEAD"))
    private void rlmc$addRlGoal(CallbackInfo ci) {
        goalSelector.add(-1, new RlGoal(((PillagerEntity)(Object)this)));
    }
}
