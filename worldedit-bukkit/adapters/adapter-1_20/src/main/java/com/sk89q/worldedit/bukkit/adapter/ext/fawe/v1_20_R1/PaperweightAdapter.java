/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.bukkit.adapter.ext.fawe.v1_20_R1;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Lifecycle;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.NBTConstants;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseItem;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.adapter.BukkitImplAdapter;
import com.sk89q.worldedit.bukkit.adapter.Refraction;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.extension.platform.Watchdog;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.internal.Constants;
import com.sk89q.worldedit.internal.block.BlockStateIdAccess;
import com.sk89q.worldedit.internal.wna.WorldNativeAccess;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.registry.state.BooleanProperty;
import com.sk89q.worldedit.registry.state.DirectionalProperty;
import com.sk89q.worldedit.registry.state.EnumProperty;
import com.sk89q.worldedit.registry.state.IntegerProperty;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.util.SideEffect;
import com.sk89q.worldedit.util.concurrency.LazyReference;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;
import com.sk89q.worldedit.util.io.file.SafeFiles;
import com.sk89q.worldedit.util.nbt.BinaryTag;
import com.sk89q.worldedit.util.nbt.ByteArrayBinaryTag;
import com.sk89q.worldedit.util.nbt.ByteBinaryTag;
import com.sk89q.worldedit.util.nbt.CompoundBinaryTag;
import com.sk89q.worldedit.util.nbt.DoubleBinaryTag;
import com.sk89q.worldedit.util.nbt.EndBinaryTag;
import com.sk89q.worldedit.util.nbt.FloatBinaryTag;
import com.sk89q.worldedit.util.nbt.IntArrayBinaryTag;
import com.sk89q.worldedit.util.nbt.IntBinaryTag;
import com.sk89q.worldedit.util.nbt.ListBinaryTag;
import com.sk89q.worldedit.util.nbt.LongArrayBinaryTag;
import com.sk89q.worldedit.util.nbt.LongBinaryTag;
import com.sk89q.worldedit.util.nbt.ShortBinaryTag;
import com.sk89q.worldedit.util.nbt.StringBinaryTag;
import com.sk89q.worldedit.world.DataFixer;
import com.sk89q.worldedit.world.RegenOptions;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.item.ItemType;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.Clearable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World.Environment;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_20_R1.CraftServer;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R1.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_20_R1.util.CraftMagicNumbers;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.generator.ChunkGenerator;
import org.spigotmc.SpigotConfig;
import org.spigotmc.WatchdogThread;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public final class PaperweightAdapter implements BukkitImplAdapter<net.minecraft.nbt.Tag> {

    private final Logger LOGGER = Logger.getLogger(getClass().getCanonicalName());

    private final Field serverWorldsField;
    private final Method getChunkFutureMethod;
    private final Field chunkProviderExecutorField;
    private final Watchdog watchdog;

    private final Boolean folia;

    // ------------------------------------------------------------------------
    // Code that may break between versions of Minecraft
    // ------------------------------------------------------------------------

    public PaperweightAdapter() throws NoSuchFieldException, NoSuchMethodException {
        // A simple test
        CraftServer.class.cast(Bukkit.getServer());

        int dataVersion = CraftMagicNumbers.INSTANCE.getDataVersion();
        if (dataVersion != 3463 && dataVersion != 3465) {
            throw new UnsupportedClassVersionError("Not 1.20(.1)!");
        }

        boolean isFolia = false;
        try {
            // Assume API is present
            Class.forName("io.papermc.paper.threadedregions.scheduler.EntityScheduler");
            isFolia = true;
        } catch (Exception unused) {

        }
        this.folia = isFolia;

        serverWorldsField = CraftServer.class.getDeclaredField("worlds");
        serverWorldsField.setAccessible(true);

        getChunkFutureMethod = ServerChunkCache.class.getDeclaredMethod(
                Refraction.pickName("getChunkFutureMainThread", "c"),
                int.class, int.class, ChunkStatus.class, boolean.class
        );
        getChunkFutureMethod.setAccessible(true);

        chunkProviderExecutorField = ServerChunkCache.class.getDeclaredField(
                Refraction.pickName("mainThreadProcessor", "g")
        );
        chunkProviderExecutorField.setAccessible(true);

        new PaperweightDataConverters(CraftMagicNumbers.INSTANCE.getDataVersion(), this).buildUnoptimized();

        Watchdog watchdog;
        try {
            Class.forName("org.spigotmc.WatchdogThread");
            watchdog = new SpigotWatchdog();
        } catch (ClassNotFoundException | NoSuchFieldException e) {
            try {
                watchdog = new MojangWatchdog(((CraftServer) Bukkit.getServer()).getServer());
            } catch (NoSuchFieldException ex) {
                watchdog = null;
            }
        }
        this.watchdog = watchdog;

        try {
            Class.forName("org.spigotmc.SpigotConfig");
            SpigotConfig.config.set("world-settings.faweregentempworld.verbose", false);
        } catch (ClassNotFoundException ignored) {
        }
    }

    public Boolean isFolia() {
        return folia;
    }

    @Override
    public DataFixer getDataFixer() {
        return PaperweightDataConverters.INSTANCE;
    }

    /**
     * Read the given NBT data into the given tile entity.
     *
     * @param tileEntity the tile entity
     * @param tag the tag
     */
    static void readTagIntoTileEntity(net.minecraft.nbt.CompoundTag tag, BlockEntity tileEntity) {
        tileEntity.load(tag);
        tileEntity.setChanged();
    }

    /**
     * Get the ID string of the given entity.
     *
     * @param entity the entity
     * @return the entity ID
     */
    private static String getEntityId(Entity entity) {
        return EntityType.getKey(entity.getType()).toString();
    }

    /**
     * Create an entity using the given entity ID.
     *
     * @param id the entity ID
     * @param world the world
     * @return an entity or null
     */
    @Nullable
    private static Entity createEntityFromId(String id, net.minecraft.world.level.Level world) {
        return EntityType.byString(id).map(t -> t.create(world)).orElse(null);
    }

    /**
     * Write the given NBT data into the given entity.
     *
     * @param entity the entity
     * @param tag the tag
     */
    private static void readTagIntoEntity(net.minecraft.nbt.CompoundTag tag, Entity entity) {
        entity.load(tag);
    }

    /**
     * Write the entity's NBT data to the given tag.
     *
     * @param entity the entity
     * @param tag the tag
     */
    private static void readEntityIntoTag(Entity entity, net.minecraft.nbt.CompoundTag tag) {
        entity.save(tag);
    }

    private static Block getBlockFromType(BlockType blockType) {

        return DedicatedServer.getServer().registryAccess().registryOrThrow(Registries.BLOCK).get(ResourceLocation.tryParse(blockType.getId()));
    }

    private static Item getItemFromType(ItemType itemType) {
        return DedicatedServer.getServer().registryAccess().registryOrThrow(Registries.ITEM).get(ResourceLocation.tryParse(itemType.getId()));
    }

    @Override
    public OptionalInt getInternalBlockStateId(BlockData data) {
        net.minecraft.world.level.block.state.BlockState state = ((CraftBlockData) data).getState();
        int combinedId = Block.getId(state);
        return combinedId == 0 && state.getBlock() != Blocks.AIR ? OptionalInt.empty() : OptionalInt.of(combinedId);
    }

    @Override
    public OptionalInt getInternalBlockStateId(BlockState state) {
        Block mcBlock = getBlockFromType(state.getBlockType());
        net.minecraft.world.level.block.state.BlockState newState = mcBlock.defaultBlockState();
        Map<Property<?>, Object> states = state.getStates();
        newState = applyProperties(mcBlock.getStateDefinition(), newState, states);
        final int combinedId = Block.getId(newState);
        return combinedId == 0 && state.getBlockType() != BlockTypes.AIR ? OptionalInt.empty() : OptionalInt.of(combinedId);
    }

    @Override
    public BlockState getBlock(Location location) {
        checkNotNull(location);

        CraftWorld craftWorld = ((CraftWorld) location.getWorld());
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        final ServerLevel handle = craftWorld.getHandle();
        LevelChunk chunk = handle.getChunk(x >> 4, z >> 4);
        final BlockPos blockPos = new BlockPos(x, y, z);
        final net.minecraft.world.level.block.state.BlockState blockData = chunk.getBlockState(blockPos);
        int internalId = Block.getId(blockData);
        BlockState state = BlockStateIdAccess.getBlockStateById(internalId);
        if (state == null) {
            org.bukkit.block.Block bukkitBlock = location.getBlock();
            state = BukkitAdapter.adapt(bukkitBlock.getBlockData());
        }

        return state;
    }

    @Override
    public BaseBlock getFullBlock(Location location) {
        BlockState state = getBlock(location);

        CraftWorld craftWorld = ((CraftWorld) location.getWorld());
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        final ServerLevel handle = craftWorld.getHandle();
        LevelChunk chunk = handle.getChunk(x >> 4, z >> 4);
        final BlockPos blockPos = new BlockPos(x, y, z);

        // Read the NBT data
        BlockEntity te = chunk.getBlockEntity(blockPos);
        if (te != null) {
            net.minecraft.nbt.CompoundTag tag = te.saveWithId();
            return state.toBaseBlock((CompoundBinaryTag) toNativeBinary(tag));
        }

        return state.toBaseBlock();
    }
/*
    @Override
    public boolean hasCustomBiomeSupport() {
        return true;
    }
*/
    private static final HashMap<BiomeType, Holder<Biome>> biomeTypeToNMSCache = new HashMap<>();
    private static final HashMap<Holder<Biome>, BiomeType> biomeTypeFromNMSCache = new HashMap<>();

   /* @Override
    public BiomeType getBiome(Location location) {
        checkNotNull(location);

        CraftWorld craftWorld = ((CraftWorld) location.getWorld());
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        final ServerLevel handle = craftWorld.getHandle();
        LevelChunk chunk = handle.getChunk(x >> 4, z >> 4);

        return biomeTypeFromNMSCache.computeIfAbsent(chunk.getNoiseBiome(x >> 2, y >> 2, z >> 2), b -> BiomeType.REGISTRY.get(b.unwrapKey().get().location().toString()));
    }

    @Override
    public void setBiome(Location location, BiomeType biome) {
        checkNotNull(location);
        checkNotNull(biome);

        CraftWorld craftWorld = ((CraftWorld) location.getWorld());
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        final ServerLevel handle = craftWorld.getHandle();
        LevelChunk chunk = handle.getChunk(x >> 4, z >> 4);
        chunk.setBiome(x >> 2, y >> 2, z >> 2, biomeTypeToNMSCache.computeIfAbsent(biome, b -> ((CraftServer) Bukkit.getServer()).getServer().registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(ResourceKey.create(Registries.BIOME, new ResourceLocation(b.getId())))));
        chunk.setUnsaved(true);
    }*/

    @Override
    public WorldNativeAccess<?, ?, ?> createWorldNativeAccess(org.bukkit.World world) {
        return new com.sk89q.worldedit.bukkit.adapter.ext.fawe.v1_20_R1.PaperweightWorldNativeAccess(this,
                new WeakReference<>(((CraftWorld) world).getHandle()));
    }

    private static net.minecraft.core.Direction adapt(Direction face) {
        switch (face) {
            case NORTH:
                return net.minecraft.core.Direction.NORTH;
            case SOUTH:
                return net.minecraft.core.Direction.SOUTH;
            case WEST:
                return net.minecraft.core.Direction.WEST;
            case EAST:
                return net.minecraft.core.Direction.EAST;
            case DOWN:
                return net.minecraft.core.Direction.DOWN;
            case UP:
            default:
                return net.minecraft.core.Direction.UP;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private net.minecraft.world.level.block.state.BlockState applyProperties(
            StateDefinition<Block, net.minecraft.world.level.block.state.BlockState> stateContainer,
            net.minecraft.world.level.block.state.BlockState newState,
            Map<Property<?>, Object> states
    ) {
        for (Map.Entry<Property<?>, Object> state : states.entrySet()) {
            net.minecraft.world.level.block.state.properties.Property<?> property =
                    stateContainer.getProperty(state.getKey().getName());
            Comparable<?> value = (Comparable) state.getValue();
            // we may need to adapt this value, depending on the source prop
            if (property instanceof DirectionProperty) {
                Direction dir = (Direction) value;
                value = adapt(dir);
            } else if (property instanceof net.minecraft.world.level.block.state.properties.EnumProperty) {
                String enumName = (String) value;
                value = ((net.minecraft.world.level.block.state.properties.EnumProperty<?>) property)
                        .getValue(enumName).orElseThrow(() ->
                                new IllegalStateException(
                                        "Enum property " + property.getName() + " does not contain " + enumName
                                )
                        );
            }

            newState = newState.setValue(
                    (net.minecraft.world.level.block.state.properties.Property) property,
                    (Comparable) value
            );
        }
        return newState;
    }

    @Override
    public BaseEntity getEntity(org.bukkit.entity.Entity entity) {
        checkNotNull(entity);

        CraftEntity craftEntity = ((CraftEntity) entity);
        Entity mcEntity = craftEntity.getHandle();

        // Do not allow creating of passenger entity snapshots, passengers are included in the vehicle entity
        if (mcEntity.isPassenger()) {
            return null;
        }

        String id = getEntityId(mcEntity);

        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        readEntityIntoTag(mcEntity, tag);
        return new BaseEntity(
                com.sk89q.worldedit.world.entity.EntityTypes.get(id),
                LazyReference.from(() -> (CompoundBinaryTag) toNativeBinary(tag))
        );
    }

    @Nullable
    @Override
    public org.bukkit.entity.Entity createEntity(Location location, BaseEntity state) {
        checkNotNull(location);
        checkNotNull(state);

        CraftWorld craftWorld = ((CraftWorld) location.getWorld());
        ServerLevel worldServer = craftWorld.getHandle();

        Entity createdEntity = createEntityFromId(state.getType().getId(), craftWorld.getHandle());

        if (createdEntity != null) {
            CompoundBinaryTag nativeTag = state.getNbt();
            if (nativeTag != null) {
                net.minecraft.nbt.CompoundTag tag = (net.minecraft.nbt.CompoundTag) fromNativeBinary(nativeTag);
                for (String name : Constants.NO_COPY_ENTITY_NBT_FIELDS) {
                    tag.remove(name);
                }
                readTagIntoEntity(tag, createdEntity);
            }

            createdEntity.absMoveTo(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());

            worldServer.addFreshEntity(createdEntity, SpawnReason.CUSTOM);
            return createdEntity.getBukkitEntity();
        } else {
            return null;
        }
    }

    // This removes all unwanted tags from the main entity and all its passengers
    private void removeUnwantedEntityTagsRecursively(net.minecraft.nbt.CompoundTag tag) {
        for (String name : Constants.NO_COPY_ENTITY_NBT_FIELDS) {
            tag.remove(name);
        }

        // Adapted from net.minecraft.world.entity.EntityType#loadEntityRecursive
        if (tag.contains("Passengers", NBTConstants.TYPE_LIST)) {
            net.minecraft.nbt.ListTag nbttaglist = tag.getList("Passengers", NBTConstants.TYPE_COMPOUND);

            for (int i = 0; i < nbttaglist.size(); ++i) {
                removeUnwantedEntityTagsRecursively(nbttaglist.getCompound(i));
            }
        }
    }

    @Override
    public Component getRichBlockName(BlockType blockType) {
        return TranslatableComponent.of(getBlockFromType(blockType).getDescriptionId());
    }

    @Override
    public Component getRichItemName(ItemType itemType) {
        return TranslatableComponent.of(getItemFromType(itemType).getDescriptionId());
    }

    @Override
    public Component getRichItemName(BaseItemStack itemStack) {
        return TranslatableComponent.of(CraftItemStack.asNMSCopy(BukkitAdapter.adapt(itemStack)).getDescriptionId());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static final LoadingCache<net.minecraft.world.level.block.state.properties.Property, Property<?>> PROPERTY_CACHE = CacheBuilder.newBuilder().build(new CacheLoader<net.minecraft.world.level.block.state.properties.Property, Property<?>>() {
        @Override
        public Property<?> load(net.minecraft.world.level.block.state.properties.Property state) throws Exception {
            if (state instanceof net.minecraft.world.level.block.state.properties.BooleanProperty) {
                return new BooleanProperty(state.getName(), ImmutableList.copyOf(state.getPossibleValues()));
            } else if (state instanceof DirectionProperty) {
                return new DirectionalProperty(state.getName(),
                        (List<Direction>) state.getPossibleValues().stream().map(e -> Direction.valueOf(((StringRepresentable) e).getSerializedName().toUpperCase(Locale.ROOT))).collect(Collectors.toList()));
            } else if (state instanceof net.minecraft.world.level.block.state.properties.EnumProperty) {
                return new EnumProperty(state.getName(),
                        (List<String>) state.getPossibleValues().stream().map(e -> ((StringRepresentable) e).getSerializedName()).collect(Collectors.toList()));
            } else if (state instanceof net.minecraft.world.level.block.state.properties.IntegerProperty) {
                return new IntegerProperty(state.getName(), ImmutableList.copyOf(state.getPossibleValues()));
            } else {
                throw new IllegalArgumentException("WorldEdit needs an update to support " + state.getClass().getSimpleName());
            }
        }
    });

    @SuppressWarnings({ "rawtypes" })
    @Override
    public Map<String, ? extends Property<?>> getProperties(BlockType blockType) {
        Map<String, Property<?>> properties = new TreeMap<>();
        Block block = getBlockFromType(blockType);
        StateDefinition<Block, net.minecraft.world.level.block.state.BlockState> blockStateList =
                block.getStateDefinition();
        for (net.minecraft.world.level.block.state.properties.Property state : blockStateList.getProperties()) {
            Property<?> property = PROPERTY_CACHE.getUnchecked(state);
            properties.put(property.getName(), property);
        }
        return properties;
    }

    @Override
    public void sendFakeNBT(Player player, BlockVector3 pos, CompoundBinaryTag nbtData) {
        ((CraftPlayer) player).getHandle().connection.send(ClientboundBlockEntityDataPacket.create(
                new StructureBlockEntity(
                        new BlockPos(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ()),
                        Blocks.STRUCTURE_BLOCK.defaultBlockState()
                ),
                __ -> (net.minecraft.nbt.CompoundTag) fromNativeBinary(nbtData)
        ));
    }

    /*@Override
    public void sendFakeNBT(Player player, BlockVector3 pos, CompoundTag nbtData) {
        ((CraftPlayer) player).getHandle().connection.send(ClientboundBlockEntityDataPacket.create(
                new StructureBlockEntity(
                        new BlockPos(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ()),
                        Blocks.STRUCTURE_BLOCK.defaultBlockState()
                ),
                __ -> (net.minecraft.nbt.CompoundTag) fromNative(nbtData)
        ));
    }*/

    @Override
    public void sendFakeOP(Player player) {
        ((CraftPlayer) player).getHandle().connection.send(new ClientboundEntityEventPacket(
                ((CraftPlayer) player).getHandle(), (byte) 28
        ));
    }

    @Override
    public org.bukkit.inventory.ItemStack adapt(BaseItemStack item) {
        ItemStack stack = new ItemStack(
                DedicatedServer.getServer().registryAccess().registryOrThrow(Registries.ITEM).get(ResourceLocation.tryParse(item.getType().getId())),
                item.getAmount()
        );
        stack.setTag(((net.minecraft.nbt.CompoundTag) fromNative(item.getNbtData())));
        return CraftItemStack.asCraftMirror(stack);
    }

    @Override
    public BaseItemStack adapt(org.bukkit.inventory.ItemStack itemStack) {
        final ItemStack nmsStack = CraftItemStack.asNMSCopy(itemStack);
        final BaseItemStack weStack = new BaseItemStack(BukkitAdapter.asItemType(itemStack.getType()), itemStack.getAmount());
        weStack.setNbt(((CompoundBinaryTag) toNativeBinary(nmsStack.getTag())));
        return weStack;
    }

    private final LoadingCache<ServerLevel, PaperweightFakePlayer> fakePlayers
            = CacheBuilder.newBuilder().weakKeys().softValues().build(CacheLoader.from(PaperweightFakePlayer::new));

    @Override
    public boolean simulateItemUse(org.bukkit.World world, BlockVector3 position, BaseItem item, Direction face) {
        CraftWorld craftWorld = (CraftWorld) world;
        ServerLevel worldServer = craftWorld.getHandle();
        ItemStack stack = CraftItemStack.asNMSCopy(BukkitAdapter.adapt(item instanceof BaseItemStack
                ? ((BaseItemStack) item) : new BaseItemStack(item.getType(), item.getNbtData(), 1)));
        stack.setTag((net.minecraft.nbt.CompoundTag) fromNative(item.getNbtData()));

        PaperweightFakePlayer fakePlayer;
        try {
            fakePlayer = fakePlayers.get(worldServer);
        } catch (ExecutionException ignored) {
            return false;
        }
        fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, stack);
        fakePlayer.absMoveTo(position.getBlockX(), position.getBlockY(), position.getBlockZ(),
                (float) face.toVector().toYaw(), (float) face.toVector().toPitch());

        final BlockPos blockPos = new BlockPos(position.getBlockX(), position.getBlockY(), position.getBlockZ());
        final Vec3 blockVec = Vec3.atLowerCornerOf(blockPos);
        final net.minecraft.core.Direction enumFacing = adapt(face);
        BlockHitResult rayTrace = new BlockHitResult(blockVec, enumFacing, blockPos, false);
        UseOnContext context = new UseOnContext(fakePlayer, InteractionHand.MAIN_HAND, rayTrace);
        InteractionResult result = stack.useOn(context);
        if (result != InteractionResult.SUCCESS) {
            if (worldServer.getBlockState(blockPos).use(worldServer, fakePlayer, InteractionHand.MAIN_HAND, rayTrace).consumesAction()) {
                result = InteractionResult.SUCCESS;
            } else {
                result = stack.getItem().use(worldServer, fakePlayer, InteractionHand.MAIN_HAND).getResult();
            }
        }

        return result == InteractionResult.SUCCESS;
    }

    @Override
    public boolean canPlaceAt(org.bukkit.World world, BlockVector3 position, BlockState blockState) {
        int internalId = BlockStateIdAccess.getBlockStateId(blockState);
        net.minecraft.world.level.block.state.BlockState blockData = Block.stateById(internalId);
        return blockData.canSurvive(((CraftWorld) world).getHandle(), new BlockPos(position.getX(), position.getY(), position.getZ()));
    }

    @Override
    public boolean regenerate(org.bukkit.World bukkitWorld, Region region, Extent extent, RegenOptions options) {
        try {
            doRegen(bukkitWorld, region, extent, options);
        } catch (Exception e) {
            throw new IllegalStateException("Regen failed.", e);
        }

        return true;
    }

    private void doRegen(org.bukkit.World bukkitWorld, Region region, Extent extent, RegenOptions options) throws Exception {
        Environment env = bukkitWorld.getEnvironment();
        ChunkGenerator gen = bukkitWorld.getGenerator();

        Path tempDir = Files.createTempDirectory("WorldEditWorldGen");
        LevelStorageSource levelStorage = LevelStorageSource.createDefault(tempDir);
        ResourceKey<LevelStem> worldDimKey = getWorldDimKey(env);
        try (LevelStorageSource.LevelStorageAccess session = levelStorage.createAccess("faweregentempworld", worldDimKey)) {
            ServerLevel originalWorld = ((CraftWorld) bukkitWorld).getHandle();
            PrimaryLevelData levelProperties = (PrimaryLevelData) originalWorld.getServer()
                    .getWorldData().overworldData();
            WorldOptions originalOpts = levelProperties.worldGenOptions();

            long seed = options.getSeed().orElse(originalWorld.getSeed());
            WorldOptions newOpts = options.getSeed().isPresent()
                    ? originalOpts.withSeed(OptionalLong.of(seed))
                    : originalOpts;

            LevelSettings newWorldSettings = new LevelSettings(
                    "faweregentempworld",
                    levelProperties.settings.gameType(),
                    levelProperties.settings.hardcore(),
                    levelProperties.settings.difficulty(),
                    levelProperties.settings.allowCommands(),
                    levelProperties.settings.gameRules(),
                    levelProperties.settings.getDataConfiguration()
            );

            PrimaryLevelData.SpecialWorldProperty specialWorldProperty =
                    levelProperties.isFlatWorld()
                            ? PrimaryLevelData.SpecialWorldProperty.FLAT
                            : levelProperties.isDebugWorld()
                                    ? PrimaryLevelData.SpecialWorldProperty.DEBUG
                                    : PrimaryLevelData.SpecialWorldProperty.NONE;

            PrimaryLevelData newWorldData = new PrimaryLevelData(newWorldSettings, newOpts, specialWorldProperty, Lifecycle.stable());

            ServerLevel freshWorld = new ServerLevel(
                    originalWorld.getServer(),
                    originalWorld.getServer().executor,
                    session, newWorldData,
                    originalWorld.dimension(),
                    new LevelStem(
                            originalWorld.dimensionTypeRegistration(),
                            originalWorld.getChunkSource().getGenerator()
                    ),
                    new NoOpWorldLoadListener(),
                    originalWorld.isDebug(),
                    seed,
                    ImmutableList.of(),
                    false,
                    originalWorld.getRandomSequences(),
                    env,
                    gen,
                    bukkitWorld.getBiomeProvider()
            );
            try {
                regenForWorld(region, extent, freshWorld, options);
            } finally {
                freshWorld.getChunkSource().close(false);
            }
        } finally {
            try {
                @SuppressWarnings("unchecked")
                Map<String, org.bukkit.World> map = (Map<String, org.bukkit.World>) serverWorldsField.get(Bukkit.getServer());
                map.remove("faweregentempworld");
            } catch (IllegalAccessException ignored) {
            }
            SafeFiles.tryHardToDeleteDir(tempDir);
        }
    }

    private BiomeType adapt(ServerLevel serverWorld, Biome origBiome) {
        ResourceLocation key = serverWorld.registryAccess().registryOrThrow(Registries.BIOME).getKey(origBiome);
        if (key == null) {
            return null;
        }
        return BiomeTypes.get(key.toString());
    }

    @SuppressWarnings("unchecked")
    private void regenForWorld(Region region, Extent extent, ServerLevel serverWorld, RegenOptions options) throws WorldEditException {
        List<CompletableFuture<ChunkAccess>> chunkLoadings = submitChunkLoadTasks(region, serverWorld);
        BlockableEventLoop<Runnable> executor;
        try {
            executor = (BlockableEventLoop<Runnable>) chunkProviderExecutorField.get(serverWorld.getChunkSource());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Couldn't get executor for chunk loading.", e);
        }
        executor.managedBlock(() -> {
            // bail out early if a future fails
            if (chunkLoadings.stream().anyMatch(ftr ->
                    ftr.isDone() && Futures.getUnchecked(ftr) == null
            )) {
                return false;
            }
            return chunkLoadings.stream().allMatch(CompletableFuture::isDone);
        });
        Map<ChunkPos, ChunkAccess> chunks = new HashMap<>();
        for (CompletableFuture<ChunkAccess> future : chunkLoadings) {
            @Nullable
            ChunkAccess chunk = future.getNow(null);
            checkState(chunk != null, "Failed to generate a chunk, regen failed.");
            chunks.put(chunk.getPos(), chunk);
        }

        for (BlockVector3 vec : region) {
            BlockPos pos = new BlockPos(vec.getBlockX(), vec.getBlockY(), vec.getBlockZ());
            ChunkAccess chunk = chunks.get(new ChunkPos(pos));
            final net.minecraft.world.level.block.state.BlockState blockData = chunk.getBlockState(pos);
            int internalId = Block.getId(blockData);
            BlockStateHolder<?> state = BlockStateIdAccess.getBlockStateById(internalId);
            Objects.requireNonNull(state);
            BlockEntity blockEntity = chunk.getBlockEntity(pos);
            if (blockEntity != null) {
                net.minecraft.nbt.CompoundTag tag = blockEntity.saveWithId();
                state = state.toBaseBlock(((CompoundBinaryTag) toNativeBinary(tag)));
            }
            extent.setBlock(vec, state.toBaseBlock());
            if (options.shouldRegenBiomes()) {
                Biome origBiome = chunk.getNoiseBiome(vec.getX(), vec.getY(), vec.getZ()).value();
                BiomeType adaptedBiome = adapt(serverWorld, origBiome);
                if (adaptedBiome != null) {
                    extent.setBiome(vec, adaptedBiome);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<CompletableFuture<ChunkAccess>> submitChunkLoadTasks(Region region, ServerLevel serverWorld) {
        ServerChunkCache chunkManager = serverWorld.getChunkSource();
        List<CompletableFuture<ChunkAccess>> chunkLoadings = new ArrayList<>();
        // Pre-gen all the chunks
        for (BlockVector2 chunk : region.getChunks()) {
            try {
                //noinspection unchecked
                chunkLoadings.add(
                        ((CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>)
                                getChunkFutureMethod.invoke(chunkManager, chunk.getX(), chunk.getZ(), ChunkStatus.FEATURES, true))
                                .thenApply(either -> either.left().orElse(null))
                );
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new IllegalStateException("Couldn't load chunk for regen.", e);
            }
        }
        return chunkLoadings;
    }

    private ResourceKey<LevelStem> getWorldDimKey(Environment env) {
        switch (env) {
            case NETHER:
                return LevelStem.NETHER;
            case THE_END:
                return LevelStem.END;
            case NORMAL:
            default:
                return LevelStem.OVERWORLD;
        }
    }

    private static final Set<SideEffect> SUPPORTED_SIDE_EFFECTS = Sets.immutableEnumSet(
            SideEffect.NEIGHBORS,
            SideEffect.LIGHTING,
            SideEffect.VALIDATION,
            SideEffect.ENTITY_AI,
            SideEffect.EVENTS,
            SideEffect.UPDATE
    );

    @Override
    public Set<SideEffect> getSupportedSideEffects() {
        return SUPPORTED_SIDE_EFFECTS;
    }

    @Override
    public boolean clearContainerBlockContents(org.bukkit.World world, BlockVector3 pt) {
        ServerLevel originalWorld = ((CraftWorld) world).getHandle();

        BlockEntity entity = originalWorld.getBlockEntity(new BlockPos(pt.getBlockX(), pt.getBlockY(), pt.getBlockZ()));
        if (entity instanceof Clearable) {
            ((Clearable) entity).clearContent();
            return true;
        }
        return false;
    }

    /*@Override
    public void initializeRegistries() {
        DedicatedServer server = ((CraftServer) Bukkit.getServer()).getServer();
        // Biomes
        for (ResourceLocation name : server.registryAccess().registryOrThrow(Registries.BIOME).keySet()) {
            if (BiomeType.REGISTRY.get(name.toString()) == null) {
                BiomeType.REGISTRY.register(name.toString(), new BiomeType(name.toString()));
            }
        }
    }*

    // ------------------------------------------------------------------------
    // Code that is less likely to break
    // ------------------------------------------------------------------------

    /**
     * Converts from a non-native NMS NBT structure to a native WorldEdit NBT
     * structure.
     *
     * @param foreign non-native NMS NBT structure
     * @return native WorldEdit NBT structure
     */
    @Override
    public BinaryTag toNativeBinary(net.minecraft.nbt.Tag foreign) {
        if (foreign == null) {
            return null;
        }
        if (foreign instanceof net.minecraft.nbt.CompoundTag) {
            Map<String, BinaryTag> values = new HashMap<>();
            Set<String> foreignKeys = ((net.minecraft.nbt.CompoundTag) foreign).getAllKeys();

            for (String str : foreignKeys) {
                net.minecraft.nbt.Tag base = ((net.minecraft.nbt.CompoundTag) foreign).get(str);
                values.put(str, toNativeBinary(base));
            }
            return CompoundBinaryTag.from(values);
        } else if (foreign instanceof net.minecraft.nbt.ByteTag) {
            return ByteBinaryTag.of(((net.minecraft.nbt.ByteTag) foreign).getAsByte());
        } else if (foreign instanceof net.minecraft.nbt.ByteArrayTag) {
            return ByteArrayBinaryTag.of(((net.minecraft.nbt.ByteArrayTag) foreign).getAsByteArray());
        } else if (foreign instanceof net.minecraft.nbt.DoubleTag) {
            return DoubleBinaryTag.of(((net.minecraft.nbt.DoubleTag) foreign).getAsDouble());
        } else if (foreign instanceof net.minecraft.nbt.FloatTag) {
            return FloatBinaryTag.of(((net.minecraft.nbt.FloatTag) foreign).getAsFloat());
        } else if (foreign instanceof net.minecraft.nbt.IntTag) {
            return IntBinaryTag.of(((net.minecraft.nbt.IntTag) foreign).getAsInt());
        } else if (foreign instanceof net.minecraft.nbt.IntArrayTag) {
            return IntArrayBinaryTag.of(((net.minecraft.nbt.IntArrayTag) foreign).getAsIntArray());
        } else if (foreign instanceof net.minecraft.nbt.LongArrayTag) {
            return LongArrayBinaryTag.of(((net.minecraft.nbt.LongArrayTag) foreign).getAsLongArray());
        } else if (foreign instanceof net.minecraft.nbt.ListTag) {
            try {
                return toNativeList((net.minecraft.nbt.ListTag) foreign);
            } catch (Throwable e) {
                LOGGER.log(Level.WARNING, "Failed to convert net.minecraft.nbt.ListTag", e);
                return ListBinaryTag.empty();
            }
        } else if (foreign instanceof net.minecraft.nbt.LongTag) {
            return LongBinaryTag.of(((net.minecraft.nbt.LongTag) foreign).getAsLong());
        } else if (foreign instanceof net.minecraft.nbt.ShortTag) {
            return ShortBinaryTag.of(((net.minecraft.nbt.ShortTag) foreign).getAsShort());
        } else if (foreign instanceof net.minecraft.nbt.StringTag) {
            return StringBinaryTag.of(foreign.getAsString());
        } else if (foreign instanceof net.minecraft.nbt.EndTag) {
            return EndBinaryTag.get();
        } else {
            throw new IllegalArgumentException("Don't know how to make native " + foreign.getClass().getCanonicalName());
        }
    }

    /**
     * Convert a foreign NBT list tag into a native WorldEdit one.
     *
     * @param foreign the foreign tag
     * @return the converted tag
     * @throws SecurityException on error
     * @throws IllegalArgumentException on error
     */
    private ListBinaryTag toNativeList(net.minecraft.nbt.ListTag foreign) throws SecurityException, IllegalArgumentException {
        ListBinaryTag.Builder values = ListBinaryTag.builder();

        for (net.minecraft.nbt.Tag tag : foreign) {
            values.add(toNativeBinary(tag));
        }

        return values.build();
    }

    /**
     * Converts a WorldEdit-native NBT structure to a NMS structure.
     *
     * @param foreign structure to convert
     * @return non-native structure
     */
    @Override
    public net.minecraft.nbt.Tag fromNativeBinary(BinaryTag foreign) {
        if (foreign == null) {
            return null;
        }
        if (foreign instanceof CompoundBinaryTag) {
            net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
            for (String key : ((CompoundBinaryTag) foreign).keySet()) {
                tag.put(key, fromNativeBinary(((CompoundBinaryTag) foreign).get(key)));
            }
            return tag;
        } else if (foreign instanceof ByteBinaryTag) {
            return net.minecraft.nbt.ByteTag.valueOf(((ByteBinaryTag) foreign).value());
        } else if (foreign instanceof ByteArrayBinaryTag) {
            return new net.minecraft.nbt.ByteArrayTag(((ByteArrayBinaryTag) foreign).value());
        } else if (foreign instanceof DoubleBinaryTag) {
            return net.minecraft.nbt.DoubleTag.valueOf(((DoubleBinaryTag) foreign).value());
        } else if (foreign instanceof FloatBinaryTag) {
            return net.minecraft.nbt.FloatTag.valueOf(((FloatBinaryTag) foreign).value());
        } else if (foreign instanceof IntBinaryTag) {
            return net.minecraft.nbt.IntTag.valueOf(((IntBinaryTag) foreign).value());
        } else if (foreign instanceof IntArrayBinaryTag) {
            return new net.minecraft.nbt.IntArrayTag(((IntArrayBinaryTag) foreign).value());
        } else if (foreign instanceof LongArrayBinaryTag) {
            return new net.minecraft.nbt.LongArrayTag(((LongArrayBinaryTag) foreign).value());
        } else if (foreign instanceof ListBinaryTag) {
            net.minecraft.nbt.ListTag tag = new net.minecraft.nbt.ListTag();
            ListBinaryTag foreignList = (ListBinaryTag) foreign;
            for (BinaryTag t : foreignList) {
                tag.add(fromNativeBinary(t));
            }
            return tag;
        } else if (foreign instanceof LongBinaryTag) {
            return net.minecraft.nbt.LongTag.valueOf(((LongBinaryTag) foreign).value());
        } else if (foreign instanceof ShortBinaryTag) {
            return net.minecraft.nbt.ShortTag.valueOf(((ShortBinaryTag) foreign).value());
        } else if (foreign instanceof StringBinaryTag) {
            return net.minecraft.nbt.StringTag.valueOf(((StringBinaryTag) foreign).value());
        } else if (foreign instanceof EndBinaryTag) {
            return net.minecraft.nbt.EndTag.INSTANCE;
        } else {
            throw new IllegalArgumentException("Don't know how to make NMS " + foreign.getClass().getCanonicalName());
        }
    }

    @Override
    public boolean supportsWatchdog() {
        return watchdog != null;
    }

    @Override
    public void tickWatchdog() {
        watchdog.tick();
    }

    private class SpigotWatchdog implements Watchdog {
        private final Field instanceField;
        private final Field lastTickField;

        SpigotWatchdog() throws NoSuchFieldException {
            Field instanceField = WatchdogThread.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            this.instanceField = instanceField;

            Field lastTickField = WatchdogThread.class.getDeclaredField("lastTick");
            lastTickField.setAccessible(true);
            this.lastTickField = lastTickField;
        }

        @Override
        public void tick() {
            try {
                WatchdogThread instance = (WatchdogThread) this.instanceField.get(null);
                if ((long) lastTickField.get(instance) != 0) {
                    WatchdogThread.tick();
                }
            } catch (IllegalAccessException e) {
                LOGGER.log(Level.WARNING, "Failed to tick watchdog", e);
            }
        }
    }

    private static class MojangWatchdog implements Watchdog {
        private final DedicatedServer server;
        private final Field tickField;

        MojangWatchdog(DedicatedServer server) throws NoSuchFieldException {
            this.server = server;
            Field tickField = MinecraftServer.class.getDeclaredField(
                    Refraction.pickName("nextTickTime", "ah")
            );
            if (tickField.getType() != long.class) {
                throw new IllegalStateException("nextTickTime is not a long field, mapping is likely incorrect");
            }
            tickField.setAccessible(true);
            this.tickField = tickField;
        }

        @Override
        public void tick() {
            try {
                tickField.set(server, Util.getMillis());
            } catch (IllegalAccessException ignored) {
            }
        }
    }

    private static class NoOpWorldLoadListener implements ChunkProgressListener {
        @Override
        public void updateSpawnPos(ChunkPos spawnPos) {
        }

        @Override
        public void onStatusChange(ChunkPos pos, @org.jetbrains.annotations.Nullable ChunkStatus status) {
        }

        @Override
        public void start() {
        }

        @Override
        public void stop() {
        }

        @Override
        public void setChunkRadius(int radius) {
        }
    }
}
