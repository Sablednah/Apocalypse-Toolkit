package com.sablednah.apocalypsetoolkit.gun;

import java.util.function.Supplier;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;

/**
 * How one kind of gun behaves.
 *
 * @param damage    damage per hit (per pellet for a shotgun), before a headshot
 * @param headshot  multiplier when the hit is at head height
 * @param pellets   rays per shot (8 for the shotgun)
 * @param range     blocks
 * @param cooldown  ticks between shots
 * @param magazine  rounds it holds
 * @param reload    ticks to reload (per round when {@code perRound})
 * @param perRound  loads one round at a time (the shotgun), and can be interrupted by firing
 * @param ammo      the round it takes
 * @param spread    degrees of cone, halved while sneaking
 * @param recoil    degrees the view kicks up per shot
 * @param automatic fires for as long as use is held
 * @param zoom      field-of-view multiplier while sneaking with it (1 = none)
 * @param laser     a beam, not a bullet: a red line, no shell, its own damage type
 * @param flame     a flamethrower: a cone of fire out to {@code range} instead of bullets, held to keep it going
 * @param ammoPer   how much of the magazine one ammo item fills (a fuel canister fills a whole tank)
 * @param sound     the shot
 */
public record GunSpec(float damage, float headshot, int pellets, int range, int cooldown, int magazine, int reload,
        boolean perRound, Supplier<Item> ammo, float spread, float recoil, boolean automatic, float zoom, boolean laser,
        boolean flame, int ammoPer, Supplier<SoundEvent> sound) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private float damage = 5F, headshot = 1.5F, spread = 2F, recoil = 2F, zoom = 1F;
        private int pellets = 1, range = 48, cooldown = 8, magazine = 10, reload = 30;
        private boolean perRound, automatic, laser, flame;
        private int ammoPer = 1;
        private Supplier<Item> ammo;
        private Supplier<SoundEvent> sound;

        public Builder damage(float v) { damage = v; return this; }
        public Builder headshot(float v) { headshot = v; return this; }
        public Builder pellets(int v) { pellets = v; return this; }
        public Builder range(int v) { range = v; return this; }
        public Builder cooldown(int v) { cooldown = v; return this; }
        public Builder magazine(int v) { magazine = v; return this; }
        public Builder reload(int v) { reload = v; return this; }
        public Builder perRoundReload() { perRound = true; return this; }
        public Builder ammo(Supplier<? extends Item> v) { ammo = v::get; return this; }
        public Builder spread(float v) { spread = v; return this; }
        public Builder recoil(float v) { recoil = v; return this; }
        public Builder automatic() { automatic = true; return this; }
        public Builder zoom(float v) { zoom = v; return this; }
        public Builder laser() { laser = true; return this; }
        public Builder flame() { flame = true; return this; }
        public Builder ammoPer(int v) { ammoPer = v; return this; }
        public Builder sound(Supplier<? extends SoundEvent> v) { sound = v::get; return this; }

        public GunSpec build() {
            return new GunSpec(damage, headshot, pellets, range, cooldown, magazine, reload, perRound, ammo, spread, recoil,
                    automatic, zoom, laser, flame, ammoPer, sound);
        }
    }
}
