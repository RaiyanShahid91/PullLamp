# Using PullLamp

## 1. The simple way: `LampScene`

```kotlin
val lamp = rememberLampState()
LampScene(lamp, Modifier.fillMaxSize()) {
    MyTheme(darkTheme = lamp.isDark) { MyScreens() }
}
```

`LampScene` draws, from back to front: your content, the light (a cone and a glow), and the lamp. The lamp
sits at the top end of the scene; change that with `lampAlignment` and `lampPadding`.

## 2. Only the lamp: `PullLamp`

`PullLamp(state)` draws the lamp and its cord and switches `state` when pulled. Without `LampScene` the
change is immediate (there is no spreading circle or cone) and only the bulb animates. Use it if you want
to lay the light out yourself.

## 3. Reading and changing the state

| API | Meaning |
| --- | --- |
| `lamp.isOn` / `lamp.isDark` | Light mode / dark mode. Changes the moment the cord clicks. |
| `lamp.isTransitioning` | True while the light is spreading. |
| `lamp.toggle()` | Suspend function. Plays the switching animation. Ignored if one is running. |
| `lamp.setOn(on)` | Same, but only if that is a change. |

Remember the choice yourself if you want it kept between launches (for example in DataStore) and pass it to
`rememberLampState(initialOn = saved)`.

## 4. Style

`LampStyle` holds colours, the `scale`, how the light is blended (`lightBlendMode`) and the screen reader
text. See the KDoc on each property.

## 5. Things to know

- The lamp's rectangle (about 128 x 300 dp at scale 1) takes touches. Keep buttons out of that corner.
- Your content is drawn through a layer so a picture of it is ready at the switch. Content that draws
  outside its bounds is cut at the scene's bounds in that picture.
- Dialogs, popups and the status bar are separate windows and are not part of the picture, so they change
  look at once instead of spreading.
- Haptic feedback on the click can be turned off with `haptics = false`.
