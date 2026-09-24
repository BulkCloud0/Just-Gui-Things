package com.bulkcloud0.justguithings.client.screen;

import com.bulkcloud0.justguithings.world.container.VacuumCollectorContainer;
import com.bulkcloud0.justguithings.world.tile.VacuumCollectorTileEntity;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public class VacuumCollectorScreen extends ContainerScreen<VacuumCollectorContainer> {
    private static final int PANEL_COLOR = 0xFF20262E;
    private static final int INNER_COLOR = 0xFF313942;
    private static final int SLOT_COLOR = 0xFF11161C;
    private static final int SLOT_BORDER_COLOR = 0xFF697582;
    private static final int ENERGY_BG_COLOR = 0xFF101419;
    private static final int ENERGY_COLOR = 0xFF2FC3D8;
    private static final int TEXT_COLOR = 0xFFE5E9ED;

    public VacuumCollectorScreen(VacuumCollectorContainer menu, PlayerInventory inventory, ITextComponent title) {
        super(menu, inventory, title);
        this.imageWidth = 194;
        this.imageHeight = 176;
        this.inventoryLabelY = 84;
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

        AbstractGui.fill(matrixStack, left, top, left + imageWidth, top + imageHeight, PANEL_COLOR);
        AbstractGui.fill(matrixStack, left + 5, top + 18, left + 189, top + 80, INNER_COLOR);
        AbstractGui.fill(matrixStack, left + 5, top + 90, left + 189, top + 171, INNER_COLOR);

        for (int column = 0; column < VacuumCollectorTileEntity.INVENTORY_SIZE; column++) {
            drawSlot(matrixStack, left + 7 + column * 18, top + 34);
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlot(matrixStack, left + 15 + column * 18, top + 94 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            drawSlot(matrixStack, left + 15 + column * 18, top + 152);
        }

        AbstractGui.fill(matrixStack, left + 178, top + 25, left + 190, top + 74, ENERGY_BG_COLOR);
        int energyHeight = menu.getEnergyScaled(45);
        if (energyHeight > 0) {
            AbstractGui.fill(matrixStack, left + 180, top + 72 - energyHeight,
                    left + 188, top + 72, ENERGY_COLOR);
        }
    }

    private void drawSlot(MatrixStack matrixStack, int x, int y) {
        AbstractGui.fill(matrixStack, x, y, x + 18, y + 18, SLOT_BORDER_COLOR);
        AbstractGui.fill(matrixStack, x + 1, y + 1, x + 17, y + 17, SLOT_COLOR);
    }

    @Override
    protected void renderLabels(MatrixStack matrixStack, int mouseX, int mouseY) {
        font.draw(matrixStack, title, 8.0F, 6.0F, TEXT_COLOR);
        SideConfigRenderer.drawHorizontal(matrixStack, font, 8.0F, 19.0F, menu::getSideMode);
        font.draw(matrixStack,
                new TranslationTextComponent("screen.justguithings.vacuum_collector.range",
                        VacuumCollectorTileEntity.COLLECTION_RANGE),
                8.0F, 58.0F, TEXT_COLOR);
        font.draw(matrixStack,
                new TranslationTextComponent("screen.justguithings.vacuum_collector.cost",
                        VacuumCollectorTileEntity.ENERGY_PER_ITEM),
                8.0F, 68.0F, TEXT_COLOR);
        font.draw(matrixStack, inventory.getDisplayName(), 16.0F, inventoryLabelY, TEXT_COLOR);
    }
}
