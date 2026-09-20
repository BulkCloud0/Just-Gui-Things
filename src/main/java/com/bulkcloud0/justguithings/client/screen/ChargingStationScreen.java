package com.bulkcloud0.justguithings.client.screen;

import com.bulkcloud0.justguithings.world.container.ChargingStationContainer;
import com.bulkcloud0.justguithings.world.tile.ChargingStationTileEntity;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;

public class ChargingStationScreen extends ContainerScreen<ChargingStationContainer> {
    private static final int PANEL_COLOR = 0xFF20262E;
    private static final int INNER_COLOR = 0xFF313942;
    private static final int SLOT_COLOR = 0xFF11161C;
    private static final int SLOT_BORDER_COLOR = 0xFF697582;
    private static final int ENERGY_BG_COLOR = 0xFF101419;
    private static final int ENERGY_COLOR = 0xFF2FC3D8;
    private static final int TEXT_COLOR = 0xFFE5E9ED;

    public ChargingStationScreen(ChargingStationContainer menu, PlayerInventory inventory, ITextComponent title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = 72;
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        this.renderTooltip(matrixStack, mouseX, mouseY);
    }

    @Override
    protected void renderBg(MatrixStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;

        AbstractGui.fill(matrixStack, left, top, left + this.imageWidth, top + this.imageHeight, PANEL_COLOR);
        AbstractGui.fill(matrixStack, left + 5, top + 18, left + 171, top + 76, INNER_COLOR);
        AbstractGui.fill(matrixStack, left + 5, top + 80, left + 171, top + 161, INNER_COLOR);

        drawSlot(matrixStack, left + 52, top + 34);
        drawSlot(matrixStack, left + 106, top + 34);

        int energyHeight = this.menu.getEnergyScaled(48);
        AbstractGui.fill(matrixStack, left + 147, top + 24, left + 158, top + 74, ENERGY_BG_COLOR);
        if (energyHeight > 0) {
            AbstractGui.fill(matrixStack, left + 149, top + 72 - energyHeight, left + 156, top + 72, ENERGY_COLOR);
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlot(matrixStack, left + 7 + column * 18, top + 83 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            drawSlot(matrixStack, left + 7 + column * 18, top + 141);
        }
    }

    private void drawSlot(MatrixStack matrixStack, int x, int y) {
        AbstractGui.fill(matrixStack, x, y, x + 18, y + 18, SLOT_BORDER_COLOR);
        AbstractGui.fill(matrixStack, x + 1, y + 1, x + 17, y + 17, SLOT_COLOR);
    }

    @Override
    protected void renderLabels(MatrixStack matrixStack, int mouseX, int mouseY) {
        this.font.draw(matrixStack, this.title, 8.0F, 6.0F, TEXT_COLOR);
        SideConfigRenderer.drawHorizontal(matrixStack, this.font, 8.0F, 19.0F, this.menu::getSideMode);
        this.font.draw(matrixStack, this.inventory.getDisplayName(), 8.0F, this.inventoryLabelY, TEXT_COLOR);
        this.font.draw(matrixStack,
                "FE: " + this.menu.getEnergyStored() + " / " + ChargingStationTileEntity.CAPACITY,
                70.0F, 55.0F, TEXT_COLOR);
        this.font.draw(matrixStack,
                ChargingStationTileEntity.CHARGE_RATE + " FE/t",
                104.0F, 64.0F, TEXT_COLOR);
    }
}
