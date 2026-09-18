package com.bulkcloud0.justguithings.world.tile;

import com.bulkcloud0.justguithings.logistics.ConduitTransferMode;
import com.bulkcloud0.justguithings.registry.ModTileEntities;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BasicFluidPipeTileEntity extends AbstractConduitNetworkTileEntity<BasicFluidPipeTileEntity> {
    public static final int TRANSFER_RATE = 250;
    private static final int RECOVERY_CAPACITY = 1_000;
    private static final int NETWORK_CACHE_TTL = 100;
    private static final int VISUAL_REFRESH_INTERVAL = 10;

    private final EnumMap<Direction, ConduitTransferMode> sideModes = new EnumMap<>(Direction.class);
    private final FluidTank recoveryTank = new FluidTank(RECOVERY_CAPACITY) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private int sourceCursor;
    private int targetCursor;

    public BasicFluidPipeTileEntity() {
        super(ModTileEntities.BASIC_FLUID_PIPE.get(), NETWORK_CACHE_TTL);
        for (Direction direction : Direction.values()) {
            sideModes.put(direction, ConduitTransferMode.BOTH);
        }
    }

    @Override
    protected Class<BasicFluidPipeTileEntity> getNetworkNodeClass() {
        return BasicFluidPipeTileEntity.class;
    }

    @Override
    protected void onNetworkCacheCleared() {
        sourceCursor = 0;
        targetCursor = 0;
    }

    @Override
    public void tick() {
        if (level == null || level.isClientSide) {
            return;
        }

        tickNetworkMaintenance(VISUAL_REFRESH_INTERVAL);
        if (!isNetworkController()) {
            return;
        }

        transferFluids(getCachedNetwork());
    }

    public ConduitTransferMode getSideMode(Direction direction) {
        return sideModes.getOrDefault(direction, ConduitTransferMode.BOTH);
    }

    public ConduitTransferMode cycleSideMode(Direction direction) {
        ConduitTransferMode next = getSideMode(direction).next();
        sideModes.put(direction, next);
        setChanged();
        return next;
    }

    private void transferFluids(List<BasicFluidPipeTileEntity> network) {
        Set<BlockPos> pipePositions = new HashSet<>();
        for (BasicFluidPipeTileEntity pipe : network) {
            if (pipe.isRemoved()) {
                invalidateNetworkCache();
                return;
            }
            pipePositions.add(pipe.getBlockPos());
        }

        List<FluidEndpoint> sources = new ArrayList<>();
        List<FluidEndpoint> targets = new ArrayList<>();
        collectEndpoints(network, pipePositions, sources, targets);

        if (targets.isEmpty()) {
            return;
        }

        int budget = flushRecovery(network, targets, TRANSFER_RATE);
        if (budget <= 0 || hasRecovery(network) || sources.isEmpty()) {
            return;
        }

        int sourceStart = Math.floorMod(sourceCursor, sources.size());
        for (int sourceOffset = 0; sourceOffset < sources.size() && budget > 0; sourceOffset++) {
            FluidEndpoint source = sources.get((sourceStart + sourceOffset) % sources.size());
            FluidStack simulatedDrain = source.handler.drain(budget, IFluidHandler.FluidAction.SIMULATE);
            if (simulatedDrain.isEmpty()) {
                continue;
            }

            int targetStart = Math.floorMod(targetCursor, targets.size());
            for (int targetOffset = 0; targetOffset < targets.size() && budget > 0; targetOffset++) {
                int targetIndex = (targetStart + targetOffset) % targets.size();
                FluidEndpoint target = targets.get(targetIndex);
                if (target.inventoryPos.equals(source.inventoryPos)) {
                    continue;
                }

                int moved = moveFluid(network, source, target, simulatedDrain, budget);
                if (moved > 0) {
                    budget -= moved;
                    targetCursor = (targetIndex + 1) % targets.size();
                    break;
                }
            }

            if (hasRecovery(network)) {
                break;
            }
        }

        sourceCursor = (sourceStart + 1) % sources.size();
    }

    private void collectEndpoints(List<BasicFluidPipeTileEntity> network,
                                  Set<BlockPos> pipePositions,
                                  List<FluidEndpoint> sources,
                                  List<FluidEndpoint> targets) {
        Set<EndpointKey> sourceKeys = new HashSet<>();
        Set<EndpointKey> targetKeys = new HashSet<>();

        for (BasicFluidPipeTileEntity pipe : network) {
            for (Direction direction : Direction.values()) {
                ConduitTransferMode mode = pipe.getSideMode(direction);
                if (mode == ConduitTransferMode.DISABLED) {
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

                IFluidHandler handler = neighbor
                        .getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, direction.getOpposite())
                        .orElse(null);
                if (handler == null) {
                    continue;
                }

                EndpointKey key = new EndpointKey(neighborPos, direction.getOpposite());
                if (mode.canPull() && sourceKeys.add(key)) {
                    sources.add(new FluidEndpoint(neighborPos, handler));
                }
                if (mode.canPush() && targetKeys.add(key)) {
                    targets.add(new FluidEndpoint(neighborPos, handler));
                }
            }
        }
    }

    private int moveFluid(List<BasicFluidPipeTileEntity> network,
                          FluidEndpoint source,
                          FluidEndpoint target,
                          FluidStack simulatedDrain,
                          int maxAmount) {
        FluidStack offer = simulatedDrain.copy();
        offer.setAmount(Math.min(maxAmount, simulatedDrain.getAmount()));

        int accepted = target.handler.fill(offer, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) {
            return 0;
        }

        FluidStack request = offer.copy();
        request.setAmount(Math.min(accepted, offer.getAmount()));
        FluidStack drained = source.handler.drain(request, IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty()) {
            return 0;
        }

        int inserted = target.handler.fill(drained, IFluidHandler.FluidAction.EXECUTE);
        inserted = Math.max(0, Math.min(inserted, drained.getAmount()));

        int remainderAmount = drained.getAmount() - inserted;
        if (remainderAmount > 0) {
            FluidStack remainder = drained.copy();
            remainder.setAmount(remainderAmount);

            int returned = source.handler.fill(remainder, IFluidHandler.FluidAction.EXECUTE);
            returned = Math.max(0, Math.min(returned, remainder.getAmount()));
            remainder.setAmount(remainder.getAmount() - returned);

            if (!remainder.isEmpty()) {
                storeRecovery(network, remainder);
            }
        }

        return inserted;
    }

    private int flushRecovery(List<BasicFluidPipeTileEntity> network,
                              List<FluidEndpoint> targets,
                              int budget) {
        if (budget <= 0 || targets.isEmpty()) {
            return budget;
        }

        for (BasicFluidPipeTileEntity pipe : network) {
            while (budget > 0 && !pipe.recoveryTank.isEmpty()) {
                FluidStack buffered = pipe.recoveryTank.getFluid().copy();
                buffered.setAmount(Math.min(buffered.getAmount(), budget));

                boolean moved = false;
                int targetStart = Math.floorMod(targetCursor, targets.size());
                for (int offset = 0; offset < targets.size(); offset++) {
                    int targetIndex = (targetStart + offset) % targets.size();
                    FluidEndpoint target = targets.get(targetIndex);

                    int accepted = target.handler.fill(buffered, IFluidHandler.FluidAction.SIMULATE);
                    if (accepted <= 0) {
                        continue;
                    }

                    FluidStack transfer = buffered.copy();
                    transfer.setAmount(Math.min(accepted, buffered.getAmount()));
                    int inserted = target.handler.fill(transfer, IFluidHandler.FluidAction.EXECUTE);
                    inserted = Math.max(0, Math.min(inserted, transfer.getAmount()));
                    if (inserted <= 0) {
                        continue;
                    }

                    pipe.recoveryTank.drain(inserted, IFluidHandler.FluidAction.EXECUTE);
                    budget -= inserted;
                    targetCursor = (targetIndex + 1) % targets.size();
                    moved = true;
                    break;
                }

                if (!moved) {
                    return budget;
                }
            }
        }

        return budget;
    }

    private boolean hasRecovery(List<BasicFluidPipeTileEntity> network) {
        for (BasicFluidPipeTileEntity pipe : network) {
            if (!pipe.recoveryTank.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void storeRecovery(List<BasicFluidPipeTileEntity> network, FluidStack stack) {
        FluidStack remaining = stack.copy();
        for (BasicFluidPipeTileEntity pipe : network) {
            if (remaining.isEmpty()) {
                break;
            }

            int accepted = pipe.recoveryTank.fill(remaining, IFluidHandler.FluidAction.EXECUTE);
            if (accepted > 0) {
                remaining.setAmount(remaining.getAmount() - accepted);
            }
        }

        if (!remaining.isEmpty()) {
            throw new IllegalStateException("Fluid recovery capacity exhausted for conduit network");
        }
    }

    @Override
    public void load(BlockState state, CompoundNBT nbt) {
        super.load(state, nbt);

        if (nbt.contains("SideConfig")) {
            CompoundNBT config = nbt.getCompound("SideConfig");
            for (Direction direction : Direction.values()) {
                String key = "Side" + direction.ordinal();
                if (config.contains(key)) {
                    sideModes.put(direction, ConduitTransferMode.fromOrdinal(config.getInt(key)));
                }
            }
        }

        if (nbt.contains("RecoveryTank")) {
            recoveryTank.readFromNBT(nbt.getCompound("RecoveryTank"));
        }

        invalidateNetworkCache();
    }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        super.save(nbt);

        CompoundNBT config = new CompoundNBT();
        for (Direction direction : Direction.values()) {
            config.putInt("Side" + direction.ordinal(), getSideMode(direction).ordinal());
        }
        nbt.put("SideConfig", config);
        nbt.put("RecoveryTank", recoveryTank.writeToNBT(new CompoundNBT()));
        return nbt;
    }

    private static final class FluidEndpoint {
        private final BlockPos inventoryPos;
        private final IFluidHandler handler;

        private FluidEndpoint(BlockPos inventoryPos, IFluidHandler handler) {
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
