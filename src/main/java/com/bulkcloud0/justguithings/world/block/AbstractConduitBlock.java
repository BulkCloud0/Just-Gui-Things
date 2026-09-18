package com.bulkcloud0.justguithings.world.block;

import com.bulkcloud0.justguithings.world.tile.AbstractConduitNetworkTileEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public abstract class AbstractConduitBlock extends Block {
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");

    private static final VoxelShape CORE = Block.box(6.0D, 6.0D, 6.0D, 10.0D, 10.0D, 10.0D);
    private static final VoxelShape NORTH_ARM = Block.box(6.0D, 6.0D, 0.0D, 10.0D, 10.0D, 6.0D);
    private static final VoxelShape SOUTH_ARM = Block.box(6.0D, 6.0D, 10.0D, 10.0D, 10.0D, 16.0D);
    private static final VoxelShape WEST_ARM = Block.box(0.0D, 6.0D, 6.0D, 6.0D, 10.0D, 10.0D);
    private static final VoxelShape EAST_ARM = Block.box(10.0D, 6.0D, 6.0D, 16.0D, 10.0D, 10.0D);
    private static final VoxelShape DOWN_ARM = Block.box(6.0D, 0.0D, 6.0D, 10.0D, 6.0D, 10.0D);
    private static final VoxelShape UP_ARM = Block.box(6.0D, 10.0D, 6.0D, 10.0D, 16.0D, 10.0D);

    protected AbstractConduitBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(NORTH, false)
                .setValue(SOUTH, false)
                .setValue(EAST, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false));
    }

    @Override
    protected final void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    @Nullable
    @Override
    public final BlockState getStateForPlacement(BlockItemUseContext context) {
        return updateConnections(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    public final BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                        IWorld world, BlockPos currentPos, BlockPos neighborPos) {
        BooleanProperty property = propertyFor(direction);
        boolean connected = canConnectTo(world, currentPos, direction);
        if (state.getValue(property) != connected) {
            invalidateExternalEndpointCache(world, currentPos);
        }
        return state.setValue(property, connected);
    }

    @Override
    public final VoxelShape getShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext context) {
        VoxelShape shape = CORE;
        if (state.getValue(NORTH)) shape = VoxelShapes.or(shape, NORTH_ARM);
        if (state.getValue(SOUTH)) shape = VoxelShapes.or(shape, SOUTH_ARM);
        if (state.getValue(EAST)) shape = VoxelShapes.or(shape, EAST_ARM);
        if (state.getValue(WEST)) shape = VoxelShapes.or(shape, WEST_ARM);
        if (state.getValue(UP)) shape = VoxelShapes.or(shape, UP_ARM);
        if (state.getValue(DOWN)) shape = VoxelShapes.or(shape, DOWN_ARM);
        return shape;
    }

    protected abstract boolean canConnectTo(IBlockReader world, BlockPos pos, Direction direction);

    protected static boolean isPositionLoaded(IBlockReader world, BlockPos pos) {
        return !(world instanceof World) || ((World) world).hasChunkAt(pos);
    }

    @Nullable
    protected static TileEntity getLoadedBlockEntity(IBlockReader world, BlockPos pos) {
        if (!isPositionLoaded(world, pos)) {
            return null;
        }
        return world.getBlockEntity(pos);
    }

    public static void refreshConnections(World world, BlockPos pos) {
        BlockState current = world.getBlockState(pos);
        if (!(current.getBlock() instanceof AbstractConduitBlock)) {
            return;
        }

        AbstractConduitBlock conduit = (AbstractConduitBlock) current.getBlock();
        BlockState updated = conduit.updateConnections(current, world, pos);
        if (!updated.equals(current)) {
            invalidateExternalEndpointCache(world, pos);
            world.setBlock(pos, updated, 3);
        }
    }

    private static void invalidateExternalEndpointCache(IWorld world, BlockPos pos) {
        if (!(world instanceof World) || ((World) world).isClientSide) {
            return;
        }

        TileEntity tile = ((World) world).getBlockEntity(pos);
        if (tile instanceof AbstractConduitNetworkTileEntity) {
            ((AbstractConduitNetworkTileEntity<?>) tile).invalidateExternalEndpointCache();
        }
    }

    private BlockState updateConnections(BlockState state, IBlockReader world, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            state = state.setValue(propertyFor(direction), canConnectTo(world, pos, direction));
        }
        return state;
    }

    private static BooleanProperty propertyFor(Direction direction) {
        switch (direction) {
            case NORTH: return NORTH;
            case SOUTH: return SOUTH;
            case EAST: return EAST;
            case WEST: return WEST;
            case UP: return UP;
            case DOWN: return DOWN;
            default: throw new IllegalArgumentException("Unsupported direction: " + direction);
        }
    }
}
