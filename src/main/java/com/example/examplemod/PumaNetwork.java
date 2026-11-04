
package com.example.examplemod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

public class PumaNetwork {

    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL =
            NetworkRegistry.newSimpleChannel(
                    new ResourceLocation(ExampleMod.MODID, "main"),
                    () -> PROTOCOL,
                    PROTOCOL::equals,
                    PROTOCOL::equals
            );

    public static void register() {
        CHANNEL.registerMessage(
                0,
                ControlsPacket.class,
                ControlsPacket::encode,
                ControlsPacket::new,
                ControlsPacket::handle
        );
    }

    public static void sendControls(
            boolean forward,
            boolean brake,
            boolean left,
            boolean right
    ) {
        CHANNEL.sendToServer(
                new ControlsPacket(forward, brake, left, right)
        );
    }

    public static class ControlsPacket {

        private final boolean forward;
        private final boolean brake;
        private final boolean left;
        private final boolean right;

        public ControlsPacket(
                boolean forward,
                boolean brake,
                boolean left,
                boolean right
        ) {
            this.forward = forward;
            this.brake = brake;
            this.left = left;
            this.right = right;
        }

        public ControlsPacket(FriendlyByteBuf buffer) {
            this(
                    buffer.readBoolean(),
                    buffer.readBoolean(),
                    buffer.readBoolean(),
                    buffer.readBoolean()
            );
        }

        public void encode(FriendlyByteBuf buffer) {
            buffer.writeBoolean(forward);
            buffer.writeBoolean(brake);
            buffer.writeBoolean(left);
            buffer.writeBoolean(right);
        }

        public static void handle(
                ControlsPacket packet,
                Supplier<NetworkEvent.Context> supplier
        ) {
            NetworkEvent.Context context = supplier.get();

            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();

                if (player == null) {
                    return;
                }

                if (player.getVehicle() instanceof CaseIhPumaEntity tractor
                        && tractor.getControllingPassenger() == player) {

                    tractor.setControls(
                            packet.forward,
                            packet.brake,
                            packet.left,
                            packet.right
                    );
                }
            });

            context.setPacketHandled(true);
        }
    }
}
