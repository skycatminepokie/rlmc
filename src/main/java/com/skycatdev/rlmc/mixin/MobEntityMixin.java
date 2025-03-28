/* Licensed MIT 2025 */
package com.skycatdev.rlmc.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.skycatdev.rlmc.environment.AgentCandidate;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MobEntity.class)
public abstract class MobEntityMixin implements AgentCandidate {
    @Unique protected boolean rlmc$isAgent = false;

    @Override
    public boolean rlmc$isAgent() {
        return rlmc$isAgent;
    }

    @Override
    public void rlmc$setIsAgent(boolean isAgent) {
        rlmc$isAgent = isAgent;
    }

    @ModifyReturnValue(method = "cannotDespawn", at = @At("RETURN"))
    private boolean rlmc$stopDespawnIfAgent(boolean original) {
        return original || rlmc$isAgent();
    }

    @ModifyReturnValue(method = "canImmediatelyDespawn", at = @At("RETURN"))
    private boolean rlmc$stopImmediateDespawnIfAgent(boolean original) {
        return original || rlmc$isAgent();
    }
}
