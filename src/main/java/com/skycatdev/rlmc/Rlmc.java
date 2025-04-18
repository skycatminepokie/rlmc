/* Licensed MIT 2025 */
package com.skycatdev.rlmc;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.skycatdev.rlmc.command.CommandManager;
import com.skycatdev.rlmc.environment.Environment;
import com.skycatdev.rlmc.network.DrawVectorPayload;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.state.StateManager;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py4j.GatewayServer;
import py4j.GatewayServerListener;
import py4j.Py4JServerConnection;

public class Rlmc implements ModInitializer {
    public static final String MOD_ID = "rlmc";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final Logger PYTHON_LOGGER = LoggerFactory.getLogger(MOD_ID + "_python");
    private static final GatewayServer GATEWAY_SERVER = new GatewayServer();
    private static final List<Environment<?, ?>> ENVIRONMENTS = new CopyOnWriteArrayList<>();
    private static @Nullable BiMap<EntityType<?>, Integer> ENTITY_TYPE_MAP = null;
    private static @Nullable BiMap<Item, Integer> ITEM_MAP = null;
    private static @Nullable BiMap<BlockState, Integer> BLOCK_STATE_MAP = null;
    private static final Collection<Runnable> RUN_BEFORE_NEXT_TICK = new LinkedList<>();

    static {
        new Thread(() -> GATEWAY_SERVER.start(false), "RLMC Python Gateway Server Thread").start();
    }

    public static boolean addEnvironment(Environment<?, ?> environment) {
        return ENVIRONMENTS.add(environment);
    }

    public static void forEachEnvironment(Consumer<? super Environment<?, ?>> consumer) {
        ENVIRONMENTS.forEach(consumer);
    }

    public static List<Environment<?, ?>> getEnvironments() {
        return ENVIRONMENTS;
    }

    @SuppressWarnings("unused") // Used by block_hit_result.py
    public static BiMap<BlockState, Integer> getBlockStateMap() {
        if (BLOCK_STATE_MAP == null) {
            List<BlockState> blockStates = Registries.BLOCK.stream()
                    .filter(block -> Registries.BLOCK.getId(block).getNamespace().equals("minecraft"))
                    .map(Block::getStateManager)
                    .map(StateManager::getStates)
                    .flatMap(Collection::stream)
                    .toList();
            BLOCK_STATE_MAP = HashBiMap.create(blockStates.size());
            for (int i = 0; i < blockStates.size(); i++) {
                BLOCK_STATE_MAP.put(blockStates.get(i), i);
            }
        }
        return BLOCK_STATE_MAP;
    }

    @SuppressWarnings("unused") // Used by entity_hit_result.py
    public static BiMap<EntityType<?>, Integer> getEntityTypeMap() {
        if (ENTITY_TYPE_MAP == null) {
            List<EntityType<?>> entityTypes = Registries.ENTITY_TYPE.stream()
                    .filter(type -> Registries.ENTITY_TYPE.getId(type).getNamespace().equals("minecraft"))
                    .sorted(Comparator.comparing(o -> Registries.ENTITY_TYPE.getId(o).getPath()))
                    .toList();
            ENTITY_TYPE_MAP = HashBiMap.create(entityTypes.size());
            for (int i = 0; i < entityTypes.size(); i++) {
                ENTITY_TYPE_MAP.put(entityTypes.get(i), i);
            }
        }
        return ENTITY_TYPE_MAP;
    }

    public static BiMap<Item, Integer> getItemMap() {
        if (ITEM_MAP == null) {
            List<Item> items = Registries.ITEM.stream()
                    .filter(item -> Registries.ITEM.getId(item).getNamespace().equals("minecraft"))
                    .sorted(Comparator.comparing(o -> Registries.ITEM.getId(o).getPath()))
                    .toList();
            ITEM_MAP = HashBiMap.create(items.size());
            for (int i = 0; i < items.size(); i++) {
                ITEM_MAP.put(items.get(i), i);
            }
        }
        return ITEM_MAP;
    }

    public static PythonEntrypoint getPythonEntrypoint() {
        return (PythonEntrypoint) GATEWAY_SERVER.getPythonServerEntryPoint(new Class[]{PythonEntrypoint.class});
    }

    public static void runBeforeNextTick(Runnable runnable) {
        synchronized (RUN_BEFORE_NEXT_TICK) {
            RUN_BEFORE_NEXT_TICK.add(runnable);
        }
    }

    public static void preTick() {
        synchronized (RUN_BEFORE_NEXT_TICK) {
            RUN_BEFORE_NEXT_TICK.forEach(Runnable::run);
            RUN_BEFORE_NEXT_TICK.clear();
        }
    }

    @SuppressWarnings("unused") // Used by entrypoint.py
    public static void pythonLog(String level, String message) {
        switch (level.toUpperCase()) {
            case "DEBUG":
                PYTHON_LOGGER.debug(message);
                break;
            case "WARN":
                PYTHON_LOGGER.warn(message);
                break;
            case "ERROR":
                PYTHON_LOGGER.error(message);
                break;
            default:
                PYTHON_LOGGER.info(message);
        }
    }

    public static boolean removeEnvironment(Environment<?, ?> environment) {
        return ENVIRONMENTS.remove(environment);
    }

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playS2C().register(DrawVectorPayload.PACKET_ID, DrawVectorPayload.CODEC);
        CommandRegistrationCallback.EVENT.register(new CommandManager());
        GATEWAY_SERVER.addListener(new GatewayServerListener() {
            @Override
            public void connectionError(Exception e) {
                LOGGER.warn("Py4J Connection Error. Printing stack trace.", e);
            }

            @Override
            public void connectionStarted(Py4JServerConnection gatewayConnection) {
                LOGGER.debug("Py4J connection started");
            }

            @Override
            public void connectionStopped(Py4JServerConnection gatewayConnection) {
                LOGGER.debug("Py4J connection stopped");
            }

            @Override
            public void serverError(Exception e) {
                LOGGER.warn("Py4J Server Error. Print stack trace.", e);
            }

            @Override
            public void serverPostShutdown() {
                LOGGER.debug("Py4J server post shutdown");
            }

            @Override
            public void serverPreShutdown() {
                LOGGER.debug("Py4J server pre shutdown");
            }

            @Override
            public void serverStarted() {
                LOGGER.debug("Py4J server started");
            }

            @Override
            public void serverStopped() {
                LOGGER.debug("Py4J server stopped");
            }
        });
    }

}