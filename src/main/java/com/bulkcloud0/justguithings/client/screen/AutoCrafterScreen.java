package com.bulkcloud0.justguithings.client.screen;

import com.bulkcloud0.justguithings.world.container.AutoCrafterContainer;
import com.bulkcloud0.justguithings.world.tile.AutoCrafterTileEntity;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public class AutoCrafterScreen extends ContainerScreen<AutoCrafterContainer> {
    private static final int PANEL_COLOR = 0xFF20262E;
    private static final int INNER_COLOR = 0xFF313942;
    private static final int SLOT_COLOR = 0xFF11161C;
    private static final int SLOT_BORDER_COLOR = 0xFF697582;
    private static final int ENERGY_BG_COLOR = 0xFF101419;
    private static final int ENERGY_COLOR = 0xFF2FC3D8;
    private static final int PROGRESS_BG_COLOR = 0xFF101419;
    private static final int PROGRESS_COLOR = 0xFF7DB46C;
    private static final int TEXT_COLOR = 0xFFE5E9ED;

    public AutoCrafterScreen(AutoCrafterContainer menu, PlayerInventory inventory, ITextComponent title) {
        super(menu, inventory, title);
        this.imageWidth = 212;
        this.imageHeight = 196;
        this.inventoryLabelY = 100;
    }

    @Override
    protected void init() {
        super.init();
        addButton(new Button(this.leftPos + 67, this.topPos + 24, 64, 20,
                new TranslationTextComponent("screen.justguithings.auto_crafter.toggle_lock"),
                button -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(
                                this.menu.containerId, AutoCrafterContainer.BUTTON_TOGGLE_LOCK);
                    }
                }));
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
        AbstractGui.fill(matrixStack, left + 5, top + 18, left + 207, top + 98, INNER_COLOR);
        AbstractGui.fill(matrixStack, left + 5, top + 104, left + 207, top + 191, INNER_COLOR);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                drawSlot(matrixStack, left + 7 + column * 18, top + 31 + row * 18);
                drawSlot(matrixStack, left + 139 + column * 18, top + 31 + row * 18);
            }
        }
        drawSlot(matrixStack, left + 105, top + 49);
        drawSlot(matrixStack, left + 74, top + 71);
        drawSlot(matrixStack, left + 92, top + 71);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlot(matrixStack, left + 24 + column * 18, top + 110 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            drawSlot(matrixStack, left + 24 + column * 18, top + 168);
        }

        AbstractGui.fill(matrixStack, left + 71, top + 49, left + 100, top + 61, PROGRESS_BG_COLOR);
        int progressWidth = menu.getProgressScaled(27);
        if (progressWidth > 0) {
            AbstractGui.fill(matrixStack, left + 72, top + 50,
                    left + 72 + progressWidth, top + 60, PROGRESS_COLOR);
        }

        int energyHeight = menu.getEnergyScaled(66);
        AbstractGui.fill(matrixStack, left + 197, top + 25, left + 209, top + 93, ENERGY_BG_COLOR);
        if (energyHeight > 0) {
            AbstractGui.fill(matrixStack, left + 199, top + 91 - energyHeight,
                    left + 207, top + 91, ENERGY_COLOR);
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
                new TranslationTextComponent(menu.isRecipeLocked()
                        ? "screen.justguithings.auto_crafter.locked"
                        : "screen.justguithings.auto_crafter.unlocked"),
                68.0F, 17.0F, TEXT_COLOR);
        font.draw(matrixStack,
                new TranslationTextComponent("screen.justguithings.auto_crafter.returns"),
                139.0F, 19.0F, TEXT_COLOR);
        font.draw(matrixStack, inventory.getDisplayName(), 25.0F, inventoryLabelY, TEXT_COLOR);
        font.draw(matrixStack, menu.getCurrentEnergyPerTick() + " FE/t", 68.0F, 64.0F, TEXT_COLOR);
        font.draw(matrixStack,
                "S" + menu.getSpeedUpgradeCount() + " E" + menu.getEfficiencyUpgradeCount(),
                112.0F, 83.0F, TEXT_COLOR);
        font.draw(matrixStack,
                "FE " + menu.getEnergyStored() + "/" + AutoCrafterTileEntity.CAPACITY,
                112.0F, 6.0F, TEXT_COLOR);
    }
}
