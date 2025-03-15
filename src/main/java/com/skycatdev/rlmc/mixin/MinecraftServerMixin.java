/* Licensed MIT 2025 */
package com.skycatdev.rlmc.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.skycatdev.rlmc.Rlmc;
import com.skycatdev.rlmc.environment.Environment;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    protected void rlmc$preTick(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        //Rlmc.forEachEnvironment(Environment::preTick);
    }

    @Inject(method = "tick", at = @At("RETURN"))
    protected void rlmc$postTick(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        //Rlmc.forEachEnvironment(Environment::postTick);
    }

    @WrapOperation(method = "tickWorlds", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;tick(Ljava/util/function/BooleanSupplier;)V"))
    protected void rlmc$tickWorld(ServerWorld instance, BooleanSupplier shouldKeepTicking, Operation<Void> original) {
        List<Environment<?, ?>> envs = Rlmc.getEnvironments().stream().filter(env -> env.isIn(instance)).toList();
        boolean readyToTick = envs.stream().allMatch(Environment::shouldTick);
        if (readyToTick) {
            envs.forEach(Environment::preTick);
            original.call(instance, shouldKeepTicking);
            envs.forEach(Environment::postTick);
        }
    }
}
