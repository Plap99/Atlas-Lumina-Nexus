package com.radig.atlasluminanexus;

import com.radig.atlasluminanexus.core.AtlasConstants;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AtlasLuminaNexus implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger(AtlasConstants.MOD_ID);
    @Override public void onInitialize() {
        LOGGER.info("[ALN] {} {} | Fabric ready", AtlasConstants.MOD_NAME, AtlasConstants.VERSION);
    }
}
