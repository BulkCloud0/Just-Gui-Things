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

import javax.annotation.Nullable;

public abstract class AbstractConduitNetworkTileEntity<T extends AbstractConduitNetworkTileEntity<T>>
        extends TileEntity implements ITickableTileEntity {
    private static final int EXTERNAL_ENDPOINT_CACHE_TTL = 20;

    private final int networkCacheTtl;

    private List<T> cachedNetwork = Collections.emptyList();
    private Set<BlockPos> cachedNetworkPositions = Collections.emptySet();
    private BlockPos cachedController;
    private long cacheValidUntil;

    private List<ExternalEndpoint<T>> cachedExternalEndpoints = Collections.emptyList();
    private long externalEndpointsValidUntil;

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

    protected final List<ExternalEndpoint<T>> getCachedExternalEndpoints() {
        if (level == null || cachedNetwork.isEmpty()) {
            return Collections.emptyList();
        }

        long gameTime = level.getGameTime();
        if (gameTime < externalEndpointsValidUntil) {
            return cachedExternalEndpoints;
        }

        rebuildExternalEndpointCache(gameTime);
        return cachedExternalEndpoints;
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

    public final void invalidateExternalEndpointCache() {
        if (cachedNetwork.isEmpty()) {
            clearExternalEndpointCacheLocal();
            return;
        }

        List<T> previousNetwork = new ArrayList<>(cachedNetwork);
        for (T node : previousNetwork) {
            AbstractConduitNetworkTileEntity<T> base = node;
            base.clearExternalEndpointCacheLocal();
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
        Set<BlockPos> discoveredPositions = new HashSet<>();
        for (T node : discovered) {
            BlockPos nodePos = node.getBlockPos();
            discoveredPositions.add(nodePos.immutable());
            if (nodePos.asLong() < controller.asLong()) {
                controller = nodePos;
            }
        }

        List<T> sharedNetwork = Collections.unmodifiableList(new ArrayList<>(discovered));
        Set<BlockPos> sharedNetworkPositions =
                Collections.unmodifiableSet(discoveredPositions);
        long validUntil = gameTime + networkCacheTtl;
        for (T node : discovered) {
            AbstractConduitNetworkTileEntity<T> base = node;
            base.cachedNetwork = sharedNetwork;
            base.cachedNetworkPositions = sharedNetworkPositions;
            base.cachedController = controller;
            base.cacheValidUntil = validUntil;
            base.clearExternalEndpointCacheLocal();
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
            TileEntity tile = getLoadedBlockEntity(pos);
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

                TileEntity nextTile = getLoadedBlockEntity(next);
                if (nodeClass.isInstance(nextTile)) {
                    queue.add(next);
                }
            }
        }

        return nodes;
    }

    private void rebuildExternalEndpointCache(long gameTime) {
        if (level == null || cachedNetwork.isEmpty()) {
            clearExternalEndpointCacheLocal();
            return;
        }

        for (T node : cachedNetwork) {
            if (node.isRemoved()) {
                invalidateNetworkCache();
                return;
            }
        }

        List<ExternalEndpoint<T>> endpoints = new ArrayList<>();
        for (T node : cachedNetwork) {
            for (Direction direction : Direction.values()) {
                BlockPos neighborPos = node.getBlockPos().relative(direction);
                if (cachedNetworkPositions.contains(neighborPos)) {
                    continue;
                }

                if (getLoadedBlockEntity(neighborPos) == null) {
                    continue;
                }

                endpoints.add(new ExternalEndpoint<>(
                        node,
                        direction,
                        neighborPos.immutable(),
                        direction.getOpposite()));
            }
        }

        cachedExternalEndpoints = Collections.unmodifiableList(endpoints);
        externalEndpointsValidUntil = gameTime + EXTERNAL_ENDPOINT_CACHE_TTL;
    }

    @Nullable
    protected final TileEntity getLoadedBlockEntity(BlockPos pos) {
        if (level == null || !level.hasChunkAt(pos)) {
            return null;
        }
        return level.getBlockEntity(pos);
    }

    private void clearNetworkCacheLocal() {
        cachedNetwork = Collections.emptyList();
        cachedNetworkPositions = Collections.emptySet();
        cachedController = null;
        cacheValidUntil = 0L;
        clearExternalEndpointCacheLocal();
        onNetworkCacheCleared();
    }

    private void clearExternalEndpointCacheLocal() {
        cachedExternalEndpoints = Collections.emptyList();
        externalEndpointsValidUntil = 0L;
    }

    @Override
    public void setRemoved() {
        invalidateNetworkCache();
        super.setRemoved();
    }

    protected static final class ExternalEndpoint<N extends AbstractConduitNetworkTileEntity<N>> {
        private final N conduit;
        private final Direction conduitSide;
        private final BlockPos neighborPos;
        private final Direction neighborSide;

        private ExternalEndpoint(N conduit, Direction conduitSide,
                                 BlockPos neighborPos, Direction neighborSide) {
            this.conduit = conduit;
            this.conduitSide = conduitSide;
            this.neighborPos = neighborPos;
            this.neighborSide = neighborSide;
        }

        public N getConduit() {
            return conduit;
        }

        public Direction getConduitSide() {
            return conduitSide;
        }

        public BlockPos getNeighborPos() {
            return neighborPos;
        }

        public Direction getNeighborSide() {
            return neighborSide;
        }
    }
}
