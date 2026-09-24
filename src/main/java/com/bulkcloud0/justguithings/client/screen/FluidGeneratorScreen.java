package com.bulkcloud0.justguithings.client.screen;

import com.bulkcloud0.justguithings.world.container.FluidGeneratorContainer;
import com.bulkcloud0.justguithings.world.tile.FluidGeneratorTileEntity;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;

public class FluidGeneratorScreen extends ContainerScreen<FluidGeneratorContainer> {
    private static final int PANEL=0xFF20262E, INNER=0xFF313942, SLOT=0xFF11161C, SLOT_BORDER=0xFF697582;
    private static final int BAR_BG=0xFF101419, ENERGY=0xFF2FC3D8, FLUID=0xFF397FE6, TEXT=0xFFE5E9ED;

    public FluidGeneratorScreen(FluidGeneratorContainer menu, PlayerInventory inventory, ITextComponent title) {
        super(menu, inventory, title);
        imageWidth=176; imageHeight=166; inventoryLabelY=72;
    }

    @Override
    public void render(MatrixStack matrixStack,int mouseX,int mouseY,float partialTicks){
        renderBackground(matrixStack); super.render(matrixStack,mouseX,mouseY,partialTicks); renderTooltip(matrixStack,mouseX,mouseY);
    }

    @Override
    protected void renderBg(MatrixStack matrixStack,float partialTicks,int mouseX,int mouseY){
        int left=leftPos, top=topPos;
        AbstractGui.fill(matrixStack,left,top,left+imageWidth,top+imageHeight,PANEL);
        AbstractGui.fill(matrixStack,left+5,top+18,left+171,top+76,INNER);
        AbstractGui.fill(matrixStack,left+5,top+80,left+171,top+161,INNER);
        int fluidHeight=menu.getFluidScaled(46);
        AbstractGui.fill(matrixStack,left+126,top+24,left+140,top+72,BAR_BG);
        if(fluidHeight>0) AbstractGui.fill(matrixStack,left+128,top+70-fluidHeight,left+138,top+70,FLUID);
        int energyHeight=menu.getEnergyScaled(46);
        AbstractGui.fill(matrixStack,left+147,top+24,left+161,top+72,BAR_BG);
        if(energyHeight>0) AbstractGui.fill(matrixStack,left+149,top+70-energyHeight,left+159,top+70,ENERGY);
        for(int row=0;row<3;row++) for(int column=0;column<9;column++) drawSlot(matrixStack,left+7+column*18,top+83+row*18);
        for(int column=0;column<9;column++) drawSlot(matrixStack,left+7+column*18,top+141);
    }

    private void drawSlot(MatrixStack matrixStack,int x,int y){
        AbstractGui.fill(matrixStack,x,y,x+18,y+18,SLOT_BORDER);
        AbstractGui.fill(matrixStack,x+1,y+1,x+17,y+17,SLOT);
    }

    @Override
    protected void renderLabels(MatrixStack matrixStack,int mouseX,int mouseY){
        font.draw(matrixStack,title,8.0F,6.0F,TEXT);
        SideConfigRenderer.drawCompactGrid(matrixStack,font,8.0F,28.0F,menu::getSideMode);
        font.draw(matrixStack,I18n.get("screen.justguithings.fluid_generator.fluid",menu.getFluidAmount()),48.0F,26.0F,TEXT);
        font.draw(matrixStack,I18n.get("screen.justguithings.fluid_generator.batch",menu.getBatchEnergyRemaining()),48.0F,40.0F,TEXT);
        font.draw(matrixStack,I18n.get("screen.justguithings.fluid_generator.generation",FluidGeneratorTileEntity.GENERATION_PER_TICK),48.0F,54.0F,TEXT);
        font.draw(matrixStack,inventory.getDisplayName(),8.0F,inventoryLabelY,TEXT);
    }
}
