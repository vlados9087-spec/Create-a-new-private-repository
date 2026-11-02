
package com.example.examplemod;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

public class CaseIhPumaEntity extends Entity {
    @Override
public double getPassengersRidingOffset() {
    @Override
public InteractionResult interact(Player player, InteractionHand hand) {
    if (!level.isClientSide && getPassengers().isEmpty()) {
        player.startRiding(this);
    }

    return InteractionResult.sidedSuccess(level.isClientSide);
}
    return 1.2D;
}
private int speed = 0;
private static final int MAX_SPEED = 50;
private float steering = 0.0F;
private static final float MAX_STEERING = 35.0F;
private static final float TURN_SPEED = 2.0F;
    public CaseIhPumaEntity(EntityType<? extends CaseIhPumaEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    } 

private void accelerate() {
    speed = Math.min(MAX_SPEED, speed + 2);
}

private void slowDown() {
    speed = Math.max(0, speed - 1);
}private void steer(float direction) {
    steering = Math.max(-MAX_STEERING,
            Math.min(MAX_STEERING, steering + direction * TURN_SPEED));
}

private void centerSteering() {
    steering *= 0.8F;
    if (Math.abs(steering) < 0.5F) {
        steering = 0.0F;
    }
}
    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
public void tick() {
    super.tick();

    if (!this.level.isClientSide) {
        float speedFactor = speed / 50.0F;

        // Поворот: на высокой скорости плавнее
        float turnFactor = 0.02F + (1.0F - speedFactor) * 0.08F;
        setYRot(getYRot() + steering * turnFactor);

        // Направление движения
        double radians = Math.toRadians(getYRot());
        Vec3 direction = new Vec3(
                -Math.sin(radians),
                0,
                Math.cos(radians)
        );

        // Скорость в блоках за тик
        double movementSpeed = speedFactor * 0.694;

        setDeltaMovement(direction.scale(movementSpeed));
        move(net.minecraft.world.entity.MoverType.SELF, getDeltaMovement());

        setNoGravity(true);
    }
}

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}