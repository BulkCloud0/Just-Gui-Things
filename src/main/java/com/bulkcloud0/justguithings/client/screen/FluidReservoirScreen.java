package com.bulkcloud0.justguithings.client.screen;

import com.bulkcloud0.justguithings.world.container.FluidReservoirContainer;
import com.bulkcloud0.justguithings.world.tile.FluidReservoirTileEntity;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;

public class FluidReservoirScreen extends ContainerScreen<FluidReservoirContainer> {
    private static final int PANEL = 0xFF20262E;
    private static final int INNER = 0xFF313942;
    private static final int BAR_BG = 0xFF101419;
    private static final int FLUID = 0xFF3A8DDE;
    private static final int TEXT = 0xFFE5E9ED;

    public FluidReservoirScreen(FluidReservoirContainer menu, PlayerInventory inventory, ITextComponent title) {
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
        AbstractGui.fill(matrixStack,left,top,left+imageWidth,top+imageHeight,PANEL);
        AbstractGui.fill(matrixStack,left+5,top+18,left+171,top+76,INNER);
        int h = menu.getFluidScaled(46);
        AbstractGui.fill(matrixStack,left+81,top+24,left+95,top+72,BAR_BG);
        if(h>0) AbstractGui.fill(matrixStack,left+83,top+70-h,left+93,top+70,FLUID);
    }

    @Override
    protected void renderLabels(MatrixStack matrixStack, int mouseX, int mouseY) {
        font.draw(matrixStack,title,8,6,TEXT);
        font.draw(matrixStack,inventory.getDisplayName(),8,inventoryLabelY,TEXT);
        font.draw(matrixStack,"mB: "+menu.getFluidAmount()+" / "+FluidReservoirTileEntity.CAPACITY,48,6,TEXT);
    }
}
