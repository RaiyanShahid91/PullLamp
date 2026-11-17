# Changelog

## 0.1.0

First version.

- `LampScene`: puts a pull-cord lamp over your content, with a cone of light, a glow around the bulb and a
  new look that spreads across the screen from the bulb when the cord is pulled.
- `PullLamp`: just the lamp, if you want to lay it out yourself.
- `LampState` / `rememberLampState`: whether the light is on (light mode) or off (dark mode).
- `LampStyle`: colours, size, light blend mode and screen reader text.
- Real cord physics: a chain of points that stretches, springs back, swings with the lamp, and clicks once
  per pull.
- Accessibility: the lamp is a switch for screen readers and can be tapped instead of dragged.
