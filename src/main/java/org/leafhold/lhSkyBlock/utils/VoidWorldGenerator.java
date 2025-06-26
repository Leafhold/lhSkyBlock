package org.leafhold.lhSkyBlock.utils;

import org.leafhold.lhSkyBlock.lhSkyBlock;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.Location;
import org.bukkit.generator.BiomeProvider;


import java.util.List;
import java.util.Random;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VoidWorldGenerator extends ChunkGenerator {
    private static lhSkyBlock plugin;
    private static FileConfiguration config;

    public VoidWorldGenerator(lhSkyBlock plugin) {
        this.plugin = plugin;
        config = plugin.getConfig();
    }

    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        return new VoidChunkGenerator(worldName);
    }
}

class VoidChunkGenerator extends ChunkGenerator {
    private String worldName;

    public VoidChunkGenerator(String worldName) {
        this.worldName = worldName;
    }

        @Override
        public List<BlockPopulator> getDefaultPopulators(World world) { return List.of(); }

        @Override
        public void generateNoise(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkData chunkData) {}
        
        @Override
        public void generateSurface(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkData chunkData) {}
        
        @Override
        public void generateBedrock(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkData chunkData) {}
        
        @Override
        public void generateCaves(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkData chunkData) {}

        @Override
        @Nullable
        public BiomeProvider getDefaultBiomeProvider(@NotNull WorldInfo worldInfo) { return new VoidBiomeProvider(worldName); }

        @Override
        public boolean canSpawn(World world, int x, int z) { return true; }

    private class VoidBiomeProvider extends BiomeProvider {
        private final String worldName;

        private VoidBiomeProvider(String worldName) {
            this.worldName = worldName;
        }

        @Override
        public @NotNull Biome getBiome(@NotNull WorldInfo worldInfo, int x, int y, int z) {
            return Biome.THE_VOID;
        }

        @Override
        public @NotNull List<Biome> getBiomes(@NotNull WorldInfo worldInfo) {
            return List.of(Biome.THE_VOID);
        }
    }
}
