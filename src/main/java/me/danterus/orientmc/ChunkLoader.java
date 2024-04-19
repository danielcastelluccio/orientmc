package me.danterus.orientmc;

import java.awt.Point;
import java.awt.Polygon;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;

import it.unimi.dsi.fastutil.Pair;
import net.minestom.server.MinecraftServer;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.DynamicChunk;
import net.minestom.server.instance.IChunkLoader;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;

public class ChunkLoader implements IChunkLoader {

    int sizeX;
    int sizeY;

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

    Random random = new Random();

    public ChunkLoader(int size) {
        MinecraftServer.getSchedulerManager().scheduleNextTick(() -> {
            try {
                JSONObject object = new JSONObject(Files.readString(Path.of("map.json")));

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

                sizeX = size;
                sizeY = (int) (size * ((maxY - minY) / (maxX - minX)));

                for (Object featureOld : features) {
                    JSONObject feature = (JSONObject) featureOld;

                    JSONObject properties = feature.getJSONObject("properties");

                    if (!properties.has("sym")) {
                        continue;
                    }

                    int symbol = properties.getInt("sym");
                    JSONObject geometry = feature.getJSONObject("geometry");

                    int BOULDER = 204000;
                    int CONTOUR = 101000;
                    int CONTOUR2 = 101001;
                    int CONTOUR3 = 101002;
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
                    int FOOTPATH = 505;
                    int CONCRETE = 501000;
                    int CONCRETE2 = 501001;
                    int OUT_OF_BOUNDS = 520000;
                    int FENCE = 516000;
                    int VEGETATION_FIGHT = 410000;
                    int POWER_LINE = 510000;

                    Object coordinatesBase = geometry.get("coordinates");
                    if (symbol == BOULDER) {
                        Pair<Double, Double> coordinates = getPointCoordinates(coordinatesBase);

                        double relativeX = 1 - ((coordinates.left() - minX) / (maxX - minX));
                        double relativeY = (coordinates.right() - minY) / (maxY - minY);

                        boulders.add(new Point((int) (relativeX * sizeX - sizeX/2), (int) (relativeY * sizeY - sizeY/2)));
                    } else if (symbol == KNOLL) {
                        Pair<Double, Double> coordinates = getPointCoordinates(coordinatesBase);

                        double relativeX = 1 - ((coordinates.left() - minX) / (maxX - minX));
                        double relativeY = (coordinates.right() - minY) / (maxY - minY);

                        knolls.add(new Point((int) (relativeX * sizeX - sizeX/2), (int) (relativeY * sizeY - sizeY/2)));
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

                        Polygon polygon = new Polygon(xPoints.stream().mapToInt(i -> (int) (i * sizeX - sizeX/2)).toArray(), yPoints.stream().mapToInt(i -> (int) (i * sizeY - sizeY/2)).toArray(), xPoints.size());
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

                        Polygon polygon = new Polygon(xPoints.stream().mapToInt(i -> (int) (i * sizeX - sizeX/2)).toArray(), yPoints.stream().mapToInt(i -> (int) (i * sizeY - sizeY/2)).toArray(), xPoints.size());
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

                        Polygon polygon = new Polygon(xPoints.stream().mapToInt(i -> (int) (i * sizeX - sizeX/2)).toArray(), yPoints.stream().mapToInt(i -> (int) (i * sizeY - sizeY/2)).toArray(), xPoints.size());
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

                        Polygon polygon = new Polygon(xPoints.stream().mapToInt(i -> (int) (i * sizeX - sizeX/2)).toArray(), yPoints.stream().mapToInt(i -> (int) (i * sizeY - sizeY/2)).toArray(), xPoints.size());
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

                        Polygon polygon = new Polygon(xPoints.stream().mapToInt(i -> (int) (i * sizeX - sizeX/2)).toArray(), yPoints.stream().mapToInt(i -> (int) (i * sizeY - sizeY/2)).toArray(), xPoints.size());
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

                        Polygon polygon = new Polygon(xPoints.stream().mapToInt(i -> (int) (i * sizeX - sizeX/2)).toArray(), yPoints.stream().mapToInt(i -> (int) (i * sizeY - sizeY/2)).toArray(), xPoints.size());
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

                        Polygon polygon = new Polygon(xPoints.stream().mapToInt(i -> (int) (i * sizeX - sizeX/2)).toArray(), yPoints.stream().mapToInt(i -> (int) (i * sizeY - sizeY/2)).toArray(), xPoints.size());
                        outOfBounds.add(polygon);
                    } else if (symbol == CLIFF) {
                        List<Point> coordinatesNew = new ArrayList<>();

                        List<Pair<Double, Double>> coordinates = getLineStringCoordinates(coordinatesBase);

                        for (Pair<Double, Double> coordinate : coordinates) {
                            double relativeX = ((coordinate.left() - minX) / (maxX - minX));
                            double relativeY = 1 - (coordinate.right() - minY) / (maxY - minY);

                            coordinatesNew.add(new Point((int) ((1 - relativeX) * sizeX - sizeX/2), (int) ((1 - relativeY) * sizeY - sizeY/2)));
                        }

                        cliffs.add(coordinatesNew);
                    } else if (symbol == POWER_LINE) {
                        List<Point> coordinatesNew = new ArrayList<>();

                        List<Pair<Double, Double>> coordinates = getLineStringCoordinates(coordinatesBase);

                        for (Pair<Double, Double> coordinate : coordinates) {
                            double relativeX = ((coordinate.left() - minX) / (maxX - minX));
                            double relativeY = 1 - (coordinate.right() - minY) / (maxY - minY);

                            coordinatesNew.add(new Point((int) ((1 - relativeX) * sizeX - sizeX/2), (int) ((1 - relativeY) * sizeY - sizeY/2)));
                        }

                        powerLines.add(coordinatesNew);
                    } else if (symbol == FENCE) {
                        List<Point> coordinatesNew = new ArrayList<>();

                        List<Pair<Double, Double>> coordinates = getLineStringCoordinates(coordinatesBase);

                        for (Pair<Double, Double> coordinate : coordinates) {
                            double relativeX = ((coordinate.left() - minX) / (maxX - minX));
                            double relativeY = 1 - (coordinate.right() - minY) / (maxY - minY);

                            coordinatesNew.add(new Point((int) ((1 - relativeX) * sizeX - sizeX/2), (int) ((1 - relativeY) * sizeY - sizeY/2)));
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

                        Polygon polygon = new Polygon(xPoints.stream().mapToInt(i -> (int) (i * sizeX - sizeX/2)).toArray(), yPoints.stream().mapToInt(i -> (int) (i * sizeY - sizeY/2)).toArray(), xPoints.size());
                        buildings.add(polygon);
                    } else if (symbol == CONCRETE || symbol == CONCRETE2) {
                        List<Double> xPoints = new ArrayList<>();
                        List<Double> yPoints = new ArrayList<>();

                        List<Pair<Double, Double>> coordinates = getPolygonCoordinates(coordinatesBase);

                        for (Pair<Double, Double> coordinate : coordinates) {
                            double relativeX = ((coordinate.left() - minX) / (maxX - minX));
                            double relativeY = 1 - (coordinate.right() - minY) / (maxY - minY);

                            xPoints.add(1 - relativeX);
                            yPoints.add(1 - relativeY);
                        }

                        Polygon polygon = new Polygon(xPoints.stream().mapToInt(i -> (int) (i * sizeX - sizeX/2)).toArray(), yPoints.stream().mapToInt(i -> (int) (i * sizeY - sizeY/2)).toArray(), xPoints.size());
                        concretes.add(polygon);
                    } else if (symbol == CONCRETE2) {
                        List<Point> coordinatesNew = new ArrayList<>();

                        List<Pair<Double, Double>> coordinates = getLineStringCoordinates(coordinatesBase);

                        for (Pair<Double, Double> coordinate : coordinates) {
                            double relativeX = ((coordinate.left() - minX) / (maxX - minX));
                            double relativeY = 1 - (coordinate.right() - minY) / (maxY - minY);

                            coordinatesNew.add(new Point((int) ((1 - relativeX) * sizeX - sizeX/2), (int) ((1 - relativeY) * sizeY - sizeY/2)));
                        }

                        concreteLines.add(coordinatesNew);
                    } else if (symbol == ROAD) {
                        List<Point> coordinatesNew = new ArrayList<>();

                        List<Pair<Double, Double>> coordinates = getLineStringCoordinates(coordinatesBase);

                        for (Pair<Double, Double> coordinate : coordinates) {
                            double relativeX = ((coordinate.left() - minX) / (maxX - minX));
                            double relativeY = 1 - (coordinate.right() - minY) / (maxY - minY);

                            coordinatesNew.add(new Point((int) ((1 - relativeX) * sizeX - sizeX/2), (int) ((1 - relativeY) * sizeY - sizeY/2)));
                        }

                        roads.add(coordinatesNew);
                    } else if (symbol == UNMAINTAINED_ROAD) {
                        List<Point> coordinatesNew = new ArrayList<>();

                        List<Pair<Double, Double>> coordinates = getLineStringCoordinates(coordinatesBase);

                        for (Pair<Double, Double> coordinate : coordinates) {
                            double relativeX = ((coordinate.left() - minX) / (maxX - minX));
                            double relativeY = 1 - (coordinate.right() - minY) / (maxY - minY);

                            coordinatesNew.add(new Point((int) ((1 - relativeX) * sizeX - sizeX/2), (int) ((1 - relativeY) * sizeY - sizeY/2)));
                        }

                        badRoads.add(coordinatesNew);
                    } else if (symbol / 1000 == FOOTPATH) {
                        List<Point> coordinatesNew = new ArrayList<>();

                        List<Pair<Double, Double>> coordinates = getLineStringCoordinates(coordinatesBase);

                        for (Pair<Double, Double> coordinate : coordinates) {
                            double relativeX = ((coordinate.left() - minX) / (maxX - minX));
                            double relativeY = 1 - (coordinate.right() - minY) / (maxY - minY);

                            coordinatesNew.add(new Point((int) ((1 - relativeX) * sizeX - sizeX/2), (int) ((1 - relativeY) * sizeY - sizeY/2)));
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

    }

    @Override
    public @NotNull CompletableFuture<@Nullable Chunk> loadChunk(@NotNull Instance instance, int chunkX, int chunkZ) {
        return CompletableFuture.supplyAsync(() -> {
            Chunk chunk = new DynamicChunk(instance, chunkX, chunkZ);

            int[][] heightMap = new int[16][16];

            if (chunkX - 1 < -sizeX/2 / 16) {

            } else if (chunkZ - 1 < -sizeY/2 / 16) {

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
                                if (random.nextInt(8) == 0) {
                                    chunk.setBlock(chunkX * 16 + i, 40 + k, chunkZ * 16 + j, Block.TALL_GRASS);
                                }
                            }
                            case "water" -> {
                                chunk.setBlock(chunkX * 16 + i, 39 + k, chunkZ * 16 + j, Block.WATER);
                                chunk.setBlock(chunkX * 16 + i, 38 + k, chunkZ * 16 + j, Block.WATER);
                                chunk.setBlock(chunkX * 16 + i, 37 + k, chunkZ * 16 + j, Block.WATER);
                                chunk.setBlock(chunkX * 16 + i, 36 + k, chunkZ * 16 + j, Block.SAND);
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
                                    chunk.setBlock(chunkX * 16 + i, 39 + k + 1, chunkZ * 16 + j, Block.SWEET_BERRY_BUSH.withProperties(Map.of("age", Integer.toString(random.nextInt(3)))));
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
                                chunk.setBlock(chunkX * 16 + i, 39 + k, chunkZ * 16 + j, Block.SMOOTH_STONE);
                            }
                        }

                        for (List<Point> concrete : concreteLines) {
                            for (int l = 1; l < concrete.size(); l++) {
                                Point point = concrete.get(l);
                                Point previousPoint = concrete.get(l - 1);

                                if (pDistance(chunkX * 16 + i, chunkZ * 16 + j, point.x, point.y, previousPoint.x, previousPoint.y) < 1) {
                                    for (int p = 0; p < k; p++) {
                                        chunk.setBlock(chunkX * 16 + i, 39 + p, chunkZ * 16 + j, Block.SMOOTH_STONE);
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

                                while (chunk.getBlock(x, y, z) != Block.AIR && chunk.getBlock(x, y, z) != Block.WATER && chunk.getBlock(x, y, z) != Block.GRASS_BLOCK && chunk.getBlock(x, y, z) != Block.TALL_GRASS) {
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

                                while (chunk.getBlock(x, y, z) != Block.AIR && chunk.getBlock(x, y, z) != Block.WATER && chunk.getBlock(x, y, z) != Block.GRASS_BLOCK && chunk.getBlock(x, y, z) != Block.TALL_GRASS) {
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

    private boolean isSolid(Block block) {
        return block != Block.AIR && block != Block.WATER && block != Block.GRASS_BLOCK && block.id() != Block.TALL_GRASS.id() && block != Block.OAK_LEAVES;
    }

    private Pair<Double, Double> getPointCoordinates(Object coordinatesBase) {
        JSONArray coordinates = (JSONArray) coordinatesBase;
        return Pair.of(coordinates.getDouble(0), coordinates.getDouble(1));
    }

    private List<Pair<Double, Double>> getLineStringCoordinates(Object coordinatesBase) {
        JSONArray coordinates = (JSONArray) coordinatesBase;
        List<Pair<Double, Double>> list = new ArrayList<>();
        for (Object item : coordinates) {
            if (item instanceof JSONArray) {
                JSONArray array = (JSONArray) item;
                list.add(Pair.of(array.getDouble(0), array.getDouble(1)));
            }
        }
        return list;
    }

    private List<Pair<Double, Double>> getPolygonCoordinates(Object coordinatesBase) {
        List<Pair<Double, Double>> list = new ArrayList<>();
        if (coordinatesBase instanceof JSONArray) {
            if (((JSONArray) ((JSONArray) coordinatesBase).get(0)).get(0) instanceof JSONArray) {
                JSONArray coordinates = ((JSONArray) coordinatesBase).getJSONArray(0);
                for (Object item : coordinates) {
                    if (item instanceof JSONArray) {
                        JSONArray array = (JSONArray) item;
                        list.add(Pair.of(array.getDouble(0), array.getDouble(1)));
                    }
                }
            } else {
                for (Object item : ((JSONArray) coordinatesBase)) {
                    if (item instanceof JSONArray) {
                        JSONArray array = (JSONArray) item;
                        list.add(Pair.of(array.getDouble(0), array.getDouble(1)));
                    }
                }
            }
        }
        return list;
    }

    public float pDistance(float x, float y, float x1, float y1, float x2, float y2) {
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
}
