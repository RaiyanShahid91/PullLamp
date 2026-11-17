"""Draws docs/preview.gif.

The cord and lamp motion comes straight from the library's real rope engine (CordSimulation and
LampRig, dumped to CSV by a small Kotlin program). Only the drawing is re-created here with PIL, so
this is a faithful picture of the motion but not a screen recording of the Compose UI.

usage: python3 render_preview.py frames.csv out.gif
"""
import math
import sys
import numpy as np
from PIL import Image, ImageDraw, ImageFont

W, H, S = 300, 520, 2
FPS = 60
LAMP_W, LAMP_H = 128, 300
LAMP_X = W - 24 - LAMP_W
PIVOT = 64.0
SHADE_TOP, SHADE_BOTTOM = 40.0, 102.0
TOP_HW, BOT_HW = 18.0, 50.0
ORIGIN = (LAMP_X + PIVOT, 104.0)
LIGHT = (255, 213, 138)
CONE_A, GLOW_A = 0.32, 0.22
REVEAL_S, FAR = 0.9, math.hypot(ORIGIN[0], H - ORIGIN[1])

FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
BOLD = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
def font(sz, bold=False): return ImageFont.truetype(BOLD if bold else FONT, int(sz * S))

THEMES = {
    "light": dict(bg=(255, 251, 243), text=(42, 38, 32), sub=(110, 100, 88), card=(241, 233, 216), accent=(46, 125, 107)),
    "dark": dict(bg=(14, 16, 19), text=(231, 227, 218), sub=(150, 148, 142), card=(27, 31, 38), accent=(127, 209, 188)),
}

def scene(theme, taps=0):
    c = THEMES[theme]
    im = Image.new("RGB", (W * S, H * S), c["bg"])
    d = ImageDraw.Draw(im)
    d.text((20 * S, 40 * S), "Good evening", font=font(21), fill=c["text"])
    d.text((20 * S, 76 * S), "Pull the cord to change the light.", font=font(10.5), fill=c["sub"])
    # switch row
    d.rounded_rectangle((20 * S, 126 * S, 56 * S, 146 * S), 10 * S, fill=c["accent"])
    d.ellipse((38 * S, 129 * S, 53 * S, 144 * S), fill=c["bg"])
    d.text((66 * S, 128 * S), "Notifications", font=font(12), fill=c["text"])
    d.rounded_rectangle((20 * S, 164 * S, 100 * S, 192 * S), 14 * S, fill=c["accent"])
    d.text((33 * S, 171 * S), "Taps: %d" % taps, font=font(11, True), fill=c["bg"])
    # gradient hero card
    hero = np.zeros((120 * S, (W - 40) * S, 3), dtype=np.float32)
    g = np.linspace(0, 1, (W - 40) * S)[None, :, None]
    a, b = np.array([46, 125, 107], np.float32), np.array([224, 162, 75], np.float32)
    hero[:] = a * (1 - g) + b * g
    mask = Image.new("L", ((W - 40) * S, 120 * S), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, (W - 40) * S - 1, 120 * S - 1), 20 * S, fill=255)
    im.paste(Image.fromarray(hero.astype(np.uint8)), (20 * S, 274 * S), mask)
    d.text((36 * S, 362 * S), "Your day", font=font(15, True), fill=(255, 255, 255))
    for k, title in enumerate(["Morning run", "Design review"]):
        y = 414 + k * 76
        d.rounded_rectangle((20 * S, y * S, (W - 20) * S, (y + 64) * S), 14 * S, fill=c["card"])
        d.text((36 * S, (y + 12) * S), title, font=font(13, True), fill=c["text"])
        d.text((36 * S, (y + 36) * S), "Tap to open", font=font(10), fill=c["sub"])
    return im

def vgrad(w, h, stops):
    xs = np.linspace(0, 1, w)
    cols = np.zeros((w, 3), np.float32)
    pos = [p for p, _ in stops]
    for ch in range(3):
        cols[:, ch] = np.interp(xs, pos, [c[ch] for _, c in stops])
    return np.tile(cols[None, :, :], (h, 1, 1))

def lerp_c(a, b, t): return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))

