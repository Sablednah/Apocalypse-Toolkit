package com.sablednah.apocalypsetoolkit;

import com.sablednah.apocalypsetoolkit.gun.ReloadPayload;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

/** Apocalypse Toolkit: guns, ammo, throwables, field medicine, sentries and improvised gear. See docs/DESIGN.md. */
@Mod(ApocalypseToolkit.MOD_ID)
public final class ApocalypseToolkit {
    public static final String MOD_ID = "apocalypse_toolkit";

    public ApocalypseToolkit(IEventBus modBus) {
        ModRegistry.register(modBus);
        ReloadPayload.register(modBus);
        if (FMLEnvironment.getDist().isClient()) com.sablednah.apocalypsetoolkit.client.ToolkitClient.register(modBus);
    }
}
