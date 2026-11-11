
package com.example.examplemod;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Comparator;

public class CaseIhPumaEntity extends Entity {

    // ---------------- MOVEMENT ----------------

    private int speed = 0;
    private static final int MAX_SPEED = 50;
    private double verticalSpeed = 0.0D;
    private float steering = 0.0F;
    private static final float MAX_STEERING = 35.0F;
    private static final float TURN_SPEED = 2.0F;

    private boolean forwardInput;
    private boolean brakeInput;
    private boolean leftInput;
    private boolean rightInput;

    // ---------------- REAR HITCH ----------------

    private static final String HITCHABLE_TAG = "puma_hitchable";
    private static final String HITCHED_TAG = "puma_hitched";

    // Temporary model-to-world scale.
    // We will fine-tune it after rendering the tractor model.
    private static final double MODEL_SCALE = 0.25D;

    // Blockbench hitch coordinates
    private static final double HITCH_X = 0.0D;
    private static final double HITCH_Y = 3.1D;
    private static final double HITCH_Z = 6.5D;

    private float hitchLift = 0.0F;
    private static final float MAX_HITCH_LIFT = 0.75F;
    private static final float HITCH_LIFT_STEP = 0.15F;

    private static final double SEARCH_DISTANCE = 2.5D;

    private static final EntityDataAccessor<Optional<UUID>> HITCHED_ENTITY =
            SynchedEntityData.defineId(
                    CaseIhPumaEntity.class,
                    EntityDataSerializers.OPTIONAL_UUID
            );

    private boolean attachedEntityWasNoGravity = false;

    public CaseIhPumaEntity(
            EntityType<? extends CaseIhPumaEntity> type,
            Level level
    ) {
        super(type, level);this.blocksBuilding = true;
        setNoGravity(true);
        setMaxUpStep(1.0F);
    }

    @Override
public boolean canBeCollidedWith() {
    return true;
}

@Override
public boolean isPushable() {
    return true;
}

@Override
public boolean isPickable() {
    return true;
}
    // ---------------- DRIVER ----------------
@Override
protected boolean couldAcceptPassenger() {
    return true;
}

@Override
protected boolean canAddPassenger(Entity passenger) {
    return passenger instanceof Player
            && getPassengers().isEmpty();
}
    @Override
    public double getPassengersRidingOffset() {
        return 1.2D;
    }

@Override
public void positionRider(Entity passenger) {
    if (this.hasPassenger(passenger)) {

        double angle = Math.toRadians(this.getYRot());

        double offsetForward = -0.5D;

        double x = this.getX()
                - Math.sin(angle) * offsetForward;

        double y = this.getY()
                + getPassengersRidingOffset()
                + passenger.getMyRidingOffset();

        double z = this.getZ()
                + Math.cos(angle) * offsetForward;

        passenger.setPos(x, y, z);
    }
}
    @Override
    public LivingEntity getControllingPassenger() {
        Entity passenger = getFirstPassenger();

        return passenger instanceof LivingEntity living
                ? living : null;
    }

    @Override
public InteractionResult interact(Player player, InteractionHand hand) {

    if (this.level.isClientSide) {
        player.displayClientMessage(
                Component.literal("PUMA CLICK DETECTED!"),
                true
        );
    }

    if (!this.level.isClientSide) {

        // Sneak + right click = hitch interaction
        if (player.isShiftKeyDown()) {

            if (getHitchEntityId() != null) {
                detachImplement(player);
            } else {
                attachNearestImplement(player);
            }

        } else if (getPassengers().isEmpty()) {

            // Normal right click = enter tractor
            player.startRiding(this);
        }
    }

    return InteractionResult.sidedSuccess(this.level.isClientSide);
}

    // ---------------- CONTROLS ----------------

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