def lamp_layer(angle, lit, pts):
    ls = LAMP_W * S, LAMP_H * S
    body = Image.new("RGBA", ls, (0, 0, 0, 0))
    d = ImageDraw.Draw(body)
    p = lambda v: v * S
    # cable
    d.line((p(PIVOT), p(6), p(PIVOT), p(SHADE_TOP)), fill=(59, 59, 66, 255), width=int(2 * S))
    # warm bloom behind bulb
    if lit > 0.01:
        R = 62
        yy, xx = np.mgrid[0:ls[1], 0:ls[0]]
        dist = np.hypot(xx - p(PIVOT), yy - p(104)) / p(R)
        al = np.clip(1 - dist, 0, 1) * 0.55 * lit
        layer = np.zeros((ls[1], ls[0], 4), np.uint8)
        layer[..., 0], layer[..., 1], layer[..., 2] = LIGHT
        layer[..., 3] = (al * 255).astype(np.uint8)
        body = Image.alpha_composite(body, Image.fromarray(layer, "RGBA"))
        d = ImageDraw.Draw(body)
    # bulb
    bulb = lerp_c((185, 190, 198), LIGHT, lit)
    d.ellipse((p(PIVOT - 11), p(104 - 11), p(PIVOT + 11), p(104 + 11)), fill=tuple(int(v) for v in bulb) + (255,))
    d.ellipse((p(PIVOT - 7), p(104 - 8), p(PIVOT - 2), p(104 - 3)), fill=(255, 255, 255, int(150 + 90 * lit)))
    # shade with horizontal shading
    base = np.array([46, 125, 107], np.float32)
    dark1, dark2, hi = base * 0.65, base * 0.55, base + (255 - base) * 0.28
    x0, x1 = PIVOT - BOT_HW, PIVOT + BOT_HW
    grad = np.zeros((ls[1], ls[0], 3), np.float32)
    xs = (np.arange(ls[0]) / S - x0) / (x1 - x0)
    stops = [(0, dark1), (0.28, base), (0.42, hi), (0.7, base), (1, dark2)]
    for ch in range(3):
        col = np.interp(xs, [s for s, _ in stops], [c[ch] for _, c in stops])
        grad[:, :, ch] = col[None, :]
    shade_mask = Image.new("L", ls, 0)
    ImageDraw.Draw(shade_mask).polygon([(p(PIVOT - TOP_HW), p(SHADE_TOP)), (p(PIVOT + TOP_HW), p(SHADE_TOP)), (p(x1), p(SHADE_BOTTOM)), (p(x0), p(SHADE_BOTTOM))], fill=255)
    body.paste(Image.fromarray(grad.astype(np.uint8)).convert("RGBA"), (0, 0), shade_mask)
    d = ImageDraw.Draw(body)
    rim = lerp_c((23, 24, 27), LIGHT, lit)
    d.ellipse((p(x0), p(SHADE_BOTTOM - 5), p(x1), p(SHADE_BOTTOM + 5)), fill=tuple(int(v) for v in rim) + (255,), outline=(90, 72, 30, 255), width=int(1.4 * S))
    d.rounded_rectangle((p(PIVOT - TOP_HW - 2), p(SHADE_TOP - 4), p(PIVOT + TOP_HW + 2), p(SHADE_TOP + 4)), int(4 * S), fill=(201, 162, 75, 255))
    body = body.rotate(-math.degrees(angle), resample=Image.BICUBIC, center=(p(PIVOT), 0))
    # canopy (does not swing)
    d = ImageDraw.Draw(body)
    d.rounded_rectangle((p(PIVOT - 14), 0, p(PIVOT + 14), p(7)), int(3 * S), fill=(59, 59, 66, 255))
    # cord, drawn in lamp space (it already includes the swing)
    cord = Image.new("RGBA", ls, (0, 0, 0, 0))
    cd = ImageDraw.Draw(cord)
    poly = [(pts[0][0], pts[0][1])]
    for i in range(1, len(pts) - 1):
        a = pts[i]
        b = ((pts[i][0] + pts[i + 1][0]) / 2, (pts[i][1] + pts[i + 1][1]) / 2)
        prev = poly[-1]
        for t in (0.25, 0.5, 0.75, 1.0):
            poly.append(((1 - t) ** 2 * prev[0] + 2 * (1 - t) * t * a[0] + t * t * b[0], (1 - t) ** 2 * prev[1] + 2 * (1 - t) * t * a[1] + t * t * b[1]))
    poly.append(pts[-1])
    sp = [(x * S, y * S) for x, y in poly]
    cd.line(sp, fill=(130, 112, 80, 255), width=int(3.1 * S), joint="curve")
    cd.line(sp, fill=(217, 200, 164, 255), width=int(2.2 * S), joint="curve")
    ex, ey = pts[-1]; px_, py_ = pts[-2]
    ang = math.atan2(ey - py_, ex - px_); dx, dy = math.cos(ang), math.sin(ang)
    bx, by = ex + dx * 4.2, ey + dy * 4.2
    cd.line((bx * S, by * S, (bx + dx * 13) * S, (by + dy * 13) * S), fill=(217, 200, 164, 255), width=int(3.4 * S))
    cd.ellipse(((bx - 7) * S, (by - 7) * S, (bx + 7) * S, (by + 7) * S), fill=(201, 162, 75, 255), outline=(110, 84, 30, 255), width=int(1 * S))
    cd.ellipse(((bx - 4.5) * S, (by - 5) * S, (bx - 1.5) * S, (by - 2) * S), fill=(255, 238, 190, 255))
    return Image.alpha_composite(body, cord)

