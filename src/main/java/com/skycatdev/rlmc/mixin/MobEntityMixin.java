/* Licensed MIT 2025 */
package com.skycatdev.rlmc.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.skycatdev.rlmc.environment.MobAgentCandidate;
import java.util.function.Consumer;
import net.minecraft.entity.mob.MobEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(MobEntity.class)
public abstract class MobEntityMixin implements MobAgentCandidate {
    @Unique protected boolean rlmc$isAgent = false;
    @Unique @Nullable protected Consumer<MobEntity> rlmc$aiCallback = null;

    @Override
    public void rlmc$setIsAgent(boolean isAgent) {
        rlmc$isAgent = isAgent;
    }

    @Override
    public boolean rlmc$isAgent() {
        return rlmc$isAgent;
    }

    @Override
    public void rlmc$setAiCallback(Consumer<MobEntity> callback) {
        rlmc$aiCallback = callback;
    }

    @WrapMethod(method = "tickNewAi")
    private void tickNewAi(Operation<Void> original) {
        if (!rlmc$isAgent) {
            original.call();
        } else {
            if (rlmc$aiCallback != null) {
                rlmc$aiCallback.accept((MobEntity)(Object)this);
            }
        }
    }

}
