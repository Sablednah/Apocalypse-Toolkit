package com.sablednah.apocalypsetoolkit.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.phys.Vec3;

/** Client side of {@link com.sablednah.apocalypsetoolkit.gun.LaserBeamPayload}: red dots every 0.35 blocks along the beam. */
public final class LaserBeams {
    private LaserBeams() {}

    private static final DustParticleOptions RED = new DustParticleOptions(0xFF2A2A, 0.7F);

    public static void draw(Vec3 from, Vec3 to) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        double len = from.distanceTo(to);
        if (len <= 0 || len > 256) return;
        Vec3 dir = to.subtract(from).scale(1 / len);
        for (double d = 0; d < len; d += 0.35) {
            Vec3 p = from.add(dir.scale(d));
            level.addParticle(RED, p.x, p.y, p.z, 0, 0, 0);
        }
    }
}
