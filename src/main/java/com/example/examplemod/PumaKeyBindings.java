package com.example.examplemod;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = ExampleMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public class PumaKeyBindings {

    public static final KeyMapping HITCH_RAISE =
            new KeyMapping(
                    "key.caseihpuma.hitch_raise",
                    InputConstants.Type.KEYSYM,
                    InputConstants.KEY_R,
                    "key.categories.misc"
            );

    public static final KeyMapping HITCH_LOWER =
            new KeyMapping(
                    "key.caseihpuma.hitch_lower",
                    InputConstants.Type.KEYSYM,
                    InputConstants.KEY_F,
                    "key.categories.misc"
            );

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(HITCH_RAISE);
        event.register(HITCH_LOWER);
    }
}
