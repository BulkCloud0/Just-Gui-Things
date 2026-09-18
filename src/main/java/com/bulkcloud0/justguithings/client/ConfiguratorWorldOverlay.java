package com.bulkcloud0.justguithings.client;

import com.bulkcloud0.justguithings.JustGuiThings;
import com.bulkcloud0.justguithings.machine.BaseMachineTileEntity;
import com.bulkcloud0.justguithings.registry.ModItems;
import com.bulkcloud0.justguithings.world.tile.BasicEnergyCableTileEntity;
import com.bulkcloud0.justguithings.world.tile.BasicFluidPipeTileEntity;
import com.bulkcloud0.justguithings.world.tile.BasicItemPipeTileEntity;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber(modid = JustGuiThings.MOD_ID, value = Dist.CLIENT)
public final class ConfiguratorWorldOverlay {
    private static final double FACE_INSET = 0.08D;
    private static final double FACE_OFFSET = 0.002D;
    private static final double FACE_DEPTH = 0.003D;

    private ConfiguratorWorldOverlay() {
    }

    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientPlayerEntity player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.screen != null || !isConfiguratorHeld(player)) {
            return;
        }

        RayTraceResult hit = minecraft.hitResult;
        if (!(hit instanceof BlockRayTraceResult) || hit.getType() != RayTraceResult.Type.BLOCK) {
            return;
        }

        BlockRayTraceResult blockHit = (BlockRayTraceResult) hit;
        TileEntity tile = minecraft.level.getBlockEntity(blockHit.getBlockPos());
        Integer color = getModeColor(tile, blockHit.getDirection());
        if (color == null) {
            return;
        }

        renderFaceOutline(event, minecraft, blockHit.getBlockPos(), blockHit.getDirection(), color);
    }

    private static boolean isConfiguratorHeld(ClientPlayerEntity player) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        return mainHand.getItem() == ModItems.CONFIGURATOR.get()
                || offHand.getItem() == ModItems.CONFIGURATOR.get();
    }

    @Nullable
    private static Integer getModeColor(@Nullable TileEntity tile, Direction direction) {
        if (tile instanceof BasicItemPipeTileEntity) {
            return SideModeColors.getConduitColor(((BasicItemPipeTileEntity) tile).getSideMode(direction));
        }
        if (tile instanceof BasicFluidPipeTileEntity) {
            return SideModeColors.getConduitColor(((BasicFluidPipeTileEntity) tile).getSideMode(direction));
        }
        if (tile instanceof BasicEnergyCableTileEntity) {
            return SideModeColors.getConduitColor(((BasicEnergyCableTileEntity) tile).getSideMode(direction));
        }
        if (tile instanceof BaseMachineTileEntity) {
            return SideModeColors.getMachineColor(((BaseMachineTileEntity) tile).getSideMode(direction));
        }
        return null;
    }

    private static void renderFaceOutline(RenderWorldLastEvent event, Minecraft minecraft,
                                          BlockPos pos, Direction direction, int color) {
        double minX = pos.getX() + FACE_INSET;
        double minY = pos.getY() + FACE_INSET;
        double minZ = pos.getZ() + FACE_INSET;
        double maxX = pos.getX() + 1.0D - FACE_INSET;
        double maxY = pos.getY() + 1.0D - FACE_INSET;
        double maxZ = pos.getZ() + 1.0D - FACE_INSET;

        switch (direction) {
            case NORTH:
                minZ = pos.getZ() - FACE_OFFSET - FACE_DEPTH;
                maxZ = pos.getZ() - FACE_OFFSET;
                break;
            case SOUTH:
                minZ = pos.getZ() + 1.0D + FACE_OFFSET;
                maxZ = minZ + FACE_DEPTH;
                break;
            case WEST:
                minX = pos.getX() - FACE_OFFSET - FACE_DEPTH;
                maxX = pos.getX() - FACE_OFFSET;
                break;
            case EAST:
                minX = pos.getX() + 1.0D + FACE_OFFSET;
                maxX = minX + FACE_DEPTH;
                break;
            case DOWN:
                minY = pos.getY() - FACE_OFFSET - FACE_DEPTH;
                maxY = pos.getY() - FACE_OFFSET;
                break;
            case UP:
                minY = pos.getY() + 1.0D + FACE_OFFSET;
                maxY = minY + FACE_DEPTH;
                break;
            default:
                return;
        }

        MatrixStack matrixStack = event.getMatrixStack();
        Vector3d camera = minecraft.gameRenderer.getMainCamera().getPosition();
        IRenderTypeBuffer.Impl buffer = minecraft.renderBuffers().bufferSource();

        matrixStack.pushPose();
        matrixStack.translate(-camera.x, -camera.y, -camera.z);

        IVertexBuilder builder = buffer.getBuffer(RenderType.lines());
        WorldRenderer.renderLineBox(
                matrixStack, builder,
                minX, minY, minZ, maxX, maxY, maxZ,
                SideModeColors.red(color), SideModeColors.green(color), SideModeColors.blue(color), 1.0F,
                SideModeColors.red(color), SideModeColors.green(color), SideModeColors.blue(color));

        matrixStack.popPose();
        buffer.endBatch(RenderType.lines());
    }
}
