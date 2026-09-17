package dev.bulkcloud0.justguithings.client.screen;

import com.mojang.blaze3d.matrix.MatrixStack;
import dev.bulkcloud0.justguithings.menu.CoalGeneratorContainer;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;

public class CoalGeneratorScreen extends ContainerScreen<CoalGeneratorContainer> {
    public CoalGeneratorScreen(CoalGeneratorContainer menu, PlayerInventory inventory, ITextComponent title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
    }

    @Override
    public void render(MatrixStack matrix, int mouseX, int mouseY, float partialTicks) {
        renderBackground(matrix);
        super.render(matrix, mouseX, mouseY, partialTicks);
        renderTooltip(matrix, mouseX, mouseY);
    }

    @Override
    protected void renderBg(MatrixStack matrix, float partialTicks, int mouseX, int mouseY) {
        fill(matrix, leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF20242A);
        fill(matrix, leftPos + 7, topPos + 17, leftPos + 169, topPos + 75, 0xFF30363D);
        fill(matrix, leftPos + 79, topPos + 34, leftPos + 98, topPos + 53, 0xFF111418);

        int maxBurn = Math.max(1, menu.getMaxBurnTime());
        int flame = Math.min(18, menu.getBurnTime() * 18 / maxBurn);
        fill(matrix, leftPos + 55, topPos + 53 - flame, leftPos + 64, topPos + 53, 0xFFFF7A00);

        int maxEnergy = Math.max(1, menu.getMaxEnergy());
        int energyHeight = Math.min(50, menu.getEnergy() * 50 / maxEnergy);
        fill(matrix, leftPos + 151, topPos + 22 + (50 - energyHeight), leftPos + 160, topPos + 72, 0xFFFFC857);
    }

    @Override
    protected void renderLabels(MatrixStack matrix, int mouseX, int mouseY) {
        font.draw(matrix, title, 8.0F, 6.0F, 0xFFFFFF);
        font.draw(matrix, inventory.getDisplayName(), 8.0F, 72.0F, 0xD0D0D0);
        font.draw(matrix, menu.getEnergy() + " FE", 104.0F, 60.0F, 0xFFFFFF);
    }
}
