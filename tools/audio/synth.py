"""Synthesised sounds, from nothing but numpy: every gunshot is a noise burst through a low-pass, a low thump and an
exponential tail, tuned per gun; the laser is a falling zap; the mechanical sounds are clicks and clacks. Two takes
of each gunshot (different seeds) so repeated fire doesn't sound looped. Writes Ogg Vorbis straight into
assets/apocalypse_toolkit/sounds/gun/.
Run: ../Threadwork/tools/audio/.venv/bin/python tools/audio/synth.py   (any Python with numpy and soundfile)"""
import os
import numpy as np
import soundfile as sf

SR = 44100
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "src", "main", "resources", "assets",
                   "apocalypse_toolkit", "sounds", "gun")

def t(seconds):
    return np.arange(int(SR * seconds)) / SR

def lowpass(x, cutoff):
    """One-pole low-pass."""
    a = np.exp(-2 * np.pi * cutoff / SR)
    y = np.empty_like(x); acc = 0.0
    for i, v in enumerate(x):
        acc = (1 - a) * v + a * acc
        y[i] = acc
    return y

def highpass(x, cutoff):
    return x - lowpass(x, cutoff)

def env(n, attack, decay):
    tt = np.arange(n) / SR
    e = np.exp(-tt / decay)
    a = max(1, int(attack * SR))
    e[:a] *= np.linspace(0, 1, a)
    return e

def norm(x, peak=0.9):
    m = np.max(np.abs(x)) or 1.0
    return x / m * peak

def shot(seed, length, crack_cut, crack_decay, thump_hz, thump_decay, tail_decay, tail_cut, crack_gain=1.0, thump_gain=0.8, tail_gain=0.4):
    rnd = np.random.default_rng(seed)
    n = int(SR * length)
    noise = rnd.standard_normal(n)
    crack = highpass(lowpass(noise, crack_cut), 300) * env(n, 0.0005, crack_decay) * crack_gain
    tt = np.arange(n) / SR
    sweep = thump_hz * (1 + 1.5 * np.exp(-tt / 0.01))                    # a pitch drop at the front of the thump
    thump = np.sin(2 * np.pi * np.cumsum(sweep) / SR) * env(n, 0.001, thump_decay) * thump_gain
    tail = lowpass(rnd.standard_normal(n), tail_cut) * env(n, 0.01, tail_decay) * tail_gain
    x = crack + thump + tail
    return norm(np.tanh(norm(x, 1.0) * 2.2))                              # a little saturation for punch

def laser(seed):
    rnd = np.random.default_rng(seed)
    tt = t(0.32)
    f = 2200 * np.exp(-tt / 0.08) + 260
    f = f * (1 + 0.03 * np.sin(2 * np.pi * 45 * tt))                     # a wobble
    tone = np.sin(2 * np.pi * np.cumsum(f) / SR) + 0.35 * np.sign(np.sin(2 * np.pi * np.cumsum(f * 0.5) / SR))
    fizz = highpass(rnd.standard_normal(len(tt)), 3000) * 0.15
    return norm((tone + fizz) * env(len(tt), 0.002, 0.11))

def click(seed, freq=3800, decay=0.004, length=0.05, gain=1.0):
    rnd = np.random.default_rng(seed)
    n = int(SR * length)
    ring = np.sin(2 * np.pi * freq * np.arange(n) / SR) * env(n, 0.0002, decay)
    tick = highpass(rnd.standard_normal(n), 2000) * env(n, 0.0001, decay * 0.5)
    return (ring * 0.6 + tick) * gain

def place(total, *parts):
    """Mixes (start seconds, signal) parts into one clip of `total` seconds."""
    out = np.zeros(int(SR * total))
    for start, sig in parts:
        i = int(start * SR)
        j = min(len(out), i + len(sig))
        out[i:j] += sig[: j - i]
    return norm(out, 0.8)

def slide(seed, length=0.12, cut=1800):
    rnd = np.random.default_rng(seed)
    n = int(SR * length)
    return lowpass(highpass(rnd.standard_normal(n), 400), cut) * env(n, 0.02, length / 2.5) * 0.5

def beep(freq=2600, length=0.075):
    """An electronic beep: a soft-edged square wave, rounded off so it isn't harsh (a timer, not a bell)."""
    n = int(SR * length); tt = np.arange(n) / SR
    sq = np.sign(np.sin(2 * np.pi * freq * tt)) * 0.6 + np.sin(2 * np.pi * freq * tt) * 0.4
    edge = int(SR * 0.004)
    e = np.ones(n); e[:edge] = np.linspace(0, 1, edge); e[-edge:] = np.linspace(1, 0, edge)
    return norm(lowpass(sq * e, 7000), 0.7)

def roar(seed, length=0.45):
    """A flamethrower: a rushing roar (low-passed noise, a little flutter) over a low rumble, soft at both ends so it
    loops while held."""
    rnd = np.random.default_rng(seed)
    n = int(SR * length); tt = np.arange(n) / SR
    rush = lowpass(rnd.standard_normal(n), 1100) * (1 + 0.25 * np.sin(2 * np.pi * 13 * tt))
    rumble = lowpass(rnd.standard_normal(n), 140) * 1.8
    e = np.ones(n); edge = int(SR * 0.06)
    e[:edge] = np.linspace(0, 1, edge); e[-edge:] = np.linspace(1, 0, edge)
    return norm((rush + rumble) * e, 0.8)

def write(name, x):
    os.makedirs(OUT, exist_ok=True)
    sf.write(os.path.join(OUT, name + ".ogg"), x.astype(np.float32), SR, format="OGG", subtype="VORBIS")

def main():
    guns = {
        #          length crack_cut crack_dec thump thump_dec tail_dec tail_cut
        "pistol":  (0.45, 6000, 0.018, 120, 0.05, 0.12, 1800),
        "smg":     (0.30, 7500, 0.010, 150, 0.03, 0.07, 2400),
        "shotgun": (1.10, 2800, 0.040, 48, 0.18, 0.38, 800, 0.8, 1.15, 0.55),   # deep: low thump, dull crack, dark tail
        "rifle":   (0.60, 7000, 0.020, 100, 0.06, 0.18, 2000),
        "sniper":  (1.60, 9000, 0.025, 80, 0.09, 0.55, 1400),
    }
    for name, p in guns.items():
        for take in (1, 2):
            write(f"{name}{take}", shot(sum(map(ord, name)) * 10 + take, *p))
    for take in (1, 2):
        write(f"laser{take}", laser(take))
    write("empty", place(0.12, (0.0, click(1, 3200, 0.006))))
    write("reload_start", place(0.45, (0.0, click(2, 2600, 0.008)), (0.05, slide(3, 0.14)), (0.22, click(4, 1800, 0.012, gain=0.8))))
    write("reload_end", place(0.5, (0.0, slide(5, 0.1, 2200)), (0.12, click(6, 2400, 0.01)), (0.3, click(7, 3000, 0.008)), (0.34, slide(8, 0.08, 2600))))
    write("shell_in", place(0.25, (0.0, slide(9, 0.06, 1500)), (0.06, click(10, 1500, 0.015, gain=0.9))))
    write("beep", place(0.12, (0.0, beep())))
    for take in (1, 2):
        write(f"flamethrower{take}", roar(400 + take))
    print("sounds written")

if __name__ == "__main__":
    main()
