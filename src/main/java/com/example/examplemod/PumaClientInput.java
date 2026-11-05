
package com.example.examplemod;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = ExampleMod.MODID,
        value = Dist.CLIENT
)
public class PumaClientInput {

    private static boolean wasDriving = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {

        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null || minecraft.level == null) {
            wasDriving = false;
            return;
        }

        boolean driving =
                minecraft.player.getVehicle() instanceof CaseIhPumaEntity;

        if (driving) {

            // Tractor movement
            PumaNetwork.sendControls(
                    minecraft.options.keyUp.isDown(),
                    minecraft.options.keyDown.isDown(),
                    minecraft.options.keyLeft.isDown(),
                    minecraft.options.keyRight.isDown()
            );

            // Raise rear hitch with R
            while (PumaKeyBindings.HITCH_RAISE.consumeClick()) {
                PumaNetwork.sendHitchCommand(true);
            }

            // Lower rear hitch with V
            while (PumaKeyBindings.HITCH_LOWER.consumeClick()) {
                PumaNetwork.sendHitchCommand(false);
            }

            wasDriving = true;

        } else if (wasDriving) {

            PumaNetwork.sendControls(
                    false,
                    false,
                    false,
                    false
            );

            wasDriving = false;
        }
    }
}

