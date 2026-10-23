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
        if (--be.cooldown > 0) return;
        be.cooldown = RATE;
        if (be.mode == Mode.OFF || be.health <= 0 || be.items.get(AMMO).isEmpty()) return;
        Vec3 muzzle = Vec3.atCenterOf(pos).add(0, 0.5, 0);           // above its own model, so its sight starts clear
        Optional<LivingEntity> target = server.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(RANGE),
                        e -> be.wants(server, e) && e.distanceToSqr(muzzle) <= RANGE * RANGE && be.sees(server, muzzle, e))
                .stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(muzzle)));
        if (target.isEmpty()) return;
        LivingEntity t = target.get();
        Direction face = Direction.getApproximateNearest(t.getX() - muzzle.x, 0, t.getZ() - muzzle.z);
        if (face.getAxis().isHorizontal() && state.getValue(SentryBlock.FACING) != face)
            server.setBlock(pos, state.setValue(SentryBlock.FACING, face), 2);
        be.fire(server, muzzle, t);
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
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("health", health);
        output.putInt("mode", mode.ordinal());
        if (owner != null) output.putString("owner", owner.toString());
        output.putString("owner_name", ownerName);
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