    private void accelerateForward() {
    if (speed < 0) {
        speed = Math.min(0, speed + 4);
    } else {
        speed = Math.min(MAX_SPEED, speed + 2);
    }
}

private void accelerateBackward() {
    if (speed > 0) {
        speed = Math.max(0, speed - 4);
    } else {
        speed = Math.max(-MAX_SPEED, speed - 2);
    }
}

private void slowDown() {
    if (speed > 0) {
        speed = Math.max(0, speed - 1);
    } else if (speed < 0) {
        speed = Math.min(0, speed + 1);
    }
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

    // ---------------- HITCH POSITION ----------------

    public void raiseHitch() {
        hitchLift = Math.min(
                MAX_HITCH_LIFT,
                hitchLift + HITCH_LIFT_STEP
        );
        updateAttachedImplement();
    }

    public void lowerHitch() {
        hitchLift = Math.max(
                0.0F,
                hitchLift - HITCH_LIFT_STEP
        );
        updateAttachedImplement();
    }

    private Vec3 getHitchWorldPosition() {

        double radians = Math.toRadians(getYRot());

        Vec3 forward = new Vec3(
                -Math.sin(radians),
                0,
                Math.cos(radians)
        );

        Vec3 right = new Vec3(
                Math.cos(radians),
                0,
                Math.sin(radians)
        );

        return position()
                .add(right.scale(HITCH_X * MODEL_SCALE))
                .add(0, HITCH_Y * MODEL_SCALE + hitchLift, 0)
                .subtract(forward.scale(HITCH_Z * MODEL_SCALE));
    }

    private UUID getHitchEntityId() {
        return entityData.get(HITCHED_ENTITY).orElse(null);
    }

    // ---------------- ATTACH IMPLEMENT ----------------

    private void attachNearestImplement(Player player) {

        Vec3 hitchPosition = getHitchWorldPosition();

        AABB searchBox = new AABB(
                hitchPosition.x - SEARCH_DISTANCE,
                hitchPosition.y - SEARCH_DISTANCE,
                hitchPosition.z - SEARCH_DISTANCE,
                hitchPosition.x + SEARCH_DISTANCE,
                hitchPosition.y + SEARCH_DISTANCE,
                hitchPosition.z + SEARCH_DISTANCE
        );

        List<Entity> candidates = level.getEntities(
                this,
                searchBox,
                entity ->
                        !(entity instanceof Player)
                        && entity.getVehicle() == null
                        && entity.getPassengers().isEmpty()
                        && entity.getTags().contains(HITCHABLE_TAG)
                        && !entity.getTags().contains(HITCHED_TAG)
        );

        Entity nearest = candidates.stream()
                .min(Comparator.comparingDouble(
                        entity -> entity.position().distanceToSqr(hitchPosition)
                ))
                .orElse(null);

        if (nearest == null ||
                nearest.position().distanceToSqr(hitchPosition)
                        > SEARCH_DISTANCE * SEARCH_DISTANCE) {

            player.displayClientMessage(
                    Component.literal(
                            "No hitchable equipment nearby!"
                    ),
                    true
            );
            return;
        }

        attachedEntityWasNoGravity = nearest.isNoGravity();

        nearest.setNoGravity(true);
        nearest.setDeltaMovement(Vec3.ZERO);
        nearest.addTag(HITCHED_TAG);

        entityData.set(
                HITCHED_ENTITY,
                Optional.of(nearest.getUUID())
        );

        positionAttachedImplement(nearest);

        player.displayClientMessage(
                Component.literal("Equipment attached to Puma 185!"),
                true
        );
    }

    // ---------------- DETACH IMPLEMENT ----------------

    private void detachImplement(Player player) {

        Entity implement = getAttachedImplement();

        if (implement != null) {

            implement.setNoGravity(attachedEntityWasNoGravity);
            implement.setDeltaMovement(Vec3.ZERO);
            implement.removeTag(HITCHED_TAG);
        }

        entityData.set(HITCHED_ENTITY, Optional.empty());

        attachedEntityWasNoGravity = false;

        player.displayClientMessage(
                Component.literal("Equipment detached!"),
                true
        );
    }

    // ---------------- FOLLOW TRACTOR ----------------

    private Entity getAttachedImplement() {

        UUID id = getHitchEntityId();

        if (id == null || !(level instanceof ServerLevel serverLevel)) {
            return null;
        }

        return serverLevel.getEntity(id);
    }

    private void positionAttachedImplement(Entity implement) {

        Vec3 hitchPosition = getHitchWorldPosition();

        implement.setPos(
                hitchPosition.x,
                hitchPosition.y,
                hitchPosition.z
        );

        implement.setDeltaMovement(Vec3.ZERO);
        implement.setYRot(getYRot());
        implement.setXRot(0.0F);
    }

    private void updateAttachedImplement() {

        if (!(level instanceof ServerLevel)) {
            return;
        }

        UUID id = getHitchEntityId();

        if (id == null) {
            return;
        }

        Entity implement = getAttachedImplement();

        if (implement == null || implement.isRemoved()) {

            entityData.set(HITCHED_ENTITY, Optional.empty());
            attachedEntityWasNoGravity = false;

            return;
        }

        positionAttachedImplement(implement);
    }

    // ---------------- ENTITY DATA ----------------

    @Override
    protected void defineSynchedData() {

        entityData.define(HITCHED_ENTITY, Optional.empty());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {

        if (tag.hasUUID("HitchEntity")) {

            entityData.set(
                    HITCHED_ENTITY,
                    Optional.of(tag.getUUID("HitchEntity"))
            );

            attachedEntityWasNoGravity =
                    tag.getBoolean("HitchWasNoGravity");
        }

        hitchLift = Math.max(
                0.0F,
                Math.min(MAX_HITCH_LIFT, tag.getFloat("HitchLift"))
        );
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {

        entityData.get(HITCHED_ENTITY).ifPresent(
                uuid -> tag.putUUID("HitchEntity", uuid)
        );

        tag.putBoolean(
                "HitchWasNoGravity",
                attachedEntityWasNoGravity
        );

        tag.putFloat("HitchLift", hitchLift);
    }

    // ---------------- TICK ----------------

    


@Override
public void tick() {
    super.tick();

    // Driver check
    if (getControllingPassenger() == null) {
        setControls(false, false, false, false);

        if (speed > 0) {
            speed = Math.max(0, speed - 2);
        } else if (speed < 0) {
            speed = Math.min(0, speed + 2);
        }

        centerSteering();

    } else {

        // Acceleration and reverse
        if (forwardInput && !brakeInput) {
            accelerateForward();
        } else if (brakeInput && !forwardInput) {
            accelerateBackward();
        } else {
            slowDown();
        }

        // Steering
        if (leftInput && !rightInput) {
            steer(-1.0F);
        } else if (rightInput && !leftInput) {
            steer(1.0F);
        } else {
            centerSteering();
        }
    }

    // Turning
    double speedFactor = Math.abs(speed) / 50.0D;

    double turnFactor =
            0.02D + (1.0D - speedFactor) * 0.08D;

    if (speed != 0) {
        setYRot(
                getYRot()
                + (float)(steering * turnFactor * Math.signum(speed))
        );
    }

    // Direction
    double radians = Math.toRadians(getYRot());

    Vec3 direction = new Vec3(
            -Math.sin(radians),
            0,
            Math.cos(radians)
    );

    // Movement with block collision and hill climbing
double movementSpeed = (speed / 50.0D) * 0.694D;

// Gravity and ground contact
if (this.isOnGround()) {
    verticalSpeed = -0.08D;
} else {
    verticalSpeed = Math.max(
            verticalSpeed - 0.08D,
            -0.6D
    );
}

// Move tractor
move(
        MoverType.SELF,
        new Vec3(
                direction.x * movementSpeed,
                verticalSpeed,
                direction.z * movementSpeed
        )
);

// Stop vertical movement after collision
if (this.verticalCollision) {
    verticalSpeed = 0.0D;
}

setDeltaMovement(Vec3.ZERO);

   
    // Update attached equipment
    if (!this.level.isClientSide) {
        updateAttachedImplement();
    }
}

    // ---------------- SPAWN PACKET ----------------

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}

