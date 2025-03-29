/* Licensed MIT 2025 */
package com.skycatdev.rlmc.mixin;

import carpet.patches.EntityPlayerMPFake;
import com.skycatdev.rlmc.environment.player.PlayerAgentCandidate;
import java.util.function.Consumer;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityPlayerMPFake.class)
public abstract class EntityPlayerMPFakeMixin implements PlayerAgentCandidate {
    @Shadow public abstract void kill();

    @Unique protected boolean rlmc$isAgent = false;
    @Unique @Nullable protected Consumer<PlayerAgentCandidate> rlmc$onKilledTrigger;

    @Inject(method = "kill(Lnet/minecraft/text/Text;)V", at = @At(value = "INVOKE", target = "Lcarpet/patches/EntityPlayerMPFake;shakeOff()V", shift = At.Shift.AFTER), cancellable = true)
    private void rlmc$stopAgentKill(Text reason, CallbackInfo ci) {
        if (rlmc$isAgent()) {
            if (rlmc$onKilledTrigger != null) {
                rlmc$onKilledTrigger.accept(this);
            }
            ci.cancel();
        }
    }

    @Override
    public boolean rlmc$isAgent() {
        return rlmc$isAgent;
    }

    @Override
    public void rlmc$setIsAgent(boolean isAgent) {
        rlmc$isAgent = isAgent;
    }

    @Override
    public void rlmc$setKilledTrigger(Consumer<PlayerAgentCandidate> trigger) {
        rlmc$onKilledTrigger = trigger;
    }

    @Override
    public void rlmc$forceKill() {
        rlmc$setIsAgent(false);
        kill();
        rlmc$setIsAgent(true);
    }
}
