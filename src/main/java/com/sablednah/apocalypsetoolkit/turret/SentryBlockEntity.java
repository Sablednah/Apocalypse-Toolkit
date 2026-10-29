package com.sablednah.apocalypsetoolkit.turret;

import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

import com.sablednah.apocalypsetoolkit.ModRegistry;
import com.sablednah.apocalypsetoolkit.gun.GunItem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The turret's brain. Every {@link #RATE} ticks, if it is switched on, loaded and not worn out, it picks the nearest
 * target within {@link #RANGE} blocks that it can see and that its mode allows, turns to it and fires one round:
 * {@link #DAMAGE} bullet damage (rifle rounds hit harder), credited to its owner. Each shot wears it by one; at zero
 * it stops until repaired. Iron ingots in the repair slot are used up automatically, {@link #REPAIR} each.
 * Pistol and rifle rounds both feed it.
 */
public class SentryBlockEntity extends BlockEntity implements Container, MenuProvider {
    public static final int AMMO = 0, IRON = 1, RANGE = 16, RATE = 15, MAX_WEAR = 256, REPAIR = 32;
    public static final float DAMAGE = 4F, RIFLE_DAMAGE = 6F;

    /** What it shoots at; cycled by the screen's button. */
    public enum Mode {
        OFF, HOSTILES, HOSTILES_AND_PLAYERS, EVERYTHING;

        Mode next() { return values()[(ordinal() + 1) % values().length]; }

        public Component label() { return Component.translatable("sentry.apocalypse_toolkit.mode." + name().toLowerCase(java.util.Locale.ROOT)); }
    }

    private final NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
    private int health = MAX_WEAR, cooldown;
    /** Degrees a tick the head turns (180 in about 0.6 s), and how near its aim must be to fire. */
    public static final float TURN = 15F, ON_TARGET = 12F;
    /** Where the head is pointing and where it is turning to, in degrees (0 = north, the model's way); the server
     * sends the aim, and both sides turn the head towards it at the same speed. */
    private float yaw, yawO, aim;
    private int targetId = -1, ticks;
    private Mode mode = Mode.HOSTILES;
    private UUID owner;
    private String ownerName = "";

    private final ContainerData data = new ContainerData() {
        @Override public int get(int i) { return switch (i) { case 0 -> health; case 1 -> MAX_WEAR; default -> mode.ordinal(); }; }
        @Override public void set(int i, int v) { if (i == 0) health = v; else if (i == 2) mode = Mode.values()[Math.floorMod(v, 4)]; }
        @Override public int getCount() { return 3; }
    };

    public SentryBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.SENTRY_BE.get(), pos, state);
        yaw = yawO = aim = state.hasProperty(SentryBlock.FACING) ? 180F - state.getValue(SentryBlock.FACING).toYRot() : 0F;
    }

    /** The head's yaw this frame, between last tick and this one. */
    public float yaw(float partial) {
        return yawO + net.minecraft.util.Mth.wrapDegrees(yaw - yawO) * partial;
    }

    private void turn() {
        yawO = yaw;
        float d = net.minecraft.util.Mth.wrapDegrees(aim - yaw);
        yaw += net.minecraft.util.Mth.clamp(d, -TURN, TURN);
    }

    /** The head's yaw now, and where it is turning to (the self-test). */
    public float yaw() { return yaw; }
    public float aim() { return aim; }

    public static void clientTick(Level level, BlockPos pos, BlockState state, SentryBlockEntity be) {
        be.turn();
    }

    void setOwner(Player p) {
        owner = p.getUUID();
        ownerName = p.getName().getString();
        setChanged();
    }

    void cycleMode() {
        mode = mode.next();
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SentryBlockEntity be) {
        if (!(level instanceof ServerLevel server)) return;
        be.repair();
        be.ticks++;
        if (be.ticks % 10 == 0) be.pullAmmo(server, pos);
        be.turn();
        if (be.cooldown > 0) be.cooldown--;
        if (be.mode == Mode.OFF || be.health <= 0 || be.items.get(AMMO).isEmpty()) { be.targetId = -1; return; }
        Vec3 muzzle = Vec3.atCenterOf(pos).add(0, 0.5, 0);           // above its own model, so its sight starts clear
        LivingEntity t = server.getEntity(be.targetId) instanceof LivingEntity e && be.valid(server, muzzle, e) ? e : null;
        if (t == null && be.ticks % 5 == 0) {
            t = server.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(RANGE), e -> be.valid(server, muzzle, e))
                    .stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(muzzle))).orElse(null);
        }
        be.targetId = t == null ? -1 : t.getId();
        if (t == null) return;
        float want = (float) Math.toDegrees(Math.atan2(-(t.getX() - muzzle.x), -(t.getZ() - muzzle.z)));
        if (Math.abs(net.minecraft.util.Mth.wrapDegrees(want - be.aim)) > 2F) {
            be.aim = want;
            server.sendBlockUpdated(pos, state, state, 2);              // the client turns to it too
        }
        if (be.cooldown <= 0 && Math.abs(net.minecraft.util.Mth.wrapDegrees(want - be.yaw)) <= ON_TARGET) {
            be.cooldown = RATE;
            be.fire(server, muzzle, t);
        }
    }

    private boolean valid(ServerLevel level, Vec3 muzzle, LivingEntity e) {
        return wants(level, e) && e.distanceToSqr(muzzle) <= RANGE * RANGE && sees(level, muzzle, e);
    }

    /** Tops up the ammunition slot from a container underneath (a chest, barrel, hopper, AE2 interface...). */
    private void pullAmmo(ServerLevel level, BlockPos pos) {
        ItemStack have = items.get(AMMO);
        if (have.getCount() >= have.getMaxStackSize() && !have.isEmpty()) return;
        var below = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK, pos.below(), Direction.UP);
        if (below == null) return;
        for (int i = 0; i < below.size(); i++) {
            var res = below.getResource(i);
            if (res.isEmpty()) continue;
            ItemStack kind = res.toStack();
            if (!isAmmo(kind) || (!have.isEmpty() && !ItemStack.isSameItemSameComponents(have, kind))) continue;
            int room = kind.getMaxStackSize() - have.getCount();
            try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                int got = below.extract(i, res, room, tx);
                tx.commit();
                if (got <= 0) continue;
                if (have.isEmpty()) items.set(AMMO, res.toStack(got)); else have.grow(got);
                setChanged();
                return;
            }
        }
    }

    // ---- client sync: only the aim goes over the wire ----

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        tag.putFloat("aim", aim);
        return tag;
    }

    private boolean wants(ServerLevel level, LivingEntity e) {
        if (!e.isAlive() || e instanceof ArmorStand || e.isSpectator()) return false;
        if (owner != null && e.getUUID().equals(owner)) return false;
        if (e instanceof OwnableEntity pet && owner != null && owner.equals(pet.getOwnerReference() == null ? null : pet.getOwnerReference().getUUID())) return false;
        if (e instanceof Player p && (p.isCreative())) return false;
        return switch (mode) {
            case OFF -> false;
            case HOSTILES -> e instanceof Enemy;
            case HOSTILES_AND_PLAYERS -> e instanceof Enemy || e instanceof Player;
            case EVERYTHING -> true;
        };
    }

    private boolean sees(ServerLevel level, Vec3 from, LivingEntity e) {
        return level.clip(new ClipContext(from, e.getEyePosition(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                net.minecraft.world.phys.shapes.CollisionContext.empty())).getType() == HitResult.Type.MISS;
    }

    private void fire(ServerLevel level, Vec3 muzzle, LivingEntity t) {
        ItemStack ammo = items.get(AMMO);
        float damage = ammo.is(ModRegistry.RIFLE_ROUNDS.get()) ? RIFLE_DAMAGE : DAMAGE;
        ammo.shrink(1);
        health--;
        setChanged();
        var type = level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(GunItem.BULLET);
        ServerPlayer by = owner == null ? null : level.getServer().getPlayerList().getPlayer(owner);
        DamageSource source = by != null ? new DamageSource(type, by) : new DamageSource(type, muzzle);
        t.invulnerableTime = 0;
        t.hurtServer(level, source, damage);
        if (by != null && t.isDeadOrDying()) com.sablednah.apocalypsetoolkit.feats.Feats.award(by, "sentry_duty");
        level.playSound(null, muzzle.x, muzzle.y, muzzle.z, ModRegistry.SMG_SHOT.get(), SoundSource.BLOCKS, 1.5F, 1.1F);
        Vec3 dir = t.getEyePosition().subtract(muzzle).normalize();
        Vec3 flash = muzzle.add(dir.scale(0.6));
        level.sendParticles(ParticleTypes.SMOKE, flash.x, flash.y, flash.z, 2, 0.02, 0.02, 0.02, 0.01);
        level.sendParticles(ParticleTypes.CRIT, t.getX(), t.getEyeY() - 0.3, t.getZ(), 3, 0.15, 0.15, 0.15, 0.05);
    }

    private void repair() {
        ItemStack iron = items.get(IRON);
        if (health > MAX_WEAR - REPAIR || iron.isEmpty() || !iron.is(Items.IRON_INGOT)) return;
        iron.shrink(1);
        health = Math.min(MAX_WEAR, health + REPAIR);
        setChanged();
    }

    // ---- screen -------------------------------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return ownerName.isEmpty() ? Component.translatable("block.apocalypse_toolkit.sentry")
                : Component.translatable("sentry.apocalypse_toolkit.title", ownerName);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new SentryMenu(id, inv, this, data, worldPosition);
    }

    // ---- saving -------------------------------------------------------------------------------

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items.replaceAll(s -> ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        health = input.getIntOr("health", MAX_WEAR);
        mode = Mode.values()[Math.floorMod(input.getIntOr("mode", 1), 4)];
        owner = input.getString("owner").map(UUID::fromString).orElse(null);
        ownerName = input.getStringOr("owner_name", "");
        aim = input.getFloatOr("aim", aim);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("health", health);
        output.putInt("mode", mode.ordinal());
        if (owner != null) output.putString("owner", owner.toString());
        output.putString("owner_name", ownerName);
        output.putFloat("aim", aim);
    }

    // ---- Container ----------------------------------------------------------------------------

    @Override public int getContainerSize() { return items.size(); }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot, int count) { ItemStack s = ContainerHelper.removeItem(items, slot, count); if (!s.isEmpty()) setChanged(); return s; }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }
    @Override public void setItem(int slot, ItemStack stack) { items.set(slot, stack); stack.limitSize(getMaxStackSize(stack)); setChanged(); }
    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }
    @Override public void clearContent() { items.clear(); }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == AMMO ? isAmmo(stack) : stack.is(Items.IRON_INGOT);
    }

    public static boolean isAmmo(ItemStack stack) {
        return stack.is(ModRegistry.PISTOL_ROUNDS.get()) || stack.is(ModRegistry.RIFLE_ROUNDS.get());
    }
}
