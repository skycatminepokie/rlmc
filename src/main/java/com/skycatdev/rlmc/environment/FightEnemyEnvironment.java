/* Licensed MIT 2025 */
package com.skycatdev.rlmc.environment;

import com.skycatdev.rlmc.Rlmc;
import com.skycatdev.rlmc.SpreadEntitiesHelper;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Function;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameRules;
import net.minecraft.world.dimension.DimensionTypes;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

public class FightEnemyEnvironment extends BasicPlayerEnvironment {
    @Nullable private RuntimeWorldHandle worldHandle;
    @Nullable protected MobEntity enemy;
    protected boolean justKilled;
    @Nullable protected Vec3d startPos;
    protected EntityType<? extends MobEntity> enemyType;

    public FightEnemyEnvironment(ServerPlayerEntity agent, EntityType<? extends MobEntity> enemyType) {
        super(agent, 20, 20, 3, 3);
        justKilled = false;
        this.enemyType = enemyType;
    }

    public static @Nullable Future<FightEnemyEnvironment> makeAndConnect(String agentName, MinecraftServer server, EntityType<? extends MobEntity> entityType) {
        @Nullable CompletableFuture<ServerPlayerEntity> agentFuture = createPlayerAgent(agentName, server, Vec3d.ZERO, server.getOverworld().getRegistryKey());
        if (agentFuture != null) {
            Function<ServerPlayerEntity, FightEnemyEnvironment> environmentFuture = agent -> {
                FightEnemyEnvironment environment = new FightEnemyEnvironment(agent, entityType);
                Rlmc.addEnvironment(environment);
                Rlmc.getPythonEntrypoint().connectEnvironment("fight_enemy", environment);
                return environment;
            };
            return agentFuture.thenApplyAsync(environmentFuture, (runnable) -> new Thread(runnable).start());
        }
        return null;
    }

    @Override
    protected HashMap<String, Object> getInfo(BasicPlayerObservation observation) {
        return new HashMap<>();
    }

    @Override
    protected int getReward(BasicPlayerObservation observation) {
        int damageDealt = agent.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.DAMAGE_DEALT));
        int damageTaken = agent.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.DAMAGE_TAKEN));
        return damageDealt - damageTaken;
    }

    @Override
    protected Vec3d getStartPos() {
        if (startPos == null) {
            throw new IllegalStateException("Something called getStartPos before we were ready!");
        }
        return startPos;
    }

    @Override
    protected ServerWorld getWorld() {
        return getOrCreateWorld();
    }

    protected ServerWorld getOrCreateWorld() {
        if (worldHandle == null) {
            worldHandle = Fantasy.get(Objects.requireNonNull(agent.getServer())).openTemporaryWorld(new RuntimeWorldConfig()
                    .setDimensionType(DimensionTypes.OVERWORLD)
                    .setDifficulty(Difficulty.HARD)
                    .setGameRule(GameRules.DO_DAYLIGHT_CYCLE, false)
                    .setGenerator(Objects.requireNonNull(agent.getServer()).getOverworld().getChunkManager().getChunkGenerator())
                    .setSeed(new Random().nextLong()));
            worldHandle.asWorld().setTimeOfDay(24000L);
        }
        return worldHandle.asWorld();
    }

    @Override
    protected void innerPreReset(@Nullable Integer seed, @Nullable Map<String, Object> options) {
        if (worldHandle != null) {
            worldHandle.delete();
        }
        worldHandle = null;
        PlayerInventory inventory = agent.getInventory();
        inventory.clear();
        inventory.offer(new ItemStack(Items.DIAMOND_SWORD), true);
        inventory.offer(new ItemStack(Items.DIAMOND_AXE), true);
        inventory.setStack(PlayerInventory.OFF_HAND_SLOT, new ItemStack(Items.SHIELD));
        if (enemy != null) {
            enemy.discard();
        }
        enemy = enemyType.spawn(getOrCreateWorld(), BlockPos.ORIGIN, SpawnReason.COMMAND); // TODO: Move spread players usage to custom impl so I don't spawn this first
        if (enemy == null) {
            throw new NullPointerException("Skeleton was null, expected non-null");
        }
        agent.getStatHandler().setStat(agent, Stats.CUSTOM.getOrCreateStat(Stats.DAMAGE_DEALT), 0);
        agent.getStatHandler().setStat(agent, Stats.CUSTOM.getOrCreateStat(Stats.DAMAGE_TAKEN), 0);
        Vec3d spawnPos = Vec3d.of(getWorld().getSpawnPos());
        SpreadEntitiesHelper.spreadEntities(getWorld(),
                new Vec2f((float) spawnPos.getX(), (float) spawnPos.getZ()),
                2.0f,
                2f,
                100,
                false,
                List.of(enemy, agent));
        startPos = agent.getPos(); // TODO: Move to an override of something like teleportToStart
    }



    @Override
    protected boolean isTerminated(BasicPlayerObservation observation) {
        if (enemy == null) {
            throw new RuntimeException("Skeleton should not have been null");
        }
        return checkAndUpdateJustKilled() || enemy.isDead();
    }

    @Override
    protected boolean isTruncated(BasicPlayerObservation observation) {
        return agent.getServerWorld() != getOrCreateWorld();
    }


    @Override
    public void close() {
        super.close();
        worldHandle.delete();
    }
}
