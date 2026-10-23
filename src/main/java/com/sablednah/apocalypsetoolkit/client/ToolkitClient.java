package com.sablednah.apocalypsetoolkit.client;

import org.lwjgl.glfw.GLFW;

import com.sablednah.apocalypsetoolkit.ApocalypseToolkit;
import com.sablednah.apocalypsetoolkit.ModRegistry;
import com.sablednah.apocalypsetoolkit.gun.GunItem;
import com.sablednah.apocalypsetoolkit.gun.ReloadPayload;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;

/** Client side: the Reload key, the scope's zoom while sneaking, and how thrown grenades and molotovs look. */
public final class ToolkitClient {
    private ToolkitClient() {}

    private static final KeyMapping.Category CATEGORY =
            new KeyMapping.Category(Identifier.fromNamespaceAndPath(ApocalypseToolkit.MOD_ID, "toolkit"));
    public static final KeyMapping RELOAD = new KeyMapping("key.apocalypse_toolkit.reload", GLFW.GLFW_KEY_B, CATEGORY);

    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent e) -> {
            e.registerCategory(CATEGORY);
            e.register(RELOAD);
        });
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterMenuScreensEvent e) ->
                e.register(ModRegistry.SENTRY_MENU.get(), SentryScreen::new));
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> {
            e.registerEntityRenderer(ModRegistry.GRENADE_ENTITY.get(), ThrownItemRenderer::new);
            e.registerEntityRenderer(ModRegistry.MOLOTOV_ENTITY.get(), ThrownItemRenderer::new);
        });
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            while (RELOAD.consumeClick()) {
                var mc = Minecraft.getInstance();
                if (mc.player != null && mc.getConnection() != null && mc.player.getMainHandItem().getItem() instanceof GunItem)
                    ClientPacketDistributor.sendToServer(new ReloadPayload());
            }
        });
        NeoForge.EVENT_BUS.addListener((ComputeFovModifierEvent e) -> {
            var player = e.getPlayer();
            if (player.isShiftKeyDown() && player.getMainHandItem().getItem() instanceof GunItem gun && gun.spec().zoom() < 1F)
                e.setNewFovModifier(e.getNewFovModifier() * gun.spec().zoom());
        });
    }
}
