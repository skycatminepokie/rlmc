/* Licensed MIT 2025 */
package com.skycatdev.rlmc.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.skycatdev.rlmc.environment.AgentCandidate;
import com.skycatdev.rlmc.environment.DamageTracked;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntity.class)
public class LivingEntityMixin implements AgentCandidate, DamageTracked {
    @Unique protected boolean rlmc$isAgent = false;
    @Unique protected boolean rlmc$trackingDamage = false;
    @Unique protected float rlmc$damageTaken = 0;

    @Override
    public boolean rlmc$isTrackingDamage() {
        return rlmc$trackingDamage;
    }

    @Override
    public float rlmc$getDamageTaken() {
        return rlmc$damageTaken;
    }

    @Override
    public void rlmc$setDamageTaken(float damage) {
        rlmc$damageTaken = damage;
    }

    @Override
    public void rlmc$setDamageTracking(boolean tracking) {
        rlmc$trackingDamage = tracking;
    }

    @Override
    public boolean rlmc$isAgent() {
        return rlmc$isAgent;
    }

    @Override
    public void rlmc$setIsAgent(boolean isAgent) {
        rlmc$isAgent = isAgent;
    }

    @WrapMethod(method = "damage")
    private boolean rlmc$onDamage(DamageSource source, float amount, Operation<Boolean> original) {
        boolean ret = original.call(source, amount);
        if (ret) {
            rlmc$trackIfTracking(amount);
        }
        return ret;
    }
}
