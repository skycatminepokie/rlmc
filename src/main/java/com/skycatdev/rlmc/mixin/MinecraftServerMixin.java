/* Licensed MIT 2025 */
package com.skycatdev.rlmc.mixin;

import com.skycatdev.rlmc.Rlmc;
import com.skycatdev.rlmc.environment.Environment;
import java.util.function.BooleanSupplier;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    protected void rlmc$preTick(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        for (Environment<?, ?> environment : Rlmc.getEnvironments()) {
            environment.preTick();
        }
    }

    @Inject(method = "tick", at = @At("RETURN"))
    protected void rlmc$postTick(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        for (Environment<?, ?> environment : Rlmc.getEnvironments()) {
            environment.postTick();
        }
    }
}
