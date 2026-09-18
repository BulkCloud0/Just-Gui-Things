package com.bulkcloud0.justguithings.client.screen;

import com.bulkcloud0.justguithings.world.container.ResistiveFurnaceContainer;
import com.bulkcloud0.justguithings.world.tile.ResistiveFurnaceTileEntity;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;

public class ResistiveFurnaceScreen extends ContainerScreen<ResistiveFurnaceContainer> {
    private static final int PANEL = 0xFF20262E;
    private static final int INNER = 0xFF313942;
    private static final int SLOT = 0xFF11161C;
    private static final int SLOT_BORDER = 0xFF697582;
    private static final int ENERGY_BG = 0xFF101419;
    private static final int ENERGY = 0xFF2FC3D8;
    private static final int PROGRESS_BG = 0xFF101419;
    private static final int PROGRESS = 0xFFE06B3C;
    private static final int TEXT = 0xFFE5E9ED;

    public ResistiveFurnaceScreen(ResistiveFurnaceContainer menu, PlayerInventory inventory, ITextComponent title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = 72;
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
        AbstractGui.fill(matrixStack, left + 5, top + 80, left + 171, top + 161, INNER);

        drawSlot(matrixStack, left + 43, top + 34);
        drawSlot(matrixStack, left + 115, top + 34);
        drawSlot(matrixStack, left + 79, top + 55);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlot(matrixStack, left + 7 + column * 18, top + 83 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            drawSlot(matrixStack, left + 7 + column * 18, top + 141);
        }

        AbstractGui.fill(matrixStack, left + 69, top + 38, left + 105, top + 50, PROGRESS_BG);
        int progressWidth = menu.getProgressScaled(34);
        if (progressWidth > 0) {
            AbstractGui.fill(matrixStack, left + 70, top + 39, left + 70 + progressWidth, top + 49, PROGRESS);
        }

        int energyHeight = menu.getEnergyScaled(48);
        AbstractGui.fill(matrixStack, left + 147, top + 24, left + 158, top + 74, ENERGY_BG);
        if (energyHeight > 0) {
            AbstractGui.fill(matrixStack, left + 149, top + 72 - energyHeight, left + 156, top + 72, ENERGY);
        }
    }

    private void drawSlot(MatrixStack matrixStack, int x, int y) {
        AbstractGui.fill(matrixStack, x, y, x + 18, y + 18, SLOT_BORDER);
        AbstractGui.fill(matrixStack, x + 1, y + 1, x + 17, y + 17, SLOT);
    }

    @Override
    protected void renderLabels(MatrixStack matrixStack, int mouseX, int mouseY) {
        font.draw(matrixStack, title, 8.0F, 6.0F, TEXT);
        SideConfigRenderer.drawHorizontal(matrixStack, font, 8.0F, 19.0F, menu::getSideMode);
        font.draw(matrixStack, inventory.getDisplayName(), 8.0F, inventoryLabelY, TEXT);
        font.draw(matrixStack,
                "FE: " + menu.getEnergyStored() + " / " + ResistiveFurnaceTileEntity.CAPACITY,
                82.0F, 6.0F, TEXT);
        font.draw(matrixStack,
                menu.getCurrentEnergyPerTick() + " FE/t",
                8.0F, 55.0F, TEXT);
        font.draw(matrixStack, "IC", 101.0F, 59.0F, TEXT);
    }
}
