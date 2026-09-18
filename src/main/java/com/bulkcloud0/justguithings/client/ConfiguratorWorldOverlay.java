package com.bulkcloud0.justguithings.client;

import com.bulkcloud0.justguithings.JustGuiThings;
import com.bulkcloud0.justguithings.logistics.ConduitTransferMode;
import com.bulkcloud0.justguithings.machine.BaseMachineTileEntity;
import com.bulkcloud0.justguithings.machine.MachineSideMode;
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
    private static final ModeColor DISABLED = ModeColor.fromRgb(0x7D8791);
    private static final ModeColor INPUT = ModeColor.fromRgb(0x63C174);
    private static final ModeColor OUTPUT = ModeColor.fromRgb(0xE29A4A);
    private static final ModeColor ENERGY = ModeColor.fromRgb(0x45C7D9);
    private static final ModeColor FLUID_INPUT = ModeColor.fromRgb(0x5B8DEF);
    private static final ModeColor FLUID_OUTPUT = ModeColor.fromRgb(0x4EC6E6);

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
        ModeColor color = getModeColor(tile, blockHit.getDirection());
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
    private static ModeColor getModeColor(@Nullable TileEntity tile, Direction direction) {
        if (tile instanceof BasicItemPipeTileEntity) {
            return getConduitColor(((BasicItemPipeTileEntity) tile).getSideMode(direction));
        }
        if (tile instanceof BasicFluidPipeTileEntity) {
            return getConduitColor(((BasicFluidPipeTileEntity) tile).getSideMode(direction));
        }
        if (tile instanceof BasicEnergyCableTileEntity) {
            return getConduitColor(((BasicEnergyCableTileEntity) tile).getSideMode(direction));
        }
        if (tile instanceof BaseMachineTileEntity) {
            return getMachineColor(((BaseMachineTileEntity) tile).getSideMode(direction));
        }
        return null;
    }

    private static ModeColor getConduitColor(ConduitTransferMode mode) {
        switch (mode) {
            case PULL:
                return INPUT;
            case PUSH:
                return OUTPUT;
            case BOTH:
                return ENERGY;
            case DISABLED:
            default:
                return DISABLED;
        }
    }

    private static ModeColor getMachineColor(MachineSideMode mode) {
        switch (mode) {
            case INPUT:
                return INPUT;
            case OUTPUT:
                return OUTPUT;
            case FLUID_INPUT:
                return FLUID_INPUT;
            case FLUID_OUTPUT:
                return FLUID_OUTPUT;
            case ENERGY:
            case ENERGY_OUTPUT:
            case ENERGY_BOTH:
                return ENERGY;
            case DISABLED:
            default:
                return DISABLED;
        }
    }

    private static void renderFaceOutline(RenderWorldLastEvent event, Minecraft minecraft,
                                          BlockPos pos, Direction direction, ModeColor color) {
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
                color.red, color.green, color.blue, 1.0F,
                color.red, color.green, color.blue);

        matrixStack.popPose();
        buffer.endBatch(RenderType.lines());
    }

    private static final class ModeColor {
        private final float red;
        private final float green;
        private final float blue;

        private ModeColor(float red, float green, float blue) {
            this.red = red;
            this.green = green;
            this.blue = blue;
        }

        private static ModeColor fromRgb(int rgb) {
            return new ModeColor(
                    ((rgb >> 16) & 0xFF) / 255.0F,
                    ((rgb >> 8) & 0xFF) / 255.0F,
                    (rgb & 0xFF) / 255.0F);
        }
    }
}
