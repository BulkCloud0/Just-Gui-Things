package com.bulkcloud0.justguithings.world.tile;

import com.bulkcloud0.justguithings.logistics.ItemPipeSideMode;
import com.bulkcloud0.justguithings.logistics.ItemTransferHelper;
import com.bulkcloud0.justguithings.registry.ModTileEntities;
import com.bulkcloud0.justguithings.world.block.AbstractConduitBlock;
import net.minecraft.block.BlockState;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class BasicItemPipeTileEntity extends TileEntity implements ITickableTileEntity {
    public static final int TRANSFER_RATE = 8;
    private static final int NETWORK_CACHE_TTL = 100;
    private static final int VISUAL_REFRESH_INTERVAL = 10;

    private final EnumMap<Direction, ItemPipeSideMode> sideModes = new EnumMap<>(Direction.class);

    private List<BasicItemPipeTileEntity> cachedNetwork = Collections.emptyList();
    private BlockPos cachedController;
    private long cacheValidUntil;
    private int sourceCursor;
    private int targetCursor;

    public BasicItemPipeTileEntity() {
        super(ModTileEntities.BASIC_ITEM_PIPE.get());
        for (Direction direction : Direction.values()) {
            sideModes.put(direction, ItemPipeSideMode.BOTH);
        }
    }

    @Override
    public void tick() {
        if (level == null || level.isClientSide) {
            return;
        }

        if ((level.getGameTime() + worldPosition.asLong()) % VISUAL_REFRESH_INTERVAL == 0L) {
            AbstractConduitBlock.refreshConnections(level, worldPosition);
        }

        ensureNetworkCache();
        if (cachedController == null || !worldPosition.equals(cachedController)) {
            return;
        }

        transferItems(cachedNetwork);
    }

    public ItemPipeSideMode getSideMode(Direction direction) {
        return sideModes.getOrDefault(direction, ItemPipeSideMode.BOTH);
    }

    public ItemPipeSideMode cycleSideMode(Direction direction) {
        ItemPipeSideMode next = getSideMode(direction).next();
        sideModes.put(direction, next);
        setChanged();
        return next;
    }

    private void ensureNetworkCache() {
        if (level == null) {
            return;
        }

        long gameTime = level.getGameTime();
        if (!cachedNetwork.isEmpty() && cachedController != null && gameTime < cacheValidUntil) {
            return;
        }

        rebuildNetworkCache(gameTime);
    }

    private void rebuildNetworkCache(long gameTime) {
        List<BasicItemPipeTileEntity> discovered = discoverNetwork();
        if (discovered.isEmpty()) {
            clearNetworkCacheLocal();
            return;
        }

        BlockPos controller = discovered.get(0).getBlockPos();
        for (BasicItemPipeTileEntity pipe : discovered) {
            if (pipe.getBlockPos().asLong() < controller.asLong()) {
                controller = pipe.getBlockPos();
            }
        }

        List<BasicItemPipeTileEntity> sharedNetwork = Collections.unmodifiableList(new ArrayList<>(discovered));
        long validUntil = gameTime + NETWORK_CACHE_TTL;
        for (BasicItemPipeTileEntity pipe : discovered) {
            pipe.cachedNetwork = sharedNetwork;
            pipe.cachedController = controller;
            pipe.cacheValidUntil = validUntil;
        }
    }

    private List<BasicItemPipeTileEntity> discoverNetwork() {
        List<BasicItemPipeTileEntity> pipes = new ArrayList<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();

        queue.add(worldPosition);
        visited.add(worldPosition);

        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            TileEntity tile = level.getBlockEntity(pos);
            if (!(tile instanceof BasicItemPipeTileEntity)) {
                continue;
            }

            BasicItemPipeTileEntity pipe = (BasicItemPipeTileEntity) tile;
            pipes.add(pipe);

            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (!visited.add(next)) {
                    continue;
                }

                TileEntity nextTile = level.getBlockEntity(next);
                if (nextTile instanceof BasicItemPipeTileEntity) {
                    queue.add(next);
                }
            }
        }

        return pipes;
    }

    public void invalidateNetworkCache() {
        if (cachedNetwork.isEmpty()) {
            clearNetworkCacheLocal();
            return;
        }

        List<BasicItemPipeTileEntity> previousNetwork = new ArrayList<>(cachedNetwork);
        for (BasicItemPipeTileEntity pipe : previousNetwork) {
            pipe.clearNetworkCacheLocal();
        }
    }

    private void clearNetworkCacheLocal() {
        cachedNetwork = Collections.emptyList();
        cachedController = null;
        cacheValidUntil = 0L;
        sourceCursor = 0;
        targetCursor = 0;
    }

    private void transferItems(List<BasicItemPipeTileEntity> network) {
        Set<BlockPos> pipePositions = new HashSet<>();
        for (BasicItemPipeTileEntity pipe : network) {
            if (pipe.isRemoved()) {
                invalidateNetworkCache();
                return;
            }
            pipePositions.add(pipe.getBlockPos());
        }

        List<ItemEndpoint> sources = new ArrayList<>();
        List<ItemEndpoint> targets = new ArrayList<>();
        collectEndpoints(network, pipePositions, sources, targets);

        if (sources.isEmpty() || targets.isEmpty()) {
            return;
        }

        int budget = TRANSFER_RATE;
        int sourceStart = Math.floorMod(sourceCursor, sources.size());

        for (int sourceOffset = 0; sourceOffset < sources.size() && budget > 0; sourceOffset++) {
            ItemEndpoint source = sources.get((sourceStart + sourceOffset) % sources.size());

            for (int slot = 0; slot < source.handler.getSlots() && budget > 0; slot++) {
                ItemStack simulated = source.handler.extractItem(slot, budget, true);
                if (simulated.isEmpty()) {
                    continue;
                }

                int targetStart = Math.floorMod(targetCursor, targets.size());
                for (int targetOffset = 0; targetOffset < targets.size() && budget > 0; targetOffset++) {
                    int targetIndex = (targetStart + targetOffset) % targets.size();
                    ItemEndpoint target = targets.get(targetIndex);
                    if (target.inventoryPos.equals(source.inventoryPos)) {
                        continue;
                    }

                    int moved = moveItem(source, slot, target, budget);
                    if (moved > 0) {
                        budget -= moved;
                        targetCursor = (targetIndex + 1) % targets.size();
                        break;
                    }
                }
            }
        }

        sourceCursor = (sourceStart + 1) % sources.size();
    }

    private void collectEndpoints(List<BasicItemPipeTileEntity> network,
                                  Set<BlockPos> pipePositions,
                                  List<ItemEndpoint> sources,
                                  List<ItemEndpoint> targets) {
        Set<EndpointKey> sourceKeys = new HashSet<>();
        Set<EndpointKey> targetKeys = new HashSet<>();

        for (BasicItemPipeTileEntity pipe : network) {
            for (Direction direction : Direction.values()) {
                ItemPipeSideMode mode = pipe.getSideMode(direction);
                if (mode == ItemPipeSideMode.DISABLED) {
                    continue;
                }

                BlockPos neighborPos = pipe.getBlockPos().relative(direction);
                if (pipePositions.contains(neighborPos)) {
                    continue;
                }

                TileEntity neighbor = level.getBlockEntity(neighborPos);
                if (neighbor == null) {
                    continue;
                }

                IItemHandler handler = neighbor
                        .getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, direction.getOpposite())
                        .orElse(null);
                if (handler == null) {
                    continue;
                }

                EndpointKey key = new EndpointKey(neighborPos, direction.getOpposite());
                if (mode.canPull() && sourceKeys.add(key)) {
                    sources.add(new ItemEndpoint(neighborPos, handler));
                }
                if (mode.canPush() && targetKeys.add(key)) {
                    targets.add(new ItemEndpoint(neighborPos, handler));
                }
            }
        }
    }

    private int moveItem(ItemEndpoint source, int sourceSlot, ItemEndpoint target, int maxAmount) {
        ItemStack simulatedExtract = source.handler.extractItem(sourceSlot, maxAmount, true);
        if (simulatedExtract.isEmpty()) {
            return 0;
        }

        ItemStack simulatedRemainder = ItemTransferHelper.insert(target.handler, simulatedExtract, true);
        int accepted = simulatedExtract.getCount() - simulatedRemainder.getCount();
        if (accepted <= 0) {
            return 0;
        }

        ItemStack extracted = source.handler.extractItem(sourceSlot, accepted, false);
        if (extracted.isEmpty()) {
            return 0;
        }

        ItemStack remainder = ItemTransferHelper.insert(target.handler, extracted, false);
        int inserted = extracted.getCount() - remainder.getCount();

        if (!remainder.isEmpty()) {
            ItemStack returned = source.handler.insertItem(sourceSlot, remainder, false);
            if (!returned.isEmpty() && level != null) {
                InventoryHelper.dropItemStack(level,
                        source.inventoryPos.getX() + 0.5D,
                        source.inventoryPos.getY() + 0.5D,
                        source.inventoryPos.getZ() + 0.5D,
                        returned);
            }
        }

        return inserted;
    }

    @Override
    public void load(BlockState state, CompoundNBT nbt) {
        super.load(state, nbt);

        if (nbt.contains("SideConfig")) {
            CompoundNBT config = nbt.getCompound("SideConfig");
            for (Direction direction : Direction.values()) {
                String key = "Side" + direction.ordinal();
                if (config.contains(key)) {
                    sideModes.put(direction, ItemPipeSideMode.fromOrdinal(config.getInt(key)));
                }
            }
        }

        clearNetworkCacheLocal();
    }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        super.save(nbt);

        CompoundNBT config = new CompoundNBT();
        for (Direction direction : Direction.values()) {
            config.putInt("Side" + direction.ordinal(), getSideMode(direction).ordinal());
        }
        nbt.put("SideConfig", config);
        return nbt;
    }

    @Override
    public void setRemoved() {
        invalidateNetworkCache();
        super.setRemoved();
    }

    private static final class ItemEndpoint {
        private final BlockPos inventoryPos;
        private final IItemHandler handler;

        private ItemEndpoint(BlockPos inventoryPos, IItemHandler handler) {
            this.inventoryPos = inventoryPos;
            this.handler = handler;
        }
    }

    private static final class EndpointKey {
        private final BlockPos pos;
        private final Direction side;

        private EndpointKey(BlockPos pos, Direction side) {
            this.pos = pos.immutable();
            this.side = side;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof EndpointKey)) {
                return false;
            }
            EndpointKey that = (EndpointKey) other;
            return pos.equals(that.pos) && side == that.side;
        }

        @Override
        public int hashCode() {
            return 31 * pos.hashCode() + side.hashCode();
        }
    }
}
