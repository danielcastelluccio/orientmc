package me.danterus.orientmc;

import it.unimi.dsi.fastutil.Pair;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.player.*;
import net.minestom.server.instance.*;
import net.minestom.server.instance.block.Block;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.metadata.MapMeta;
import net.minestom.server.map.framebuffers.Graphics2DFramebuffer;
import net.minestom.server.network.packet.server.play.MapDataPacket;
import net.minestom.server.utils.NamespaceID;
import net.minestom.server.world.DimensionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {

    public static int actualMapSize = 20;
    public static int mapSize = 100;
    public static List<Vec> countourLocations = new ArrayList<>();

    public static void main(String[] args) {
        MinecraftServer minecraftServer = MinecraftServer.init();
        InstanceManager instanceManager = MinecraftServer.getInstanceManager();

        Random random = new Random(0);

        DimensionType dimension = DimensionType.builder(NamespaceID.from("orientmc:main")).ambientLight(2.0f).build();

        MinecraftServer.getDimensionTypeManager().addDimension(dimension);
        InstanceContainer instanceContainer = instanceManager.createInstanceContainer(dimension);

        int mapId = 1;

        GlobalEventHandler globalEventHandler = MinecraftServer.getGlobalEventHandler();
        globalEventHandler.addListener(PlayerLoginEvent.class, event -> {
            final Player player = event.getPlayer();
            event.setSpawningInstance(instanceContainer);
            player.setRespawnPoint(new Pos(-233, 50, -43));

            ItemStack map = ItemStack.builder(Material.FILLED_MAP)
                    .meta(new MapMeta.Builder().mapId(mapId).build()).build();
            player.getInventory().setItemStack(0, map);
            player.setGameMode(GameMode.ADVENTURE);
            player.setFlying(true);
        });

        List<Vec> checkpoints = new ArrayList<>();
        AtomicInteger currentCheckpoint = new AtomicInteger();

        instanceContainer.enableAutoChunkLoad(true);

        int size = 2048;
        List<Polygon> contours = new ArrayList<>();
        List<Polygon> formLines = new ArrayList<>();
        List<Polygon> waters = new ArrayList<>();
        List<Polygon> roughOpens = new ArrayList<>();
        List<Polygon> opens = new ArrayList<>();
        List<Polygon> vegetationFights = new ArrayList<>();
        List<Polygon> outOfBounds = new ArrayList<>();
        List<Polygon> buildings = new ArrayList<>();
        List<Polygon> concretes = new ArrayList<>();
        List<List<Point>> concreteLines = new ArrayList<>();
        List<List<Point>> roads = new ArrayList<>();
        List<List<Point>> fences = new ArrayList<>();
        List<List<Point>> badRoads = new ArrayList<>();
        List<List<Point>> footPaths = new ArrayList<>();
        List<Point> boulders = new ArrayList<>();
        List<Point> knolls = new ArrayList<>();
        List<List<Point>> cliffs = new ArrayList<>();
        List<List<Point>> powerLines = new ArrayList<>();

        MinecraftServer.getSchedulerManager().scheduleNextTick(() -> {
            try {
                JSONObject object = new JSONObject(Files.readString(Path.of("Sodra-Styrso-big.json")));

                JSONArray features = object.getJSONArray("features");

                double minX = Integer.MAX_VALUE;
                double minY = Integer.MAX_VALUE;
                double maxX = 0;
                double maxY = 0;

                for (Object featureOld : features) {
                    JSONObject feature = (JSONObject) featureOld;

                    JSONObject geometry = feature.getJSONObject("geometry");
                    String type = geometry.getString("type");

                    Object coordinatesBase = geometry.get("coordinates");
                    if (type.equals("Point")) {
                        JSONArray coordinates = (JSONArray) coordinatesBase;
                        minX = Math.min(coordinates.getDouble(0), minX);
                        minY = Math.min(coordinates.getDouble(1), minY);
                        maxX = Math.max(coordinates.getDouble(0), maxX);
                        maxY = Math.max(coordinates.getDouble(1), maxY);
                    }
                }

                for (Object featureOld : features) {
                    JSONObject feature = (JSONObject) featureOld;

                    JSONObject properties = feature.getJSONObject("properties");

                    if (!properties.has("sym")) {
                        continue;
                    }

                    int symbol = properties.getInt("sym");
                    JSONObject geometry = feature.getJSONObject("geometry");

                    int BOULDER = 204000;
                    int CONTOUR = 101005;
                    int CONTOUR2 = 101004;
                    int CONTOUR3 = 101006;
                    int FORM_LINE = 103000;
                    int INDEX_CONTOUR = 102000;
                    int WATER = 301000;
                    int ROUGH_OPEN = 403000;
                    int OPEN = 401000;
                    int KNOLL = 109000;
                    int CLIFF = 201002;
                    int BUILDING = 521000;
                    int CONCRETE_ROAD = 502002;
                    int ROAD = 503000;
                    int UNMAINTAINED_ROAD = 504000;
                    int FOOTPATH = 505000;
                    int CONCRETE = 501001;
                    int CONCRETE2 = 501002;
                    int OUT_OF_BOUNDS = 520000;
                    int FENCE = 516000;
                    int VEGETATION_FIGHT = 410000;
                    int POWER_LINE = 510000;

                    Object coordinatesBase = geometry.get("coordinates");
                    if (symbol == BOULDER) {
                        Pair<Double, Double> coordinates = getPointCoordinates(coordinatesBase);

                        double relativeX = 1 - ((coordinates.left() - minX) / (maxX - minX));
                        double relativeY = (coordinates.right() - minY) / (maxY - minY);

                        boulders.add(new Point((int) (relativeX * size - size/2), (int) (relativeY * size - size/2)));
                    } else if (symbol == KNOLL) {
                        Pair<Double, Double> coordinates = getPointCoordinates(coordinatesBase);

                        double relativeX = 1 - ((coordinates.left() - minX) / (maxX - minX));
                        double relativeY = (coordinates.right() - minY) / (maxY - minY);

                        knolls.add(new Point((int) (relativeX * size - size/2), (int) (relativeY * size - size/2)));
                    } else if (symbol == CONTOUR || symbol == CONTOUR2 || symbol == CONTOUR3 || symbol == INDEX_CONTOUR) {
                        List<Double> xPoints = new ArrayList<>();
                        List<Double> yPoints = new ArrayList<>();

                        List<Pair<Double, Double>> coordinates = getLineStringCoordinates(coordinatesBase);

                        for (Pair<Double, Double> coordinate : coordinates) {
                            double relativeX = ((coordinate.left() - minX) / (maxX - minX));
                            double relativeY = 1 - (coordinate.right() - minY) / (maxY - minY);

                            xPoints.add(1 - relativeX);
                            yPoints.add(1 - relativeY);
                        }

                        Polygon polygon = new Polygon(xPoints.stream().mapToInt(i -> (int) (i * size - size/2)).toArray(), yPoints.stream().mapToInt(i -> (int) (i * size - size/2)).toArray(), xPoints.size());
                        contours.add(polygon);
                    } else if (symbol == FORM_LINE) {
                        List<Double> xPoints = new ArrayList<>();
                        List<Double> yPoints = new ArrayList<>();

                        List<Pair<Double, Double>> coordinates = getLineStringCoordinates(coordinatesBase);

                        for (Pair<Double, Double> coordinate : coordinates) {
                            double relativeX = ((coordinate.left() - minX) / (maxX - minX));
                            double relativeY = 1 - (coordinate.right() - minY) / (maxY - minY);

                            xPoints.add(1 - relativeX);
                            yPoints.add(1 - relativeY);
                        }

                        Polygon polygon = new Polygon(xPoints.stream().mapToInt(i -> (int) (i * size - size/2)).toArray(), yPoints.stream().mapToInt(i -> (int) (i * size - size/2)).toArray(), xPoints.size());
                        formLines.add(polygon);
                    } else if (symbol == WATER || symbol == 301001) {
                        List<Double> xPoints = new ArrayList<>();
                        List<Double> yPoints = new ArrayList<>();

                        List<Pair<Double, Double>> coordinates = getPolygonCoordinates(coordinatesBase);

                        for (Pair<Double, Double> coordinate : coordinates) {
                            double relativeX = ((coordinate.left() - minX) / (maxX - minX));
                            double relativeY = 1 - (coordinate.right() - minY) / (maxY - minY);

                            xPoints.add(1 - relativeX);
                            yPoints.add(1 - relativeY);
                        }

                        Polygon polygon = new Polygon(xPoints.stream().mapToInt(i -> (int) (i * size - size/2)).toArray(), yPoints.stream().mapToInt(i -> (int) (i * size - size/2)).toArray(), xPoints.size());
                        waters.add(polygon);
                    } else if (symbol == ROUGH_OPEN) {
                        List<Double> xPoints = new ArrayList<>();
                        List<Double> yPoints = new ArrayList<>();

                        List<Pair<Double, Double>> coordinates = getPolygonCoordinates(coordinatesBase);

                        for (Pair<Double, Double> coordinate : coordinates) {
                            double relativeX = ((coordinate.left() - minX) / (maxX - minX));
                            double relativeY = 1 - (coordinate.right() - minY) / (maxY - minY);

                            xPoints.add(1 - relativeX);
                            yPoints.add(1 - relativeY);
                        }

                        Polygon polygon = new Polygon(xPoints.stream().mapToInt(i -> (int) (i * size - size/2)).toArray(), yPoints.stream().mapToInt(i -> (int) (i * size - size/2)).toArray(), xPoints.size());
                        roughOpens.add(polygon);
                    } else if (symbol == OPEN) {
                        List<Double> xPoints = new ArrayList<>();
                        List<Double> yPoints = new ArrayList<>();

                        List<Pair<Double, Double>> coordinates = getPolygonCoordinates(coordinatesBase);

                        for (Pair<Double, Double> coordinate : coordinates) {
                            double relativeX = ((coordinate.left() - minX) / (maxX - minX));
                            double relativeY = 1 - (coordinate.right() - minY) / (maxY - minY);

                            xPoints.add(1 - relativeX);
                            yPoints.add(1 - relativeY);
                        }

                        Polygon polygon = new Polygon(xPoints.stream().mapToInt(i -> (int) (i * size - size/2)).toArray(), yPoints.stream().mapToInt(i -> (int) (i * size - size/2)).toArray(), xPoints.size());
                        opens.add(polygon);
                    } else if (symbol == VEGETATION_FIGHT) {
                        List<Double> xPoints = new ArrayList<>();
                        List<Double> yPoints = new ArrayList<>();

                        List<Pair<Double, Double>> coordinates = getPolygonCoordinates(coordinatesBase);

                        for (Pair<Double, Double> coordinate : coordinates) {
                            double relativeX = ((coordinate.left() - minX) / (maxX - minX));
                            double relativeY = 1 - (coordinate.right() - minY) / (maxY - minY);

                            xPoints.add(1 - relativeX);
                            yPoints.add(1 - relativeY);
                        }

                        Polygon polygon = new Polygon(xPoints.stream().mapToInt(i -> (int) (i * size - size/2)).toArray(), yPoints.stream().mapToInt(i -> (int) (i * size - size/2)).toArray(), xPoints.size());
                        vegetationFights.add(polygon);
                    } else if (symbol == OUT_OF_BOUNDS) {
                        List<Double> xPoints = new ArrayList<>();
                        List<Double> yPoints = new ArrayList<>();

                        List<Pair<Double, Double>> coordinates = getPolygonCoordinates(coordinatesBase);

                        for (Pair<Double, Double> coordinate : coordinates) {
                            double relativeX = ((coordinate.left() - minX) / (maxX - minX));
                            double relativeY = 1 - (coordinate.right() - minY) / (maxY - minY);

                            xPoints.add(1 - relativeX);
                            yPoints.add(1 - relativeY);
                        }

                        Polygon polygon = new Polygon(xPoints.stream().mapToInt(i -> (int) (i * size - size/2)).toArray(), yPoints.stream().mapToInt(i -> (int) (i * size - size/2)).toArray(), xPoints.size());
                        outOfBounds.add(polygon);
                    } else if (symbol == CLIFF) {
                        List<Point> coordinatesNew = new ArrayList<>();

                        List<Pair<Double, Double>> coordinates = getLineStringCoordinates(coordinatesBase);

                        for (Pair<Double, Double> coordinate : coordinates) {
                            double relativeX = ((coordinate.left() - minX) / (maxX - minX));
                            double relativeY = 1 - (coordinate.right() - minY) / (maxY - minY);

                            coordinatesNew.add(new Point((int) ((1 - relativeX) * size - size/2), (int) ((1 - relativeY) * size - size/2)));
                        }

                        cliffs.add(coordinatesNew);
                    } else if (symbol == POWER_LINE) {
                        List<Point> coordinatesNew = new ArrayList<>();

                        List<Pair<Double, Double>> coordinates = getLineStringCoordinates(coordinatesBase);

                        for (Pair<Double, Double> coordinate : coordinates) {
                            double relativeX = ((coordinate.left() - minX) / (maxX - minX));
                            double relativeY = 1 - (coordinate.right() - minY) / (maxY - minY);

                            coordinatesNew.add(new Point((int) ((1 - relativeX) * size - size/2), (int) ((1 - relativeY) * size - size/2)));
                        }

                        powerLines.add(coordinatesNew);
                    } else if (symbol == FENCE) {
                        List<Point> coordinatesNew = new ArrayList<>();

                        List<Pair<Double, Double>> coordinates = getLineStringCoordinates(coordinatesBase);

                        for (Pair<Double, Double> coordinate : coordinates) {
                            double relativeX = ((coordinate.left() - minX) / (maxX - minX));
                            double relativeY = 1 - (coordinate.right() - minY) / (maxY - minY);

                            coordinatesNew.add(new Point((int) ((1 - relativeX) * size - size/2), (int) ((1 - relativeY) * size - size/2)));
                        }

                        fences.add(coordinatesNew);
                    } else if (symbol == BUILDING) {
                        List<Double> xPoints = new ArrayList<>();
                        List<Double> yPoints = new ArrayList<>();

                        List<Pair<Double, Double>> coordinates = getPolygonCoordinates(coordinatesBase);

                        for (Pair<Double, Double> coordinate : coordinates) {
                            double relativeX = ((coordinate.left() - minX) / (maxX - minX));
                            double relativeY = 1 - (coordinate.right() - minY) / (maxY - minY);

                            xPoints.add(1 - relativeX);
                            yPoints.add(1 - relativeY);
                        }

                        Polygon polygon = new Polygon(xPoints.stream().mapToInt(i -> (int) (i * size - size/2)).toArray(), yPoints.stream().mapToInt(i -> (int) (i * size - size/2)).toArray(), xPoints.size());
                        buildings.add(polygon);
                    } else if (symbol == CONCRETE) {
                        List<Double> xPoints = new ArrayList<>();
                        List<Double> yPoints = new ArrayList<>();

                        List<Pair<Double, Double>> coordinates = getPolygonCoordinates(coordinatesBase);

                        for (Pair<Double, Double> coordinate : coordinates) {
                            double relativeX = ((coordinate.left() - minX) / (maxX - minX));
                            double relativeY = 1 - (coordinate.right() - minY) / (maxY - minY);

                            xPoints.add(1 - relativeX);
                            yPoints.add(1 - relativeY);
                        }

                        Polygon polygon = new Polygon(xPoints.stream().mapToInt(i -> (int) (i * size - size/2)).toArray(), yPoints.stream().mapToInt(i -> (int) (i * size - size/2)).toArray(), xPoints.size());
                        concretes.add(polygon);
                    } else if (symbol == CONCRETE2 || symbol == CONCRETE_ROAD) {
                        List<Point> coordinatesNew = new ArrayList<>();

                        List<Pair<Double, Double>> coordinates = getLineStringCoordinates(coordinatesBase);

                        for (Pair<Double, Double> coordinate : coordinates) {
                            double relativeX = ((coordinate.left() - minX) / (maxX - minX));
                            double relativeY = 1 - (coordinate.right() - minY) / (maxY - minY);

                            coordinatesNew.add(new Point((int) ((1 - relativeX) * size - size/2), (int) ((1 - relativeY) * size - size/2)));
                        }

                        concreteLines.add(coordinatesNew);
                    } else if (symbol == ROAD) {
                        List<Point> coordinatesNew = new ArrayList<>();

                        List<Pair<Double, Double>> coordinates = getLineStringCoordinates(coordinatesBase);

                        for (Pair<Double, Double> coordinate : coordinates) {
                            double relativeX = ((coordinate.left() - minX) / (maxX - minX));
                            double relativeY = 1 - (coordinate.right() - minY) / (maxY - minY);

                            coordinatesNew.add(new Point((int) ((1 - relativeX) * size - size/2), (int) ((1 - relativeY) * size - size/2)));
                        }

                        roads.add(coordinatesNew);
                    } else if (symbol == UNMAINTAINED_ROAD) {
                        List<Point> coordinatesNew = new ArrayList<>();

                        List<Pair<Double, Double>> coordinates = getLineStringCoordinates(coordinatesBase);

                        for (Pair<Double, Double> coordinate : coordinates) {
                            double relativeX = ((coordinate.left() - minX) / (maxX - minX));
                            double relativeY = 1 - (coordinate.right() - minY) / (maxY - minY);

                            coordinatesNew.add(new Point((int) ((1 - relativeX) * size - size/2), (int) ((1 - relativeY) * size - size/2)));
                        }

                        badRoads.add(coordinatesNew);
                    } else if (symbol == FOOTPATH) {
                        List<Point> coordinatesNew = new ArrayList<>();

                        List<Pair<Double, Double>> coordinates = getLineStringCoordinates(coordinatesBase);

                        for (Pair<Double, Double> coordinate : coordinates) {
                            double relativeX = ((coordinate.left() - minX) / (maxX - minX));
                            double relativeY = 1 - (coordinate.right() - minY) / (maxY - minY);

                            coordinatesNew.add(new Point((int) ((1 - relativeX) * size - size/2), (int) ((1 - relativeY) * size - size/2)));
                        }

                        footPaths.add(coordinatesNew);
                    }
                }
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        instanceContainer.setChunkLoader(new IChunkLoader() {
            @Override
            public @NotNull CompletableFuture<@Nullable Chunk> loadChunk(@NotNull Instance instance, int chunkX, int chunkZ) {
                return CompletableFuture.supplyAsync(() -> {
                    Chunk chunk = new DynamicChunk(instance, chunkX, chunkZ);

                    int[][] heightMap = new int[16][16];

                    if (chunkX - 1 < -size/2 / 16) {

                    } else if (chunkZ - 1 < -size/2 / 16) {

                    } else {
                        for (int i = 0; i < 16; i++) {
                            for (int j = 0; j < 16; j++) {
                                int k = 0;
                                for (Polygon polygon : contours) {
                                    if (polygon.contains(chunkX * 16 + i, chunkZ * 16 + j)) {
                                        k += 2;
                                    }
                                }

                                for (Polygon polygon : formLines) {
                                    if (polygon.contains(chunkX * 16 + i, chunkZ * 16 + j)) {
                                        k++;
                                    }
                                }

                                heightMap[i][j] = k + 1;
                            }
                        }

                        for (int k = 0; k < 5; k++) {
                            for (int i = 0; i < 16; i += 1) {
                                for (int j = 0; j < 16; j += 1) {
                                    if (i < 15 && heightMap[i + 1][j] - 2 >= heightMap[i][j]) {
                                        heightMap[i][j] += 1;
                                    } else if (i > 0 && heightMap[i - 1][j] - 2 >= heightMap[i][j]) {
                                        heightMap[i][j] += 1;
                                    } else if (i < 15 && heightMap[i + 1][j] + 2 <= heightMap[i][j]) {
                                        heightMap[i][j] -= 1;
                                    } else if (i > 0 && heightMap[i - 1][j] + 2 <= heightMap[i][j]) {
                                        heightMap[i][j] -= 1;
                                    }

                                    if (j < 15 && heightMap[i][j + 1] - 2 >= heightMap[i][j]) {
                                        heightMap[i][j] += 1;
                                    } else if (j > 0 && heightMap[i][j - 1] - 2 >= heightMap[i][j]) {
                                        heightMap[i][j] += 1;
                                    } else if (j < 15 && heightMap[i][j + 1] + 2 <= heightMap[i][j]) {
                                        heightMap[i][j] -= 1;
                                    } else if (j > 0 && heightMap[i][j - 1] + 2 <= heightMap[i][j]) {
                                        heightMap[i][j] -= 1;
                                    }
                                }
                            }
                        }

                        for (int i = 0; i < 16; i++) {
                            for (int j = 0; j < 16; j++) {
                                int k = heightMap[i][j];

                                for (int l = 0; l < k; l++) {
                                    chunk.setBlock(chunkX * 16 + i, 39 + l, chunkZ * 16 + j, Block.STONE);
                                }

                                String type = "";

                                for (Polygon polygon : roughOpens) {
                                    if (polygon.contains(chunkX * 16 + i, chunkZ * 16 + j)) {
                                        type = "roughOpen";
                                    }
                                }

                                for (Polygon polygon : opens) {
                                    if (polygon.contains(chunkX * 16 + i, chunkZ * 16 + j)) {
                                        type = "open";
                                    }
                                }

                                for (Polygon polygon : vegetationFights) {
                                    if (polygon.contains(chunkX * 16 + i, chunkZ * 16 + j)) {
                                        type = "vegetationFight";
                                    }
                                }

                                for (Polygon polygon : outOfBounds) {
                                    if (polygon.contains(chunkX * 16 + i, chunkZ * 16 + j)) {
                                        type = "outOfBounds";
                                    }
                                }

                                for (Polygon polygon : waters) {
                                    if (polygon.contains(chunkX * 16 + i, chunkZ * 16 + j)) {
                                        type = "water";
                                    }
                                }

                                switch (type) {
                                    case "roughOpen" -> {
                                        Block block = Block.GRASS_BLOCK;
                                        if (random.nextInt(4) == 0) {
                                            block = Block.DIRT;
                                            chunk.setBlock(chunkX * 16 + i, 40 + k, chunkZ * 16 + j, Block.TALL_GRASS);
                                        } else if (random.nextInt(4) == 0) {
                                            block = Block.COARSE_DIRT;
                                        } else if (random.nextInt(8) == 0) {
                                            block = Block.DIRT_PATH;
                                        }
                                        chunk.setBlock(chunkX * 16 + i, 39 + k, chunkZ * 16 + j, block);
                                        if (random.nextInt(8) == 0) {
                                            if (random.nextInt(4) == 0) {
                                                chunk.setBlock(chunkX * 16 + i, 40 + k, chunkZ * 16 + j, Block.TALL_GRASS.withProperties(Map.of("half", "lower")));
                                                chunk.setBlock(chunkX * 16 + i, 41 + k, chunkZ * 16 + j, Block.TALL_GRASS.withProperties(Map.of("half", "upper")));
                                            } else {
                                                chunk.setBlock(chunkX * 16 + i, 40 + k, chunkZ * 16 + j, Block.GRASS);
                                            }
                                        }
                                    }
                                    case "outOfBounds" -> {
                                        chunk.setBlock(chunkX * 16 + i, 39 + k, chunkZ * 16 + j, Block.DIRT);
                                        int size = (int) (Math.pow(random.nextInt(4), 2) / 4) + 1;
                                        for (int l = 0; l < size; l++) {
                                            chunk.setBlock(chunkX * 16 + i, 39 + k + l + 1, chunkZ * 16 + j, Block.OAK_LEAVES);
                                        }
                                    }
                                    case "open" -> {
                                        chunk.setBlock(chunkX * 16 + i, 39 + k, chunkZ * 16 + j, Block.GRASS_BLOCK);
                                        if (random.nextInt(16) == 0) {
                                            chunk.setBlock(chunkX * 16 + i, 40 + k, chunkZ * 16 + j, Block.GRASS);
                                        }
                                    }
                                    case "water" -> {
                                        chunk.setBlock(chunkX * 16 + i, 40, chunkZ * 16 + j, Block.WATER);
                                        chunk.setBlock(chunkX * 16 + i, 39, chunkZ * 16 + j, Block.SAND);
                                    }
                                    case "vegetationFight" -> {
                                        if (random.nextBoolean()) {
                                            chunk.setBlock(chunkX * 16 + i, 39 + k, chunkZ * 16 + j, Block.DIRT);
                                        } else {
                                            chunk.setBlock(chunkX * 16 + i, 39 + k, chunkZ * 16 + j, Block.COARSE_DIRT);
                                        }
                                        if (random.nextInt(10) == 0) {
                                            int size = random.nextInt(4, 6);
                                            for (int l = 1; l <= size; l++) {
                                                chunk.setBlock(chunkX * 16 + i, 39 + k + l, chunkZ * 16 + j, Block.OAK_LOG);
                                            }

                                            chunk.setBlock(chunkX * 16 + i, 39 + k + size + 1, chunkZ * 16 + j, Block.OAK_LEAVES);
                                            if (i < 15) {
                                                chunk.setBlock(chunkX * 16 + i + 1, 39 + k + size, chunkZ * 16 + j, Block.OAK_LEAVES);
                                            }
                                            if (i > 0) {
                                                chunk.setBlock(chunkX * 16 + i - 1, 39 + k + size, chunkZ * 16 + j, Block.OAK_LEAVES);
                                            }
                                            if (j < 15) {
                                                chunk.setBlock(chunkX * 16 + i, 39 + k + size, chunkZ * 16 + j + 1, Block.OAK_LEAVES);
                                            }
                                            if (j > 0) {
                                                chunk.setBlock(chunkX * 16 + i, 39 + k + size, chunkZ * 16 + j - 1, Block.OAK_LEAVES);
                                            }

                                            if (random.nextBoolean() && i < 15 && j < 15) {
                                                chunk.setBlock(chunkX * 16 + i + 1, 39 + k + size, chunkZ * 16 + j + 1, Block.OAK_LEAVES);
                                            }
                                            if (random.nextBoolean() && i < 15 && j > 0) {
                                                chunk.setBlock(chunkX * 16 + i + 1, 39 + k + size, chunkZ * 16 + j - 1, Block.OAK_LEAVES);
                                            }
                                            if (random.nextBoolean() && i > 0 && j > 0) {
                                                chunk.setBlock(chunkX * 16 + i - 1, 39 + k + size, chunkZ * 16 + j - 1, Block.OAK_LEAVES);
                                            }
                                            if (random.nextBoolean() && i > 0 && j < 15) {
                                                chunk.setBlock(chunkX * 16 + i - 1, 39 + k + size, chunkZ * 16 + j + 1, Block.OAK_LEAVES);
                                            }

                                            if (random.nextBoolean() && i < 15 && j < 15 && i > 0 && j > 0) {
                                                chunk.setBlock(chunkX * 16 + i + 1, 39 + k + size + 1, chunkZ * 16 + j, Block.OAK_LEAVES);
                                                chunk.setBlock(chunkX * 16 + i - 1, 39 + k + size + 1, chunkZ * 16 + j, Block.OAK_LEAVES);
                                                chunk.setBlock(chunkX * 16 + i, 39 + k + size + 1, chunkZ * 16 + j + 1, Block.OAK_LEAVES);
                                                chunk.setBlock(chunkX * 16 + i, 39 + k + size + 1, chunkZ * 16 + j - 1, Block.OAK_LEAVES);
                                                chunk.setBlock(chunkX * 16 + i, 39 + k + size + 2, chunkZ * 16 + j, Block.OAK_LEAVES);
                                            }
                                        } else if (random.nextInt(2) == 0) {
                                            chunk.setBlock(chunkX * 16 + i, 39 + k + 1, chunkZ * 16 + j, Block.SWEET_BERRY_BUSH.withProperties(Map.of("age", "1")));
                                        }
                                    }
                                    default -> {
                                        if (random.nextBoolean()) {
                                            chunk.setBlock(chunkX * 16 + i, 39 + k, chunkZ * 16 + j, Block.DIRT);
                                        } else {
                                            chunk.setBlock(chunkX * 16 + i, 39 + k, chunkZ * 16 + j, Block.COARSE_DIRT);
                                        }
                                        if (random.nextInt(24) == 0) {
                                            int size = random.nextInt(4, 6);
                                            for (int l = 1; l <= size; l++) {
                                                chunk.setBlock(chunkX * 16 + i, 39 + k + l, chunkZ * 16 + j, Block.OAK_LOG);
                                            }

                                            chunk.setBlock(chunkX * 16 + i, 39 + k + size + 1, chunkZ * 16 + j, Block.OAK_LEAVES);
                                            if (i < 15) {
                                                chunk.setBlock(chunkX * 16 + i + 1, 39 + k + size, chunkZ * 16 + j, Block.OAK_LEAVES);
                                            }
                                            if (i > 0) {
                                                chunk.setBlock(chunkX * 16 + i - 1, 39 + k + size, chunkZ * 16 + j, Block.OAK_LEAVES);
                                            }
                                            if (j < 15) {
                                                chunk.setBlock(chunkX * 16 + i, 39 + k + size, chunkZ * 16 + j + 1, Block.OAK_LEAVES);
                                            }
                                            if (j > 0) {
                                                chunk.setBlock(chunkX * 16 + i, 39 + k + size, chunkZ * 16 + j - 1, Block.OAK_LEAVES);
                                            }

                                            if (random.nextBoolean() && i < 15 && j < 15) {
                                                chunk.setBlock(chunkX * 16 + i + 1, 39 + k + size, chunkZ * 16 + j + 1, Block.OAK_LEAVES);
                                            }
                                            if (random.nextBoolean() && i < 15 && j > 0) {
                                                chunk.setBlock(chunkX * 16 + i + 1, 39 + k + size, chunkZ * 16 + j - 1, Block.OAK_LEAVES);
                                            }
                                            if (random.nextBoolean() && i > 0 && j > 0) {
                                                chunk.setBlock(chunkX * 16 + i - 1, 39 + k + size, chunkZ * 16 + j - 1, Block.OAK_LEAVES);
                                            }
                                            if (random.nextBoolean() && i > 0 && j < 15) {
                                                chunk.setBlock(chunkX * 16 + i - 1, 39 + k + size, chunkZ * 16 + j + 1, Block.OAK_LEAVES);
                                            }

                                            if (random.nextBoolean() && i < 15 && j < 15 && i > 0 && j > 0) {
                                                chunk.setBlock(chunkX * 16 + i + 1, 39 + k + size + 1, chunkZ * 16 + j, Block.OAK_LEAVES);
                                                chunk.setBlock(chunkX * 16 + i - 1, 39 + k + size + 1, chunkZ * 16 + j, Block.OAK_LEAVES);
                                                chunk.setBlock(chunkX * 16 + i, 39 + k + size + 1, chunkZ * 16 + j + 1, Block.OAK_LEAVES);
                                                chunk.setBlock(chunkX * 16 + i, 39 + k + size + 1, chunkZ * 16 + j - 1, Block.OAK_LEAVES);
                                                chunk.setBlock(chunkX * 16 + i, 39 + k + size + 2, chunkZ * 16 + j, Block.OAK_LEAVES);
                                            }
                                        }
                                    }
                                }

                                for (Polygon polygon : buildings) {
                                    if (polygon.contains(chunkX * 16 + i, chunkZ * 16 + j)) {
                                        int area = polygon.getBounds().width * polygon.getBounds().height;;

                                        int randomNumber = (int) (Math.log(area) * 1.3);

                                        for (int l = 0; l < randomNumber; l++) {
                                            chunk.setBlock(chunkX * 16 + i, 40 + k + l, chunkZ * 16 + j, Block.BRICKS);
                                        }
                                    }
                                }

                                for (Polygon polygon : concretes) {
                                    if (polygon.contains(chunkX * 16 + i, chunkZ * 16 + j)) {
                                        chunk.setBlock(chunkX * 16 + i, 40 + k, chunkZ * 16 + j, Block.SMOOTH_STONE);
                                    }
                                }

                                for (List<Point> concrete : concreteLines) {
                                    for (int l = 1; l < concrete.size(); l++) {
                                        Point point = concrete.get(l);
                                        Point previousPoint = concrete.get(l - 1);

                                        if (pDistance(chunkX * 16 + i, chunkZ * 16 + j, point.x, point.y, previousPoint.x, previousPoint.y) < 3) {
                                            for (int p = 0; p < k; p++) {
                                                chunk.setBlock(chunkX * 16 + i, 40 + p, chunkZ * 16 + j, Block.SMOOTH_STONE);
                                            }

                                            for (int p = k; p < k + 15; p++) {
                                                if (!isSolid(chunk.getBlock(chunkX * 16 + i, 40 + p, chunkZ * 16 + j))) {
                                                    chunk.setBlock(chunkX * 16 + i, 40 + p, chunkZ * 16 + j, Block.AIR);
                                                }
                                            }
                                        }
                                    }
                                }

                                for (List<Point> concrete : roads) {
                                    for (int l = 1; l < concrete.size(); l++) {
                                        Point point = concrete.get(l);
                                        Point previousPoint = concrete.get(l - 1);

                                        if (pDistance(chunkX * 16 + i, chunkZ * 16 + j, point.x, point.y, previousPoint.x, previousPoint.y) < 1.5) {
                                            for (int p = 0; p < k; p++) {
                                                Block block;
                                                if (type.equals("water")) {
                                                    block = Block.OAK_PLANKS;
                                                } else {
                                                    if (random.nextBoolean()) {
                                                        block = Block.GRAVEL;
                                                    } else {
                                                        block = Block.DIRT;
                                                    }
                                                }
                                                chunk.setBlock(chunkX * 16 + i, 40 + p, chunkZ * 16 + j, block);
                                            }

                                            for (int p = k; p < k + 15; p++) {
                                                if (!isSolid(chunk.getBlock(chunkX * 16 + i, 40 + p, chunkZ * 16 + j))) {
                                                    chunk.setBlock(chunkX * 16 + i, 40 + p, chunkZ * 16 + j, Block.AIR);
                                                }
                                            }
                                        }
                                    }
                                }

                                for (List<Point> concrete : badRoads) {
                                    for (int l = 1; l < concrete.size(); l++) {
                                        Point point = concrete.get(l);
                                        Point previousPoint = concrete.get(l - 1);

                                        if (pDistance(chunkX * 16 + i, chunkZ * 16 + j, point.x, point.y, previousPoint.x, previousPoint.y) < 1) {
                                            for (int p = 0; p < k; p++) {
                                                Block block;
                                                if (random.nextBoolean()) {
                                                    block = Block.GRAVEL;
                                                } else {
                                                    block = Block.DIRT;
                                                }
                                                chunk.setBlock(chunkX * 16 + i, 40 + p, chunkZ * 16 + j, block);
                                            }

                                            for (int p = k; p < k + 15; p++) {
                                                if (!isSolid(chunk.getBlock(chunkX * 16 + i, 40 + p, chunkZ * 16 + j))) {
                                                    chunk.setBlock(chunkX * 16 + i, 40 + p, chunkZ * 16 + j, Block.AIR);
                                                }
                                            }
                                        }
                                    }
                                }

                                for (List<Point> concrete : footPaths) {
                                    for (int l = 1; l < concrete.size(); l++) {
                                        Point point = concrete.get(l);
                                        Point previousPoint = concrete.get(l - 1);

                                        if (pDistance(chunkX * 16 + i, chunkZ * 16 + j, point.x, point.y, previousPoint.x, previousPoint.y) < 1) {
                                            for (int p = 0; p < k; p++) {
                                                Block block;
                                                int randomInt = random.nextInt(4);
                                                switch (randomInt) {
                                                    case 0 -> block = Block.DIRT;
                                                    case 1, 3 -> block = Block.DIRT_PATH;
                                                    case 2 -> block = Block.COARSE_DIRT;
                                                    default -> block = Block.AIR;
                                                }
                                                chunk.setBlock(chunkX * 16 + i, 40 + p, chunkZ * 16 + j, block);
                                            }

                                            for (int p = k; p < k + 15; p++) {
                                                if (!isSolid(chunk.getBlock(chunkX * 16 + i, 40 + p, chunkZ * 16 + j))) {
                                                    chunk.setBlock(chunkX * 16 + i, 40 + p, chunkZ * 16 + j, Block.AIR);
                                                }
                                            }
                                        }
                                    }
                                }

                                for (List<Point> concrete : fences) {
                                    for (int l = 1; l < concrete.size(); l++) {
                                        Point point = concrete.get(l);
                                        Point previousPoint = concrete.get(l - 1);

                                        if (pDistance(chunkX * 16 + i, chunkZ * 16 + j, point.x, point.y, previousPoint.x, previousPoint.y) < 0.5) {
                                            chunk.setBlock(chunkX * 16 + i, 40 + k, chunkZ * 16 + j, Block.OAK_FENCE);
                                        }
                                    }
                                }

                                for (Point boulder : boulders) {
                                    if (boulder.x == chunkX * 16 + i && boulder.y == chunkZ * 16 + j) {
                                        int x = chunkX * 16 + i;
                                        int y = 40;
                                        int z = chunkZ * 16 + j;

                                        while (chunk.getBlock(x, y, z) != Block.AIR && chunk.getBlock(x, y, z) != Block.WATER && chunk.getBlock(x, y, z) != Block.GRASS && chunk.getBlock(x, y, z) != Block.TALL_GRASS) {
                                            y++;
                                        }
                                        chunk.setBlock(x, y, z, Block.COBBLESTONE);
                                        if (i < 15) {
                                            chunk.setBlock(x + 1, y, z, Block.COBBLESTONE);
                                        }
                                        if (j < 15) {
                                            chunk.setBlock(x, y, z + 1, Block.COBBLESTONE);
                                        }
                                        if (i > 0) {
                                            chunk.setBlock(x - 1, y, z, Block.STONE);
                                        }
                                        if (j > 0) {
                                            chunk.setBlock(x, y, z - 1, Block.STONE);
                                        }

                                        chunk.setBlock(x, y + 1, z, Block.COBBLESTONE);
                                    }
                                }

                                for (Point boulder : knolls) {
                                    if (!type.equals("outOfBounds") && boulder.x == chunkX * 16 + i && boulder.y == chunkZ * 16 + j) {
                                        int x = chunkX * 16 + i;
                                        int y = 40;
                                        int z = chunkZ * 16 + j;

                                        while (chunk.getBlock(x, y, z) != Block.AIR && chunk.getBlock(x, y, z) != Block.WATER && chunk.getBlock(x, y, z) != Block.GRASS && chunk.getBlock(x, y, z) != Block.TALL_GRASS) {
                                            y++;
                                        }
                                        chunk.setBlock(x, y, z, Block.DIRT);
                                        if (i < 15) {
                                            chunk.setBlock(x + 1, y, z, Block.DIRT);
                                        }
                                        if (j < 15) {
                                            chunk.setBlock(x, y, z + 1, Block.DIRT_PATH);
                                        }
                                        if (i > 0) {
                                            chunk.setBlock(x - 1, y, z, Block.DIRT);
                                        }
                                        if (j > 0) {
                                            chunk.setBlock(x, y, z - 1, Block.COARSE_DIRT);
                                        }

                                        if (random.nextInt(2) == 0) {
                                            chunk.setBlock(x, y + 1, z, Block.DIRT);
                                        }
                                    }
                                }

                                for (List<Point> cliff : cliffs) {
                                    for (Point point : cliff) {
                                        if (chunkX * 16 + i == point.x && chunkZ * 16 + j == point.y) {
                                            int l = 40;
                                            while (isSolid(chunk.getBlock(chunkX * 16 + i, l, chunkZ * 16 + j))) {
                                                if (random.nextInt(2) == 0) {
                                                    chunk.setBlock(chunkX * 16 + i, l, chunkZ * 16 + j, Block.STONE);
                                                } else {
                                                    chunk.setBlock(chunkX * 16 + i, l, chunkZ * 16 + j, Block.COBBLESTONE);
                                                }
                                                l++;
                                            }

                                            if (random.nextInt(4) == 0) {
                                                if (random.nextInt(2) == 0) {
                                                    chunk.setBlock(chunkX * 16 + i, l, chunkZ * 16 + j, Block.STONE_SLAB);
                                                } else {
                                                    chunk.setBlock(chunkX * 16 + i, l, chunkZ * 16 + j, Block.COBBLESTONE_SLAB);
                                                }
                                            }
                                            break;
                                        }
                                    }
                                }


                                for (List<Point> cliff : powerLines) {
                                    Point previousPoint = null;
                                    for (Point point : cliff) {
                                        if (chunkX * 16 + i == point.x && chunkZ * 16 + j == point.y) {
                                            for (int l = 0; l < 12; l++) {
                                                chunk.setBlock(chunkX * 16 + i, 40 + k + l, chunkZ * 16 + j, Block.OAK_LOG);
                                            }
                                        } else if (previousPoint != null && pDistance(chunkX * 16 + i, chunkZ * 16 + j, point.x, point.y, previousPoint.x, previousPoint.y) < 0.5) {
                                            chunk.setBlock(chunkX * 16 + i, 40 + k + 12, chunkZ * 16 + j, Block.IRON_BARS);
                                        }
                                        previousPoint = point;
                                    }
                                }
                            }
                        }

                    }

                    return chunk;
                });
            }

            @Override
            public @NotNull CompletableFuture<Void> saveChunk(@NotNull Chunk chunk) {
                return null;
            }
        });

//        instanceContainer.setGenerator(unit -> {
//            //instanceContainer.setBlock(i + bounds.x, 40, j + bounds.y, Block.WATER);
//            for (int i = 0)
//            unit.modifier().fillHeight(0, 40, Block.GRASS_BLOCK);
//            unit.modifier().setBlock();
//        });

        globalEventHandler.addListener(PlayerMoveEvent.class, event -> {
            if (currentCheckpoint.get() >= checkpoints.size()) {
                return;
            }

            Vec currentCheckpointLocation = checkpoints.get(currentCheckpoint.get());
            if (event.getNewPosition().distance(currentCheckpointLocation) < 1.5) {
                event.getPlayer().sendMessage("Found checkpoint " + currentCheckpoint);
                currentCheckpoint.getAndIncrement();
            }
        });

        AtomicInteger scroll = new AtomicInteger(1);

        globalEventHandler.addListener(PlayerTickEvent.class, event -> {
            Graphics2DFramebuffer fb = new Graphics2DFramebuffer();
            Graphics2D renderer = fb.getRenderer();
            renderer.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            renderer.setColor(Color.WHITE);
            renderer.fillRect(0, 0, 128, 128);

            renderer.setColor(Color.BLACK);

            int mapShowing = (int) (mapSize / ((double) scroll.get() / 2));

            for (Vec location : countourLocations) {
                int pixelSize = (int) Math.round(128.0 / mapShowing);
                int normalizedX = (int) Math.round(((double) (location.blockX() + mapShowing / 2) / mapShowing) * 128) - pixelSize / 2;
                int normalizedZ = (int) Math.round(((double) (location.blockZ() + mapShowing / 2) / mapShowing) * 128) - pixelSize / 2;
                renderer.fillRect(normalizedX, normalizedZ, pixelSize, pixelSize);
            }

            renderer.setColor(Color.RED);
            for (Vec checkpoint : checkpoints) {
                renderer.drawOval((int) Math.round(((double) (checkpoint.blockX() + actualMapSize / 2) / actualMapSize) * 128) - 2, (int) Math.round(((double) (checkpoint.blockZ() + actualMapSize / 2) / actualMapSize) * 128) - 2, 4, 4);
            }

            MapDataPacket mapDataPacket = fb.preparePacket(mapId);
            event.getPlayer().sendPacket(mapDataPacket);
        });

        globalEventHandler.addListener(ItemDropEvent.class, event -> {
            event.setCancelled(true);
        });

        globalEventHandler.addListener(PlayerBlockInteractEvent.class, event -> {
            scroll.getAndIncrement();
        });

        globalEventHandler.addListener(PlayerBlockBreakEvent.class, event -> {
            scroll.getAndDecrement();
        });

        minecraftServer.start("0.0.0.0", 25565);
    }

    public static float pDistance(float x, float y, float x1, float y1, float x2, float y2) {
        float A = x - x1;
        float B = y - y1;
        float C = x2 - x1;
        float D = y2 - y1;

        float dot = A * C + B * D;
        float len_sq = C * C + D * D;
        float param = -1;
        if (len_sq != 0) //in case of 0 length line
            param = dot / len_sq;

        float xx, yy;

        if (param < 0) {
            xx = x1;
            yy = y1;
        }
        else if (param > 1) {
            xx = x2;
            yy = y2;
        }
        else {
            xx = x1 + param * C;
            yy = y1 + param * D;
        }

        float dx = x - xx;
        float dy = y - yy;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private static boolean isSolid(Block block) {
        return block != Block.AIR && block != Block.WATER && block != Block.GRASS && block.id() != Block.TALL_GRASS.id() && block != Block.OAK_LEAVES;
    }

    private static Pair<Double, Double> getPointCoordinates(Object coordinatesBase) {
        JSONArray coordinates = (JSONArray) coordinatesBase;
        return Pair.of(coordinates.getDouble(0), coordinates.getDouble(1));
    }

    private static List<Pair<Double, Double>> getLineStringCoordinates(Object coordinatesBase) {
        JSONArray coordinates = (JSONArray) coordinatesBase;
        List<Pair<Double, Double>> list = new ArrayList<>();
        for (Object item : coordinates) {
            JSONArray array = (JSONArray) item;
            list.add(Pair.of(array.getDouble(0), array.getDouble(1)));
        }
        return list;
    }

    private static List<Pair<Double, Double>> getPolygonCoordinates(Object coordinatesBase) {
        JSONArray coordinates = ((JSONArray) coordinatesBase).getJSONArray(0);
        List<Pair<Double, Double>> list = new ArrayList<>();
        for (Object item : coordinates) {
            JSONArray array = (JSONArray) item;
            list.add(Pair.of(array.getDouble(0), array.getDouble(1)));
        }
        return list;
    }
}
