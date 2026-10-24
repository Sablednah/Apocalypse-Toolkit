package com.sablednah.apocalypsetoolkit.client;

import com.sablednah.apocalypsetoolkit.ApocalypseToolkit;
import com.sablednah.apocalypsetoolkit.turret.SentryMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** The turret's screen: ammunition on the left, iron for repairs on the right, a wear bar, and the target button. */
public class SentryScreen extends AbstractContainerScreen<SentryMenu> {
    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(ApocalypseToolkit.MOD_ID, "textures/gui/container/sentry.png");
    private Button modeButton;

    public SentryScreen(SentryMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, 176, 166);
    }

    @Override
    protected void init() {
        super.init();
        modeButton = addRenderableWidget(Button.builder(menu.mode().label(),
                b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0))
                .bounds(leftPos + 53, topPos + 52, 70, 16).build());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (modeButton != null) modeButton.setMessage(menu.mode().label());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        super.extractBackground(g, mouseX, mouseY, partial);
        g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
        // labels over the two slots and the bar; the bar and its numbers between them
        centred(g, Component.translatable("sentry.apocalypse_toolkit.ammo"), leftPos + 34, topPos + 19);
        centred(g, Component.translatable("sentry.apocalypse_toolkit.condition"), leftPos + 88, topPos + 19);
        centred(g, Component.translatable("sentry.apocalypse_toolkit.repair"), leftPos + 142, topPos + 19);
        int w = 64, x = leftPos + 88 - w / 2, y = topPos + 31;
        g.fill(x - 1, y - 1, x + w + 1, y + 7, 0xFF1E1E22);
        int filled = Math.round((float) w * menu.health() / menu.maxHealth());
        int colour = menu.health() * 4 > menu.maxHealth() ? 0xFF5FB05F : 0xFFC04040;
        g.fill(x, y, x + filled, y + 6, colour);
        centred(g, Component.literal(menu.health() + "/" + menu.maxHealth()), leftPos + 88, topPos + 40);
    }

    private void centred(GuiGraphicsExtractor g, Component text, int cx, int y) {
        g.text(font, text, cx - font.width(text) / 2, y, 0xFF404040, false);
    }
}
