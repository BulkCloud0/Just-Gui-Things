package com.bulkcloud0.justguithings.client.screen;

import com.bulkcloud0.justguithings.world.container.ItemBufferContainer;
import com.bulkcloud0.justguithings.world.tile.ItemBufferTileEntity;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public class ItemBufferScreen extends ContainerScreen<ItemBufferContainer> {
    private static final int PANEL_COLOR = 0xFF20262E;
    private static final int INNER_COLOR = 0xFF313942;
    private static final int SLOT_COLOR = 0xFF11161C;
    private static final int SLOT_BORDER_COLOR = 0xFF697582;
    private static final int TEXT_COLOR = 0xFFE5E9ED;

    public ItemBufferScreen(ItemBufferContainer menu, PlayerInventory inventory, ITextComponent title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 176;
        this.inventoryLabelY = 82;
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
        AbstractGui.fill(matrixStack, left + 5, top + 90, left + 171, top + 171, INNER_COLOR);

        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlot(matrixStack, left + 7 + column * 18, top + 34 + row * 18);
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlot(matrixStack, left + 7 + column * 18, top + 93 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            drawSlot(matrixStack, left + 7 + column * 18, top + 151);
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
        this.font.draw(matrixStack,
                new TranslationTextComponent(
                        "screen.justguithings.item_buffer.transfer",
                        ItemBufferTileEntity.AUTO_EJECT_RATE),
                8.0F,
                72.0F,
                TEXT_COLOR);
        this.font.draw(matrixStack, this.inventory.getDisplayName(), 8.0F, this.inventoryLabelY, TEXT_COLOR);
    }
}
