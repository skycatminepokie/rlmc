/* Licensed MIT 2025 */
package com.skycatdev.rlmc.environment;

import com.skycatdev.rlmc.NameGenerator;
import com.skycatdev.rlmc.Rlmc;
import com.skycatdev.rlmc.SpreadEntitiesHelper;
import com.skycatdev.rlmc.command.EnvironmentSettings;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.function.Function;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public class FightEnemyEnvironment extends BasicPlayerEnvironment<FightEnemyEnvironment.Observation> {
    public final int maxEnemyDistance = 300;
    @Nullable protected MobEntity enemy;
    @Nullable protected Vec3d startPos;
    protected EntityType<? extends MobEntity> enemyType;
    @Nullable private Identifier structure = null;

    public FightEnemyEnvironment(EnvironmentSettings settings, ServerPlayerEntity agent, EntityType<? extends MobEntity> enemyType) {
        super(settings, agent, 20, 20, 3, 3);
        this.enemyType = enemyType;
    }

    public FightEnemyEnvironment(EnvironmentSettings settings, ServerPlayerEntity agent, EntityType<? extends MobEntity> enemyType, Identifier structure) {
        this(settings, agent, enemyType);
        this.structure = structure;
    }

    public static @Nullable Future<FightEnemyEnvironment> makeAndConnect(EnvironmentSettings environmentSettings, String agentName, MinecraftServer server, EntityType<? extends MobEntity> entityType, @Nullable Identifier structure) {
        Rlmc.LOGGER.debug("Creating fight enemy env for \"{}\"", agentName);
        @Nullable CompletableFuture<ServerPlayerEntity> agentFuture = createPlayerAgent(agentName, server, Vec3d.ZERO, server.getOverworld().getRegistryKey());
        if (agentFuture != null) {
            Function<ServerPlayerEntity, FightEnemyEnvironment> environmentFuture = agent -> {
                FightEnemyEnvironment environment;
                if (structure != null) {
                    environment = new FightEnemyEnvironment(environmentSettings, agent, entityType, structure);
                } else {
                    environment = new FightEnemyEnvironment(environmentSettings, agent, entityType);
                }
                Rlmc.addEnvironment(environment);
                Rlmc.getPythonEntrypoint().connectEnvironment("fight_enemy", environment);
                Rlmc.LOGGER.debug("Connected fight enemy env \"{}\"", environment.getUniqueEnvName());
                return environment;
            };
            return agentFuture.thenApplyAsync(environmentFuture, (runnable) -> new Thread(runnable).start());
        }
        return null;
    }

    @Override
    public void close() {
        super.close();
        Objects.requireNonNull(worldHandle).delete();
    }

    @Override
    public Future<Future<? extends Environment<FutureActionPack, Observation>>> makeAnother() {
        Rlmc.LOGGER.trace("Making another FightEnemyEnvironment...");
        FutureTask<Future<? extends Environment<FutureActionPack, Observation>>> futureTask = new FutureTask<>(() -> Objects.requireNonNull(makeAndConnect(settings, NameGenerator.newPlayerName(getWorld().getServer().getPlayerManager().getPlayerList()), getWorld().getServer(), enemyType, structure)));
        Rlmc.runBeforeNextTick(futureTask);
        return futureTask;
    }

    @Override
    protected HashMap<String, Object> getInfo(BasicPlayerObservation observation) {
        return new HashMap<>();
    }


    @SuppressWarnings("unused") // used by wrapped_fight_enemy_environment.py
    public int getMaxEnemyDistance() {
        return maxEnemyDistance;
    }

    @Override
    protected Observation getObservation() {
        return Observation.fromBasic(BasicPlayerObservation.fromPlayer(agent, xRaycasts, yRaycasts, getRaycastDistance(), Math.PI / 2, history), Objects.requireNonNull(enemy), maxEnemyDistance);
    }

    @Override
    protected double getReward(BasicPlayerObservation observation) {
        int damageDealt = agent.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.DAMAGE_DEALT));
        int damageTaken = agent.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.DAMAGE_TAKEN));
        agent.getStatHandler().setStat(agent, Stats.CUSTOM.getOrCreateStat(Stats.DAMAGE_DEALT), 0);
        agent.getStatHandler().setStat(agent, Stats.CUSTOM.getOrCreateStat(Stats.DAMAGE_TAKEN), 0);
        return enemy != null && enemy.isDead() ? (1 - (double) damageTaken / 2) : (damageDealt - (double) damageTaken / 2) / 300d;
    }

    @Override
    protected Vec3d getStartPos() {
        if (startPos == null) {
            throw new IllegalStateException("Something called getStartPos before we were ready!");
        }
        return startPos;
    }

    @Override
    protected void innerPreReset(@Nullable Integer seed, @Nullable Map<String, Object> options) {
        deleteCurrentWorld();
        PlayerInventory inventory = agent.getInventory();
        inventory.clear();
        inventory.offer(new ItemStack(Items.DIAMOND_SWORD), true);
        inventory.offer(new ItemStack(Items.DIAMOND_AXE), true);
        if (enemy != null) {
            enemy.discard();
        }
        enemy = enemyType.spawn(getWorld(), BlockPos.ORIGIN, SpawnReason.COMMAND); // TODO: Move spread players usage to custom impl so I don't waste stuff and things
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
        startPos = enemy.getPos(); // TODO: Move to an override of something like teleportToStart
        if (structure != null) {
            var optTemplate = getWorld().getStructureTemplateManager().getTemplate(structure);
            if (optTemplate.isPresent()) {
                optTemplate.get().place(getWorld(), BlockPos.ofFloored(Objects.requireNonNull(startPos).subtract(6, 1, 6)), BlockPos.ofFloored(startPos), new StructurePlacementData(), net.minecraft.util.math.random.Random.create(), 0);
            } else {
                Rlmc.LOGGER.warn("Tried to place non-existent structure {}, skipping.", structure);
            }
        }
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
        return agent.getServerWorld() != getWorld();
    }

    public static class Observation extends BasicPlayerObservation {
        public final Vec3d vecToEnemy;

        public Observation(List<BlockHitInfo> blocks, List<@Nullable EntityHitResult> entities, ServerPlayerEntity self, FutureActionPack.History history, Vec3d vecToEnemy) {
            super(blocks, entities, self, history);
            this.vecToEnemy = vecToEnemy;
        }

        public static Observation fromBasic(BasicPlayerObservation basic, MobEntity entity, int maxEnemyDistance) {
            Vec3d vecToEnemy = entity.getEyePos().subtract(basic.self().getEyePos());
            Vec3d clamped = new Vec3d(Math.clamp(vecToEnemy.getX(), -maxEnemyDistance, maxEnemyDistance),
                    Math.clamp(vecToEnemy.getY(), -maxEnemyDistance, maxEnemyDistance),
                    Math.clamp(vecToEnemy.getZ(), -maxEnemyDistance, maxEnemyDistance));
            return new Observation(
                    basic.blocks(),
                    basic.entities(),
                    basic.self(),
                    basic.history(),
                    clamped
            );
        }

        @SuppressWarnings("unused") // Used by wrapped_fight_enemy_environment.py
        public Vec3d getVecToEnemy() {
            return vecToEnemy;
        }
    }
}
