package me.danterus.orientmc;

import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.*;
import net.minestom.server.instance.*;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.utils.NamespaceID;
import net.minestom.server.world.DimensionType;

import java.util.*;
import java.util.List;

public class Main {

    public static int actualMapSize = 20;
    public static int mapSize = 100;
    public static List<Vec> countourLocations = new ArrayList<>();

    public static void main(String[] args) {
        MinecraftServer minecraftServer = MinecraftServer.init();
        InstanceManager instanceManager = MinecraftServer.getInstanceManager();

        DimensionType dimension = DimensionType.builder(NamespaceID.from("orientmc:main")).ambientLight(2.0f).build();

        MinecraftServer.getDimensionTypeManager().addDimension(dimension);
        InstanceContainer instanceContainer = instanceManager.createInstanceContainer(dimension);

        GlobalEventHandler globalEventHandler = MinecraftServer.getGlobalEventHandler();
        globalEventHandler.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            final Player player = event.getPlayer();
            event.setSpawningInstance(instanceContainer);
            player.setRespawnPoint(new Pos(0, 50, 0));
        });

        globalEventHandler.addListener(PlayerSpawnEvent.class, event -> {
            final Player player = event.getPlayer();

            player.setGameMode(GameMode.CREATIVE);
        });

        instanceContainer.enableAutoChunkLoad(true);

        ChunkLoader chunkLoader = new ChunkLoader(2048);

        instanceContainer.setChunkLoader(chunkLoader);

        minecraftServer.start("0.0.0.0", 25565);
    }
}
