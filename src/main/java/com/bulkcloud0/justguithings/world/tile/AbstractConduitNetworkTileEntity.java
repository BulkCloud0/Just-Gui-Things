package com.bulkcloud0.justguithings.world.tile;

import com.bulkcloud0.justguithings.world.block.AbstractConduitBlock;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public abstract class AbstractConduitNetworkTileEntity<T extends AbstractConduitNetworkTileEntity<T>>
        extends TileEntity implements ITickableTileEntity {
    private final int networkCacheTtl;

    private List<T> cachedNetwork = Collections.emptyList();
    private BlockPos cachedController;
    private long cacheValidUntil;

    protected AbstractConduitNetworkTileEntity(TileEntityType<?> tileEntityType, int networkCacheTtl) {
        super(tileEntityType);
        this.networkCacheTtl = Math.max(1, networkCacheTtl);
    }

    protected abstract Class<T> getNetworkNodeClass();

    protected final void tickNetworkMaintenance(int visualRefreshInterval) {
        if (level == null || level.isClientSide) {
            return;
        }

        if (visualRefreshInterval > 0
                && (level.getGameTime() + worldPosition.asLong()) % visualRefreshInterval == 0L) {
            AbstractConduitBlock.refreshConnections(level, worldPosition);
        }

        ensureNetworkCache();
    }

    protected final boolean isNetworkController() {
        return cachedController != null && worldPosition.equals(cachedController);
    }

    protected final List<T> getCachedNetwork() {
        return cachedNetwork;
    }

    public final void invalidateNetworkCache() {
        if (cachedNetwork.isEmpty()) {
            clearNetworkCacheLocal();
            return;
        }

        List<T> previousNetwork = new ArrayList<>(cachedNetwork);
        for (T node : previousNetwork) {
            AbstractConduitNetworkTileEntity<T> base = node;
            base.clearNetworkCacheLocal();
        }
    }

    protected void onNetworkCacheCleared() {
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
        List<T> discovered = discoverNetwork();
        if (discovered.isEmpty()) {
            clearNetworkCacheLocal();
            return;
        }

        BlockPos controller = discovered.get(0).getBlockPos();
        for (T node : discovered) {
            if (node.getBlockPos().asLong() < controller.asLong()) {
                controller = node.getBlockPos();
            }
        }

        List<T> sharedNetwork = Collections.unmodifiableList(new ArrayList<>(discovered));
        long validUntil = gameTime + networkCacheTtl;
        for (T node : discovered) {
            AbstractConduitNetworkTileEntity<T> base = node;
            base.cachedNetwork = sharedNetwork;
            base.cachedController = controller;
            base.cacheValidUntil = validUntil;
        }
    }

    private List<T> discoverNetwork() {
        List<T> nodes = new ArrayList<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        Class<T> nodeClass = getNetworkNodeClass();

        queue.add(worldPosition);
        visited.add(worldPosition);

        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            TileEntity tile = level.getBlockEntity(pos);
            if (!nodeClass.isInstance(tile)) {
                continue;
            }

            T node = nodeClass.cast(tile);
            nodes.add(node);

            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (!visited.add(next)) {
                    continue;
                }

                TileEntity nextTile = level.getBlockEntity(next);
                if (nodeClass.isInstance(nextTile)) {
                    queue.add(next);
                }
            }
        }

        return nodes;
    }

    private void clearNetworkCacheLocal() {
        cachedNetwork = Collections.emptyList();
        cachedController = null;
        cacheValidUntil = 0L;
        onNetworkCacheCleared();
    }

    @Override
    public void setRemoved() {
        invalidateNetworkCache();
        super.setRemoved();
    }
}
