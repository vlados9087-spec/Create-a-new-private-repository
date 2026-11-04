
package com.example.examplemod;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

public class CaseIhPumaEntity extends Entity {

    private int speed = 0;
    private static final int MAX_SPEED = 50;

    private float steering = 0.0F;
    private static final float MAX_STEERING = 35.0F;
    private static final float TURN_SPEED = 2.0F;

    private boolean forwardInput;
    private boolean brakeInput;
    private boolean leftInput;
    private boolean rightInput;

    public CaseIhPumaEntity(
            EntityType<? extends CaseIhPumaEntity> type,
            Level level
    ) {
        super(type, level);
        setNoGravity(true);
    }

    @Override
    public double getPassengersRidingOffset() {
        return 1.2D;
    }

    @Override
    public LivingEntity getControllingPassenger() {
        Entity passenger = getFirstPassenger();

        return passenger instanceof LivingEntity living
                ? living : null;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!this.level.isClientSide && getPassengers().isEmpty()) {
            player.startRiding(this);
        }

        return InteractionResult.sidedSuccess(this.level.isClientSide);
    }

    public void setControls(
            boolean forward,
            boolean brake,
            boolean left,
            boolean right
    ) {
        this.forwardInput = forward;
        this.brakeInput = brake;
        this.leftInput = left;
        this.rightInput = right;
    }

    private void accelerate() {
        speed = Math.min(MAX_SPEED, speed + 2);
    }

    private void brake() {
        speed = Math.max(0, speed - 4);
    }

    private void slowDown() {
        speed = Math.max(0, speed - 1);
    }

    private void steer(float direction) {
        steering = Math.max(
                -MAX_STEERING,
                Math.min(
                        MAX_STEERING,
                        steering + direction * TURN_SPEED
                )
        );
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

        if (this.level.isClientSide) {
            return;
        }

        if (getControllingPassenger() == null) {
            setControls(false, false, false, false);
            speed = Math.max(0, speed - 2);
            centerSteering();
        } else {
            if (brakeInput) {
                brake();
            } else if (forwardInput) {
                accelerate();
            } else {
                slowDown();
            }

            if (leftInput && !rightInput) {
                steer(1.0F);
            } else if (rightInput && !leftInput) {
                steer(-1.0F);
            } else {
                centerSteering();
            }
        }

        float speedFactor = speed / 50.0F;

        float turnFactor =
                0.02F + (1.0F - speedFactor) * 0.08F;

        if (speed > 0) {
            setYRot(getYRot() + steering * turnFactor);
        }

        double radians = Math.toRadians(getYRot());

        Vec3 direction = new Vec3(
                -Math.sin(radians),
                0,
                Math.cos(radians)
        );

        double movementSpeed = speedFactor * 0.694;

        setDeltaMovement(direction.scale(movementSpeed));

        move(MoverType.SELF, getDeltaMovement());

        setNoGravity(true);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}


