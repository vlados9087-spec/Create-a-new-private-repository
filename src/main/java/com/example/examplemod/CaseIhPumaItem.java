
package com.example.examplemod;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class CaseIhPumaItem extends Item {

    public CaseIhPumaItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {

            CaseIhPumaEntity tractor =
                    ExampleMod.CASE_IH_PUMA_ENTITY.get().create(level);

            if (tractor == null) {
                return InteractionResultHolder.fail(stack);
            }

            float yaw = player.getYRot();
            double radians = Math.toRadians(yaw);

            double spawnX =
                    player.getX() - Math.sin(radians) * 3.2D;

            double spawnZ =
                    player.getZ() + Math.cos(radians) * 3.2D;

            tractor.moveTo(
                    spawnX,
                    player.getY(),
                    spawnZ,
                    yaw,
                    0.0F
            );

            if (!level.noCollision(
                    tractor,
                    tractor.getBoundingBox()
            )) {
                player.displayClientMessage(
                        Component.literal("Not enough space for the tractor!"),
                        true
                );

                return InteractionResultHolder.fail(stack);
            }

            level.addFreshEntity(tractor);

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        return InteractionResultHolder.sidedSuccess(
                stack,
                level.isClientSide
        );
    }
}
