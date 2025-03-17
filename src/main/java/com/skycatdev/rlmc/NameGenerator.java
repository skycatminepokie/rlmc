/* Licensed MIT 2025 */
package com.skycatdev.rlmc;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import net.minecraft.server.network.ServerPlayerEntity;

public class NameGenerator {
    private static final HashSet<String> USED_NAMES = new HashSet<>();
    private static final List<String> PREFIXES;
    private static final List<String> SUFFIXES;
    private static final Random RANDOM = new Random();

    static {
        PREFIXES = List.of("Brave", "Dumb", "Silly", "Lame", "The", "Fun", "Crazy", "Gray", "Grey",
                "Purple", "Green", "Blue", "Apt", "Tall", "Short", "Gamer", "Fat");
        SUFFIXES = List.of("Tiger", "Math", "Cheetah", "Leopard", "Chair", "Gamer", "Bee", "Hat", "Cheese",
                "Drink", "Potato", "Table", "Carpet", "Monkey", "Chimp", "Fat", "Nobody");
    }

    public synchronized static String newPlayerName(List<ServerPlayerEntity> playersOnline) {
        Rlmc.LOGGER.debug("Generating new player name");
        List<String> onlineNames = playersOnline.stream()
                .map(player -> player.getGameProfile().getName())
                .toList();
        String name;
        do {
            name = getRandom(PREFIXES) + getRandom(SUFFIXES);
            if (onlineNames.contains(name)) {
                USED_NAMES.add(name);
            }
        } while (USED_NAMES.contains(name));
        USED_NAMES.add(name);
        Rlmc.LOGGER.debug("New player name: {}", name);
        return name;
    }

    protected synchronized static <T> T getRandom(List<T> list) {
        return list.get(RANDOM.nextInt(list.size()));
    }
}
