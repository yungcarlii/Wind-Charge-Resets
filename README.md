# [![github](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/github_64h.png)](https://github.com/yungcarlii/Wind-Charge-Resets) [![modrinth](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/modrinth_64h.png) ](https://modrinth.com/mod/Wind-Charge-Resets)
---
# Wind-Charge-Resets
Replicate MMC-style spear-mace wind charge momentum resets smoothly in mid-air!
---
### Wind Charge Reset
---
**Wind Charge Reset** is a lightweight Fabric mod for Minecraft 1.21.11 that adds a clean,
responsive wind charge momentum reset mechanic for servers and single-player worlds.

When a player is falling fast enough and throws a wind charge downward, the mod instantly cancels their vertical velocity while preserving their horizontal momentum. This allows players to perform smooth mid-air fall resets without needing to wait for the wind charge to collide with a block or entity.

The mechanic is handled server-side, making it suitable for multiplayer servers where all players share the same behavior without needing to install the mod themselves.

---

**Features**
- Instant wind charge resets when thrown while falling
- Triggered by player velocity instead of projectile collision
- Preserves horizontal momentum
- Cancels vertical velocity
- Clears fall distance to prevent fall damage
- Server-side multiplayer support
- Works in single-player when installed locally
- Configurable minimum downward velocity
- Configurable pitch requirement
- Optional sound and particle effects
- Reloadable config with /windchargereset reload

---
**How It Works**

Vanilla wind charges normally apply their effect when the projectile collides with something. Wind Charge Reset changes that behavior for qualifying throws: if the player is falling at or above the configured downward velocity and is aiming downward, the reset happens immediately when the wind charge is thrown.

This makes the mechanic feel consistent, responsive, and better suited for movement-based gameplay, parkour, minigames, and PvP practice servers.

---

**Configuration**

A configuration file is generated automatically on first launch:


```
config/wind-charge-reset.yml
```


You can adjust values such as:


```
min-downward-velocity: 0.75
min-pitch: 75.0
max-pitch: 90.0
ground-bypass-distance: 2.5
```


Reload the configuration in-game with:

/windchargereset reload

---

**Compatibility**
- Minecraft: 1.21.11
- Loader: Fabric
- Requires: Fabric API
- Multiplayer: Install on the server only.
- Single-player: Install in your local mods folder.