def screen_over(img, rgba_alpha, color):
    base = np.asarray(img, np.float32) / 255.0
    c = np.array(color, np.float32) / 255.0
    scr = 1 - (1 - base) * (1 - c[None, None, :])
    a = rgba_alpha[..., None]
    return Image.fromarray((np.clip(base + (scr - base) * a, 0, 1) * 255).astype(np.uint8))

def light_overlay(img, lit):
    if lit <= 0.01:
        return img
    ow, oh = ORIGIN[0] * S, ORIGIN[1] * S
    yy, xx = np.mgrid[0:H * S, 0:W * S].astype(np.float32)
    dist = np.hypot(xx - ow, yy - oh) / (0.9 * max(W, H) * S)
    glow = np.clip(1 - dist, 0, 1) * GLOW_A * lit
    top, bot = oh, H * S
    spread = BOT_HW * S + (yy - top) / S * 0.8 * S
    inside = (yy >= top) & (np.abs(xx - ow) <= spread)
    t = np.clip((yy - top) / (bot - top), 0, 1)
    prof = np.where(t < 0.5, 1 - 0.65 * (t / 0.5), 0.35 * (1 - (t - 0.5) / 0.5))
    cone = np.where(inside, prof * CONE_A * lit, 0)
    a = np.clip(glow + cone, 0, 1).astype(np.float32)[..., None]
    base = np.asarray(img, np.float32)
    col = np.array(LIGHT, np.float32)[None, None, :]
    return Image.fromarray(np.clip(base + (col - base) * a, 0, 255).astype(np.uint8))

def flicker(ms):
    k = [(0, 0), (50, .85), (110, .2), (180, .95), (250, .5), (420, 1)]
    return float(np.interp(ms, [a for a, _ in k], [b for _, b in k]))

def render(rows, clicks):
    frames = []
    cache = {t: scene(t) for t in THEMES}
    on = True
    for f, row in enumerate(rows):
        if f % 2: continue
        angle = float(row[0])
        pts = [(float(row[2 + 2 * i]), float(row[3 + 2 * i])) for i in range(15)]
        # state timeline from the real click frames
        theme_old, theme_new, lit, p = "light", "light", 1.0, 1.0
        done = [c for c in clicks if c <= f]
        state_on = (len(done) % 2 == 0)
        if done:
            c = done[-1]
            ms = (f - c) * 1000.0 / FPS
            was_on = ((len(done) - 1) % 2 == 0)
            theme_old = "light" if was_on else "dark"
            theme_new = "dark" if was_on else "light"
            if ms < REVEAL_S * 1000: p = 1 - (1 - ms / (REVEAL_S * 1000)) ** 3
            else: p = 1.0
            lit = flicker(ms) if not was_on else max(0.0, 1 - ms / 220.0)
            if ms > 420 and not was_on: lit = 1.0
        else:
            theme_new = theme_old = "light"
        base = cache[theme_new].copy() if p >= 1.0 else cache[theme_old].copy()
        if p < 1.0:
            r = p * FAR * S
            mask = Image.new("L", (W * S, H * S), 0)
            ImageDraw.Draw(mask).ellipse((ORIGIN[0] * S - r, ORIGIN[1] * S - r, ORIGIN[0] * S + r, ORIGIN[1] * S + r), fill=255)
            base.paste(cache[theme_new], (0, 0), mask)
            yy, xx = np.mgrid[0:H * S, 0:W * S].astype(np.float32)
            d = np.hypot(xx - ORIGIN[0] * S, yy - ORIGIN[1] * S)
            ring = np.exp(-((d - r) / (14 * S)) ** 2) * 0.28 * (1 - p)
            arr = np.asarray(base, np.float32)
            col = np.array(LIGHT, np.float32)
            arr = arr + (col[None, None, :] - arr) * ring[..., None]
            base = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8))
        base = light_overlay(base, lit)
        lamp = lamp_layer(angle, lit, pts)
        base = base.convert("RGBA")
        base.alpha_composite(lamp, (LAMP_X * S, 0))
        d = ImageDraw.Draw(base)
        base = base.convert("RGB").resize((W, H), Image.LANCZOS)
        frames.append(base)
    return frames

if __name__ == "__main__":
    rows = [line.strip().split(",") for line in open(sys.argv[1]) if line.strip()]
    clicks = [i for i, r in enumerate(rows) if r[1] == "1"]
    frames = render(rows, clicks)
    n = len(frames)
    probe = Image.new("RGB", (W * 4, H))
    for i, k in enumerate([0, int(n * 0.25), int(n * 0.5), int(n * 0.72)]):
        probe.paste(frames[min(k, n - 1)], (i * W, 0))
    shared = probe.quantize(255, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE)
    pal = [f.quantize(palette=shared, dither=Image.Dither.NONE) for f in frames]
    pal[0].save(sys.argv[2], save_all=True, append_images=pal[1:], duration=int(2000 / FPS), loop=0, optimize=False)
    print("frames", len(frames), "clicks", clicks)
