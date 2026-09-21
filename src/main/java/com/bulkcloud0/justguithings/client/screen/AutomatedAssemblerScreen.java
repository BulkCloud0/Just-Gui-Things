package com.bulkcloud0.justguithings.client.screen;

import com.bulkcloud0.justguithings.world.container.AutomatedAssemblerContainer;
import com.bulkcloud0.justguithings.world.tile.AutomatedAssemblerTileEntity;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public class AutomatedAssemblerScreen extends ContainerScreen<AutomatedAssemblerContainer> {
    private static final int PANEL_COLOR = 0xFF20262E;
    private static final int INNER_COLOR = 0xFF313942;
    private static final int SLOT_COLOR = 0xFF11161C;
    private static final int SLOT_BORDER_COLOR = 0xFF697582;
    private static final int ENERGY_BG_COLOR = 0xFF101419;
    private static final int ENERGY_COLOR = 0xFF2FC3D8;
    private static final int PROGRESS_BG_COLOR = 0xFF101419;
    private static final int PROGRESS_COLOR = 0xFFD28B3C;
    private static final int TEXT_COLOR = 0xFFE5E9ED;

    public AutomatedAssemblerScreen(AutomatedAssemblerContainer menu,
                                    PlayerInventory inventory,
                                    ITextComponent title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 192;
        this.inventoryLabelY = 99;
    }

    @Override
    protected void init() {
        super.init();
        this.addButton(new Button(
                this.leftPos + 101,
                this.topPos + 79,
                62,
                16,
                new TranslationTextComponent(
                        "gui.justguithings.automated_assembler.recipe_lock"),
                button -> {
                    if (this.minecraft != null
                            && this.minecraft.player != null
                            && this.minecraft.gameMode != null
                            && this.menu.clickMenuButton(this.minecraft.player, 0)) {
                        this.minecraft.gameMode.handleInventoryButtonClick(
                                this.menu.containerId, 0);
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

        AbstractGui.fill(matrixStack, left, top,
                left + this.imageWidth, top + this.imageHeight, PANEL_COLOR);
        AbstractGui.fill(matrixStack, left + 5, top + 18,
                left + 171, top + 98, INNER_COLOR);
        AbstractGui.fill(matrixStack, left + 5, top + 104,
                left + 171, top + 187, INNER_COLOR);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                drawSlot(matrixStack,
                        left + 29 + column * 18,
                        top + 33 + row * 18);
            }
        }
        drawSlot(matrixStack, left + 115, top + 51);
        drawSlot(matrixStack, left + 147, top + 33);
        drawSlot(matrixStack, left + 147, top + 55);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlot(matrixStack,
                        left + 7 + column * 18,
                        top + 108 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            drawSlot(matrixStack,
                    left + 7 + column * 18,
                    top + 166);
        }

        AbstractGui.fill(matrixStack,
                left + 88, top + 53,
                left + 111, top + 65,
                PROGRESS_BG_COLOR);
        int progressWidth = this.menu.getProgressScaled(21);
        if (progressWidth > 0) {
            AbstractGui.fill(matrixStack,
                    left + 89, top + 54,
                    left + 89 + progressWidth, top + 64,
                    PROGRESS_COLOR);
        }

        int energyHeight = this.menu.getEnergyScaled(60);
        AbstractGui.fill(matrixStack,
                left + 166, top + 29,
                left + 174, top + 91,
                ENERGY_BG_COLOR);
        if (energyHeight > 0) {
            AbstractGui.fill(matrixStack,
                    left + 168, top + 89 - energyHeight,
                    left + 172, top + 89,
                    ENERGY_COLOR);
        }
    }

    private void drawSlot(MatrixStack matrixStack, int x, int y) {
        AbstractGui.fill(matrixStack, x, y, x + 18, y + 18, SLOT_BORDER_COLOR);
        AbstractGui.fill(matrixStack, x + 1, y + 1, x + 17, y + 17, SLOT_COLOR);
    }

    @Override
    protected void renderLabels(MatrixStack matrixStack, int mouseX, int mouseY) {
        this.font.draw(matrixStack, this.title, 8.0F, 6.0F, TEXT_COLOR);
        SideConfigRenderer.drawHorizontal(
                matrixStack, this.font, 8.0F, 19.0F, this.menu::getSideMode);
        this.font.draw(matrixStack,
                "FE: " + this.menu.getEnergyStored() + " / "
                        + AutomatedAssemblerTileEntity.CAPACITY,
                82.0F, 6.0F, TEXT_COLOR);
        this.font.draw(matrixStack,
                new TranslationTextComponent(
                        this.menu.isRecipeLocked()
                                ? "gui.justguithings.automated_assembler.recipe_locked"
                                : "gui.justguithings.automated_assembler.recipe_unlocked"),
                88.0F, 35.0F, TEXT_COLOR);
        this.font.draw(matrixStack,
                this.menu.getCurrentEnergyPerTick() + " FE/t",
                88.0F, 68.0F, TEXT_COLOR);
        this.font.draw(matrixStack,
                "S" + this.menu.getSpeedUpgradeCount()
                        + " E" + this.menu.getEfficiencyUpgradeCount(),
                8.0F, 91.0F, TEXT_COLOR);
        this.font.draw(matrixStack,
                this.inventory.getDisplayName(),
                8.0F, this.inventoryLabelY, TEXT_COLOR);
    }
}
