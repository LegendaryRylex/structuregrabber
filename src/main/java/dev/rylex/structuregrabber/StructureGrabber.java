package dev.rylex.structuregrabber;

import dev.rylex.structuregrabber.client.StructureGrabberClient;
import dev.rylex.structuregrabber.config.GrabberConfig;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(StructureGrabber.MOD_ID)
public final class StructureGrabber {
    public static final String MOD_ID = "structuregrabber";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public StructureGrabber(IEventBus modBus, ModContainer container, Dist dist) {
        container.registerConfig(ModConfig.Type.CLIENT, GrabberConfig.SPEC);
        if (dist.isClient()) {
            StructureGrabberClient.init(container);
        }
    }
}
