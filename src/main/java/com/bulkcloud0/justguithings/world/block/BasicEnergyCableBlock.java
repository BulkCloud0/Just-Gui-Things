package com.bulkcloud0.justguithings.world.block;

import com.bulkcloud0.justguithings.world.tile.BasicEnergyCableTileEntity;
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
import net.minecraftforge.energy.CapabilityEnergy;

import javax.annotation.Nullable;

public class BasicEnergyCableBlock extends Block {
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

    public BasicEnergyCableBlock(Properties properties) {
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
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        return updateConnections(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  IWorld world, BlockPos currentPos, BlockPos neighborPos) {
        return state.setValue(propertyFor(direction), canConnect(world, currentPos, direction));
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext context) {
        VoxelShape shape = CORE;
        if (state.getValue(NORTH)) shape = VoxelShapes.or(shape, NORTH_ARM);
        if (state.getValue(SOUTH)) shape = VoxelShapes.or(shape, SOUTH_ARM);
        if (state.getValue(EAST)) shape = VoxelShapes.or(shape, EAST_ARM);
        if (state.getValue(WEST)) shape = VoxelShapes.or(shape, WEST_ARM);
        if (state.getValue(UP)) shape = VoxelShapes.or(shape, UP_ARM);
        if (state.getValue(DOWN)) shape = VoxelShapes.or(shape, DOWN_ARM);
        return shape;
    }

    public static void refreshConnections(World world, BlockPos pos) {
        BlockState current = world.getBlockState(pos);
        if (!(current.getBlock() instanceof BasicEnergyCableBlock)) {
            return;
        }

        BasicEnergyCableBlock cable = (BasicEnergyCableBlock) current.getBlock();
        BlockState updated = cable.updateConnections(current, world, pos);
        if (!updated.equals(current)) {
            world.setBlock(pos, updated, 3);
        }
    }

    private BlockState updateConnections(BlockState state, IBlockReader world, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            state = state.setValue(propertyFor(direction), canConnect(world, pos, direction));
        }
        return state;
    }

    private boolean canConnect(IBlockReader world, BlockPos pos, Direction direction) {
        BlockPos neighborPos = pos.relative(direction);
        BlockState neighborState = world.getBlockState(neighborPos);
        if (neighborState.getBlock() instanceof BasicEnergyCableBlock) {
            return true;
        }

        TileEntity neighbor = world.getBlockEntity(neighborPos);
        return neighbor != null
                && neighbor.getCapability(CapabilityEnergy.ENERGY, direction.getOpposite()).isPresent();
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

    private void invalidateAdjacentCableCaches(World world, BlockPos pos) {
        TileEntity self = world.getBlockEntity(pos);
        if (self instanceof BasicEnergyCableTileEntity) {
            ((BasicEnergyCableTileEntity) self).invalidateNetworkCache();
        }
        for (Direction direction : Direction.values()) {
            TileEntity neighbor = world.getBlockEntity(pos.relative(direction));
            if (neighbor instanceof BasicEnergyCableTileEntity) {
                ((BasicEnergyCableTileEntity) neighbor).invalidateNetworkCache();
            }
        }
    }

    @Override
    public void onPlace(BlockState state, World world, BlockPos pos, BlockState oldState, boolean moving) {
        super.onPlace(state, world, pos, oldState, moving);
        if (!world.isClientSide && oldState.getBlock() != state.getBlock()) {
            invalidateAdjacentCableCaches(world, pos);
        }
    }

    @Override
    public void onRemove(BlockState state, World world, BlockPos pos, BlockState newState, boolean moving) {
        if (!world.isClientSide && state.getBlock() != newState.getBlock()) {
            invalidateAdjacentCableCaches(world, pos);
        }
        super.onRemove(state, world, pos, newState, moving);
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new BasicEnergyCableTileEntity();
    }
}
