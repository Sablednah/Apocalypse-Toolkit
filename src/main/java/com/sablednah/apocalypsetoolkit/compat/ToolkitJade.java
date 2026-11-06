package com.sablednah.apocalypsetoolkit.compat;

import com.sablednah.apocalypsetoolkit.ApocalypseToolkit;
import com.sablednah.apocalypsetoolkit.turret.SentryBlock;
import com.sablednah.apocalypsetoolkit.turret.SentryBlockEntity;

import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade finds this by its annotation and only when Jade is installed; nothing else in the Toolkit refers to it, so a
 * game without Jade never loads a Jade class. Adds the sentry's ammo, condition and mode to Jade's tooltip, read from
 * the numbers the sentry syncs to the client (no server data provider needed).
 */
@WailaPlugin
public class ToolkitJade implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(SentryInfo.INSTANCE, SentryBlock.class);
    }

    private enum SentryInfo implements IBlockComponentProvider {
        INSTANCE;

        private static final Identifier UID = Identifier.fromNamespaceAndPath(ApocalypseToolkit.MOD_ID, "sentry");

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (accessor.getBlockEntity() instanceof SentryBlockEntity sentry) sentry.readout().forEach(tooltip::add);
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }
}
