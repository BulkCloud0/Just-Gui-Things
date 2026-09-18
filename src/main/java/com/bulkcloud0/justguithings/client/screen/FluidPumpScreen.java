package com.bulkcloud0.justguithings.client.screen;

import com.bulkcloud0.justguithings.world.container.FluidPumpContainer;
import com.bulkcloud0.justguithings.world.tile.FluidPumpTileEntity;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;

public class FluidPumpScreen extends ContainerScreen<FluidPumpContainer> {
    private static final int PANEL = 0xFF20262E;
    private static final int INNER = 0xFF313942;
    private static final int BAR_BG = 0xFF101419;
    private static final int ENERGY = 0xFF2FC3D8;
    private static final int FLUID = 0xFF397FE6;
    private static final int PROGRESS = 0xFF79BFD1;
    private static final int TEXT = 0xFFE5E9ED;
    private static final int OK = 0xFF63C174;
    private static final int BAD = 0xFFE05A5A;

    public FluidPumpScreen(FluidPumpContainer menu, PlayerInventory inventory, ITextComponent title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = 72;
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        renderTooltip(matrixStack, mouseX, mouseY);
    }

    @Override
    protected void renderBg(MatrixStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        int left = leftPos;
        int top = topPos;

        AbstractGui.fill(matrixStack, left, top, left + imageWidth, top + imageHeight, PANEL);
        AbstractGui.fill(matrixStack, left + 5, top + 18, left + 171, top + 76, INNER);

        int fluidHeight = menu.getFluidScaled(48);
        AbstractGui.fill(matrixStack, left + 18, top + 24, left + 29, top + 74, BAR_BG);
        if (fluidHeight > 0) {
            AbstractGui.fill(matrixStack, left + 20, top + 72 - fluidHeight, left + 27, top + 72, FLUID);
        }

        int energyHeight = menu.getEnergyScaled(48);
        AbstractGui.fill(matrixStack, left + 147, top + 24, left + 158, top + 74, BAR_BG);
        if (energyHeight > 0) {
            AbstractGui.fill(matrixStack, left + 149, top + 72 - energyHeight, left + 156, top + 72, ENERGY);
        }

        AbstractGui.fill(matrixStack, left + 58, top + 43, left + 118, top + 53, BAR_BG);
        int progress = menu.getProgressScaled(58);
        if (progress > 0) {
            AbstractGui.fill(matrixStack, left + 59, top + 44, left + 59 + progress, top + 52, PROGRESS);
        }
    }

    @Override
    protected void renderLabels(MatrixStack matrixStack, int mouseX, int mouseY) {
        font.draw(matrixStack, title, 8.0F, 6.0F, TEXT);
        SideConfigRenderer.drawHorizontal(matrixStack, font, 8.0F, 19.0F, menu::getSideMode);
        font.draw(matrixStack, inventory.getDisplayName(), 8.0F, inventoryLabelY, TEXT);

        font.draw(matrixStack, menu.getFluidAmount() + " mB", 8.0F, 55.0F, TEXT);
        font.draw(matrixStack,
                "FE: " + menu.getEnergyStored() + " / " + FluidPumpTileEntity.ENERGY_CAPACITY,
                82.0F, 6.0F, TEXT);
        font.draw(matrixStack,
                menu.hasWaterSource() ? "Water source: OK" : "Water source: missing",
                52.0F, 58.0F, menu.hasWaterSource() ? OK : BAD);
    }
}
