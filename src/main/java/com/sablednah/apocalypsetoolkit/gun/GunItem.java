package com.sablednah.apocalypsetoolkit.gun;

import java.util.function.Consumer;

import com.sablednah.apocalypsetoolkit.ApocalypseToolkit;
import com.sablednah.apocalypsetoolkit.ModRegistry;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A gun. Right-click fires (held, for an automatic); the magazine is the {@link ModRegistry#LOADED} component, shown
 * as the item's bar. An empty gun clicks and starts reloading if its ammunition is carried; the Reload key (R by
 * default) reloads at any time. Sneaking steadies the aim (half the spread; a scoped gun zooms). A reload takes {@link GunSpec#reload()} ticks with the gun in hand
 * ({@link ModRegistry#RELOAD_UNTIL}); a shotgun loads a shell at a time and firing stops it. Shots are rays (no
 * projectile entity): each pellet traces blocks and entities out to {@link GunSpec#range()}, a hit at head height
 * takes the headshot multiplier, and damage is {@code apocalypse_toolkit:bullet} or {@code :laser}, both in
 * {@code #minecraft:is_projectile} so armour and stat systems treat it as a shot. Creative players need no ammunition.
 */
public class GunItem extends Item {
    public static final ResourceKey<DamageType> BULLET =
            ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath(ApocalypseToolkit.MOD_ID, "bullet"));
    public static final ResourceKey<DamageType> LASER =
            ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath(ApocalypseToolkit.MOD_ID, "laser"));

    private final GunSpec spec;

    public GunItem(Properties properties, GunSpec spec) {
        super(properties);
        this.spec = spec;
    }

    public GunSpec spec() {
        return spec;
    }

    public static int loaded(ItemStack stack) {
        return stack.getOrDefault(ModRegistry.LOADED.get(), 0);
    }

    public static boolean reloading(ItemStack stack) {
        return stack.has(ModRegistry.RELOAD_UNTIL.get());
    }

    // ---- firing -------------------------------------------------------------------------------

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (reloading(stack)) {
            if (!spec.perRound() || loaded(stack) <= 0) return InteractionResult.FAIL;
            stack.remove(ModRegistry.RELOAD_UNTIL.get());          // a shotgun fires out of a part-load
        }
        if (loaded(stack) <= 0) {
            if (level instanceof ServerLevel server) {
                server.playSound(null, player.blockPosition(), ModRegistry.DRY_FIRE.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
                startReload(server, player, stack);
            }
            return InteractionResult.FAIL;
        }
        if (player.getCooldowns().isOnCooldown(stack)) return InteractionResult.FAIL;
        fire(level, player, stack);
        if (spec.automatic()) player.startUsingItem(hand);
        else player.getCooldowns().addCooldown(stack, spec.cooldown());
        return InteractionResult.CONSUME;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!spec.automatic() || !(entity instanceof Player player)) return;
        int used = getUseDuration(stack, entity) - remaining;
        if (used <= 0 || used % spec.cooldown() != 0) return;
        if (loaded(stack) <= 0) {
            player.stopUsingItem();
            if (level instanceof ServerLevel server) {
                server.playSound(null, player.blockPosition(), ModRegistry.DRY_FIRE.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
                startReload(server, player, stack);
            }
            return;
        }
        fire(level, player, stack);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return spec.automatic() ? 72000 : 0;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.NONE;
    }

    private void fire(Level level, Player player, ItemStack stack) {
        if (!(level instanceof ServerLevel server)) {
            float kick = spec.recoil() * (player.isShiftKeyDown() ? 0.6F : 1F);   // the client's own view kicks
            player.setXRot(player.getXRot() - kick);
            player.setYRot(player.getYRot() + (player.getRandom().nextFloat() - 0.5F) * kick * 0.4F);
            return;
        }
        if (spec.flame()) {
            Flames.burst(server, player, stack, this);
            return;
        }
        if (!player.isCreative()) stack.set(ModRegistry.LOADED.get(), loaded(stack) - 1);
        RandomSource r = server.getRandom();
        float volume = spec.zoom() < 1F ? 4F : spec.laser() ? 1.2F : 2.5F;
        server.playSound(null, player.getX(), player.getY(), player.getZ(), spec.sound().get(), SoundSource.PLAYERS,
                volume, 0.95F + r.nextFloat() * 0.1F);
        Vec3 muzzle = player.getEyePosition().add(player.getViewVector(1F).scale(0.9)).add(0, -0.15, 0);
        server.sendParticles(spec.laser() ? ParticleTypes.END_ROD : ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z,
                spec.laser() ? 1 : 3, 0.02, 0.02, 0.02, 0.01);
        int pierce = 1 + level(server, stack, net.minecraft.world.item.enchantment.Enchantments.PIERCING);
        for (int i = 0; i < spec.pellets(); i++) ray(server, player, r, 0F, pierce);
        if (level(server, stack, net.minecraft.world.item.enchantment.Enchantments.MULTISHOT) > 0) {
            int extra = spec.pellets() > 1 ? 2 : 1;                  // two more shots, either side (a shotgun: two more pellets each)
            for (int i = 0; i < extra; i++) { ray(server, player, r, -MULTISHOT_ANGLE, pierce); ray(server, player, r, MULTISHOT_ANGLE, pierce); }
        }
    }

    /** Crossbow enchantments on a gun: Quick Charge reloads faster, Multishot fires two more shots either side, Piercing
     * goes through one more target a level. */
    static final float MULTISHOT_ANGLE = 6F, QUICK_CHARGE = 0.2F;

    private static int level(ServerLevel level, ItemStack stack, net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> key) {
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key), stack);
    }

    /** Ticks for one reload step, shortened by Quick Charge (a fifth a level, never under a quarter). */
    private int reloadTicks(ServerLevel level, ItemStack stack) {
        int qc = level(level, stack, net.minecraft.world.item.enchantment.Enchantments.QUICK_CHARGE);
        return Math.max(Math.max(1, spec.reload() / 4), Math.round(spec.reload() * (1F - QUICK_CHARGE * qc)));
    }

    private void ray(ServerLevel level, Player player, RandomSource r, float yawOffset, int pierce) {
        boolean aimed = player.isShiftKeyDown();
        float spread = spec.zoom() < 1F && aimed ? 0F : spec.spread() * (aimed ? 0.5F : 1F);
        float yaw = player.getYRot() + yawOffset + (r.nextFloat() - 0.5F) * 2F * spread;
        float pitch = Mth.clamp(player.getXRot() + (r.nextFloat() - 0.5F) * 2F * spread, -90F, 90F);
        Vec3 dir = Vec3.directionFromRotation(pitch, yaw);
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(dir.scale(spec.range()));
        BlockHitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 stop = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
        final Vec3 wall = stop;
        // everything the shot passes through before the wall, nearest first; it stops in the pierce-th
        java.util.List<java.util.Map.Entry<Entity, Vec3>> hits = new java.util.ArrayList<>();
        for (Entity e : level.getEntities(player, new AABB(eye, wall).inflate(1.0), e -> !e.isSpectator() && e.isPickable() && e.isAlive()))
            e.getBoundingBox().inflate(0.3).clip(eye, wall).ifPresent(at -> hits.add(java.util.Map.entry(e, at)));
        hits.sort(java.util.Comparator.comparingDouble(h -> h.getValue().distanceToSqr(eye)));
        if (!hits.isEmpty()) {
            for (int i = 0; i < Math.min(pierce, hits.size()); i++) {
                Entity target = hits.get(i).getKey();
                stop = hits.get(i).getValue();
                float damage = spec.damage();
                if (target instanceof LivingEntity living && stop.y >= living.getEyeY() - 0.15) damage *= spec.headshot();
                target.invulnerableTime = 0;                       // every pellet of a shotgun blast counts
                target.hurtServer(level, source(level, player), damage);
                level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, stop.x, stop.y, stop.z, 2, 0.1, 0.1, 0.1, 0.05);
            }
            if (pierce > hits.size()) stop = wall;                 // went through them all, on to the wall
        }
        if ((hits.isEmpty() || pierce > hits.size()) && block.getType() != HitResult.Type.MISS) {
            var state = level.getBlockState(block.getBlockPos());
            if (!state.isAir())
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), stop.x, stop.y, stop.z, 5, 0.05, 0.05, 0.05, 0.1);
            level.sendParticles(ParticleTypes.SMOKE, stop.x, stop.y, stop.z, 2, 0.02, 0.02, 0.02, 0.01);
        }
        if (spec.laser()) {
            double len = stop.distanceTo(eye);
            for (double d = 1.0; d < len; d += 0.35) {
                Vec3 p = eye.add(dir.scale(d)).add(0, -0.12, 0);
                level.sendParticles(new DustParticleOptions(0xFF2A2A, 0.7F), p.x, p.y, p.z, 1, 0, 0, 0, 0);
            }
        }
    }

    private DamageSource source(ServerLevel level, Player player) {
        return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(spec.laser() ? LASER : BULLET), player);
    }

    // ---- reloading ----------------------------------------------------------------------------

    /** Begins a reload if the gun isn't full and its ammunition is carried (or the player is in creative). */
    public void startReload(ServerLevel level, Player player, ItemStack stack) {
        if (reloading(stack) || loaded(stack) >= spec.magazine()) return;
        if (!player.isCreative() && count(player) <= 0) {
            player.sendOverlayMessage(Component.translatable("message.apocalypse_toolkit.no_ammo", new ItemStack(spec.ammo().get()).getHoverName()));
            return;
        }
        stack.set(ModRegistry.RELOAD_UNTIL.get(), level.getGameTime() + reloadTicks(level, stack));
        level.playSound(null, player.blockPosition(), ModRegistry.RELOAD_START.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        if (!reloading(stack)) return;
        if (!(entity instanceof Player player) || player.getMainHandItem() != stack) {
            stack.remove(ModRegistry.RELOAD_UNTIL.get());          // put away mid-reload: start again next time
            return;
        }
        if (level.getGameTime() < stack.getOrDefault(ModRegistry.RELOAD_UNTIL.get(), 0L)) return;
        int loaded = loaded(stack);
        if (spec.perRound()) {
            if (player.isCreative() || take(player, 1) == 1) loaded++;
            stack.set(ModRegistry.LOADED.get(), loaded);
            level.playSound(null, player.blockPosition(), ModRegistry.SHELL_IN.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
            if (loaded < spec.magazine() && (player.isCreative() || count(player) > 0)) {
                stack.set(ModRegistry.RELOAD_UNTIL.get(), level.getGameTime() + reloadTicks(level, stack));
                return;
            }
        } else {
            int need = spec.magazine() - loaded;
            int items = (need + spec.ammoPer() - 1) / spec.ammoPer();            // a canister fills a whole tank
            int got = player.isCreative() ? need : Math.min(need, take(player, items) * spec.ammoPer());
            stack.set(ModRegistry.LOADED.get(), loaded + got);
        }
        stack.remove(ModRegistry.RELOAD_UNTIL.get());
        level.playSound(null, player.blockPosition(), ModRegistry.RELOAD_END.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
    }

    private int count(Player player) {
        return Ammo.count(player, spec.ammo().get());
    }

    private int take(Player player, int want) {
        return Ammo.take(player, spec.ammo().get(), want);
    }

    // ---- display ------------------------------------------------------------------------------

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13F * loaded(stack) / spec.magazine());
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return reloading(stack) ? 0xF0A030 : loaded(stack) > 0 ? 0xD8D8D8 : 0xC03030;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        out.accept(Component.translatable("tooltip.apocalypse_toolkit.ammo", loaded(stack), spec.magazine(), new ItemStack(spec.ammo().get()).getHoverName())
                .withStyle(ChatFormatting.GRAY));
        String dmg = spec.pellets() > 1 ? spec.pellets() + " x " + fmt(spec.damage()) : fmt(spec.damage());
        out.accept(Component.translatable("tooltip.apocalypse_toolkit.stats", dmg, spec.range(),
                String.format(java.util.Locale.ROOT, "%.1f", 20F / spec.cooldown())).withStyle(ChatFormatting.DARK_GRAY));
        if (spec.zoom() < 1F) out.accept(Component.translatable("tooltip.apocalypse_toolkit.zoom").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static String fmt(float v) {
        return v == (int) v ? Integer.toString((int) v) : String.format(java.util.Locale.ROOT, "%.1f", v);
    }
}
