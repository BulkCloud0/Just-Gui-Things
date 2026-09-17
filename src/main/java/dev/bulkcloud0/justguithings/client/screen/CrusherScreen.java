package dev.bulkcloud0.justguithings.client.screen;

import com.mojang.blaze3d.matrix.MatrixStack;
import dev.bulkcloud0.justguithings.menu.CrusherContainer;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;

public class CrusherScreen extends ContainerScreen<CrusherContainer> {
    public CrusherScreen(CrusherContainer menu, PlayerInventory inventory, ITextComponent title) {
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
        fill(matrix, leftPos + 43, topPos + 34, leftPos + 62, topPos + 53, 0xFF111418);
        fill(matrix, leftPos + 115, topPos + 34, leftPos + 134, topPos + 53, 0xFF111418);

        int max = Math.max(1, menu.getMaxProgress());
        int progress = Math.min(24, menu.getProgress() * 24 / max);
        fill(matrix, leftPos + 76, topPos + 37, leftPos + 76 + progress, topPos + 50, 0xFFB7BDC6);

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
