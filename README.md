# 🐎✨ Equinox

## Legendary Mount System for Paper

**Equinox** transforms ordinary horses into powerful magical mounts with enchanted horse armor, mount binding, a magical whistle system, Pegasus-style flight, Bifrost-inspired sky pathways, and immersive particle effects.

Turn your favorite horse into your own **legendary companion**. 🪽✨

---

# ✨ Features

## 🐎 Legendary Mount Binding

Bind a horse as your personal **Equinox Mount**.

Once bound:

* Only the owner can use the mount's special abilities.
* The mount can be summoned using the Equinox Whistle.
* The mount remembers its permanent home location.
* The mount can be unbound when no longer needed.
* Ownership is protected from other players.

---

# 🔗 Mount Binding System

Bind your horse to become your personal Equinox Mount.

```text
/equinox mount bind
```

The location where the mount is bound becomes its **permanent home location**.

To remove the mount binding:

```text
/equinox mount unbind
```

---

# 📯 Equinox Whistle

The **Equinox Whistle** allows you to magically call your bound mount.

The whistle intelligently determines what should happen depending on your horse's location.

### 🐎 Horse is close

If your mount is within the configured return radius, using the whistle can send the mount back to its permanent home.

### 🏇 Horse is nearby

If the mount is within range, it physically runs toward its owner.

### ✨ Horse is far away

If the mount is far away or in another world, it can magically teleport near its owner.

### 🔗 Leashed Horse Protection

A leashed Equinox Mount will never be teleported.

This prevents leads from unexpectedly breaking.

### 🏠 Return Home

Your Equinox Mount can magically return to its permanent home location.

---

# 🪽 Pegasus Flight System

Equinox allows eligible mounts to become magical flying mounts.

Your horse can take flight when:

* You are riding your registered Equinox Mount.
* You are the owner of the mount.
* The mount is wearing Equinox Horse Armor.

---

## 🎮 Flight Controls

### 🪽 Take Off

While riding your Equinox Mount:

```text
SHIFT
```

Your mount will begin its magical flight.

### 🌎 Land / Descend

While flying:

```text
SHIFT
```

The mount will leave flight mode and begin descending.

### ⬆️ Fly Up

Look upward while flying.

### ➡️ Fly Forward

Look in the direction you want to travel.

### ⬇️ Fly Down

Look downward while flying.

---

# 🌈 Bifrost Sky Pathway

One of Equinox's signature effects is the magical **Bifrost-inspired flight pathway**.

While flying, magical particles appear in front of and below the horse, creating the illusion that the mount is:

> 🐎 Running across a magical bridge in the sky.

The pathway dynamically follows the direction of the flying mount.

---

# ✨ Magical Animations

Equinox includes multiple immersive visual effects.

## 🪽 Takeoff Effects

When your mount takes flight:

* Magical particles burst around the horse.
* Cloud effects create a powerful takeoff feeling.
* Enchanting particles surround the mount.
* Magical sounds play during takeoff.

---

## 🌈 Flight Effects

While flying:

* Magical particles follow the mount.
* Electric sparks create a mystical trail.
* The Bifrost pathway appears beneath and ahead of the horse.
* The effects dynamically follow the horse's movement.

---

## 🏇 Running Effects

Equinox Horse Armor can create special movement effects while the horse runs.

Depending on the configured armor enchantments, the horse can produce:

* ⚡ Electric sparks
* ✨ Magical movement particles
* 🏇 Enhanced running effects

Particles are intelligently triggered based on real movement rather than simply checking velocity.

---

## 🌎 Landing Effects

Landing creates magical visual effects and sounds around the mount.

---

# 🛡️ Equinox Horse Armor

Equinox includes a custom enchanted horse armor system.

The armor is used to unlock special mount abilities and magical effects.

The flight system requires the mount to wear valid **Equinox Armor**.

This prevents ordinary horses from gaining magical flight abilities.

---

# 📜 Commands

## Main Command

```text
/equinox
```

Alias:

```text
/eq
```

---

## 🐎 Mount Commands

### Bind Mount

```text
/equinox mount bind
```

Binds the horse you are using as your Equinox Mount.

### Unbind Mount

```text
/equinox mount unbind
```

Removes the Equinox Mount binding.

---

# 🔐 Permissions

Equinox supports a permission system for multiplayer servers.

## Player Permissions

### Use Equinox

```text
equinox.use
```

Allows players to use Equinox features and enchanted horse armor.

Default:

```text
true
```

---

### Bind Mounts

```text
equinox.mount.bind
```

Allows players to bind their horse as an Equinox Mount.

---

### Unbind Mounts

```text
equinox.mount.unbind
```

Allows players to unbind their Equinox Mount.

---

### Use Equinox Whistle

```text
equinox.whistle.use
```

Allows players to use the Equinox Whistle.

---

### Enchant Equinox Armor

```text
equinox.enchant
```

Allows players to use Equinox armor enchantment commands.

Default:

```text
op
```

---

## 👑 Administrator Permission

```text
equinox.admin
```

Provides full Equinox administration access.

Default:

```text
op
```

---

# ⚙️ Server Requirements

Equinox is designed for:

| Requirement            | Version                                               |
| ---------------------- | ----------------------------------------------------- |
| Minecraft Java Edition | 26.1.2                                                |
| Server Software        | Paper                                                 |
| Paper API              | 26.1.2                                                |
| Java                   | Compatible Java version required by your Paper server |

The plugin was developed and tested using:

```text
Paper 26.1.2
```

Example server version:

```text
Paper version 26.1.2-74
```

---

# 📦 Installation

## Step 1 — Download Equinox

Download the latest Equinox `.jar` file.

---

## Step 2 — Install the Plugin

Place the plugin inside your server's:

```text
plugins/
```

folder.

Example:

```text
Minecraft Server/
├── plugins/
│   └── Equinox.jar
│
├── world/
├── world_nether/
└── world_the_end/
```

---

## Step 3 — Start the Server

Start or restart your Paper server.

Equinox should automatically create its configuration files.

Your console should display something similar to:

```text
=================================
Equinox vVERSION
Legendary Mount System Enabled!
=================================
```

---

## Step 4 — Verify Installation

Run:

```text
/plugins
```

You should see:

```text
Equinox
```

listed as an enabled plugin.

---

# 🚀 Quick Start Guide

Follow these steps to start using Equinox.

---

## 🐎 Step 1 — Get a Horse

Find, tame, or summon a horse.

Make sure you are able to ride the horse.

---

## 🔗 Step 2 — Bind Your Mount

While using your horse, run:

```text
/equinox mount bind
```

Your horse is now registered as your personal Equinox Mount.

The binding location becomes the mount's permanent home.

---

## 🛡️ Step 3 — Equip Equinox Armor

Give your horse valid Equinox Horse Armor.

The armor unlocks magical abilities such as the flight system.

---

## 📯 Step 4 — Use the Equinox Whistle

Use the Equinox Whistle to interact with your bound mount.

Right-click with the whistle to call your mount.

Depending on distance and situation, the mount may:

* 🏇 Run toward you
* ✨ Teleport near you
* 🏠 Return home

---

## 🪽 Step 5 — Take Flight

Ride your Equinox Mount.

Press:

```text
SHIFT
```

Your mount will begin magical flight.

---

# 🏗️ How the Flight System Works

Equinox uses a plugin-based flight system.

The horse remains a normal Minecraft horse entity, while the plugin controls its movement using velocity and player direction.

During flight:

1. The player's viewing direction determines movement.
2. Looking upward causes ascent.
3. Looking downward causes descent.
4. The horse receives controlled velocity.
5. Fall damage is prevented during active flight.
6. Magical particles are generated around the mount.
7. The Bifrost pathway dynamically appears in the flight direction.

---

# 🛡️ Safety Features

Equinox includes several protections to prevent unwanted behavior.

### 🔗 Leashed Mount Protection

Leashed horses are not teleported.

### 💀 Death Cleanup

Flight data is removed when a horse dies.

### 🚪 Player Disconnect Cleanup

Flight mode is safely stopped when the rider disconnects.

### 🐎 Dismount Cleanup

Flight mode is removed when the player leaves the mount.

### 🧹 Plugin Shutdown Cleanup

Active flight data is safely cleared when the server disables the plugin.

### 🪂 Fall Protection

Flying mounts receive fall damage protection while actively flying.

### 👤 Ownership Protection

Players cannot use another player's registered Equinox Mount as their own.

---

# ⚡ Performance

Equinox is designed to avoid unnecessary particle and movement calculations.

The plugin:

* Checks movement using scheduled tasks.
* Tracks previous horse locations.
* Detects real movement.
* Only creates effects for eligible Equinox mounts.
* Cleans invalid or dead mounts from flight tracking.
* Uses particle intervals to reduce unnecessary particle spam.

For very large servers with hundreds of active horses, particle settings can be adjusted in the plugin configuration.

---

# 🧩 Plugin Architecture

The plugin is organized into multiple systems.

```text
com.istiak.equinox
│
├── EquinoxPlugin.java
│
├── commands/
│   └── EquinoxCommand.java
│
├── enchantments/
│   └── EnchantmentType.java
│
├── flight/
│   ├── FlightManager.java
│   └── FlightListener.java
│
├── items/
│   ├── HorseArmorManager.java
│   └── WhistleManager.java
│
├── listeners/
│   ├── HorseArmorListener.java
│   ├── HorseMovementListener.java
│   └── WhistleListener.java
│
└── mounts/
    ├── MountData.java
    ├── MountManager.java
    └── SummonManager.java
```

---

# 🗺️ Future Development

Equinox is designed to grow into a complete legendary mount system.

Planned and possible future features include:

## 🪽 3D Pegasus Wings

A dedicated resource pack system that adds:

* Animated 3D wings
* Wings attached directly to the horse
* Takeoff animations
* Flying animations
* Wing folding during landing
* Magical wing effects

---

## 🦄 Unicorn System

Potential features:

* Unicorn horn models
* Magical horn particles
* Special abilities
* Healing effects
* Teleportation abilities

---

## 🐉 More Legendary Mount Types

Possible future mounts:

* 🪽 Pegasus
* 🦄 Unicorn
* 🔥 Fire Horse
* ⚡ Storm Horse
* ❄️ Frost Horse
* 🌌 Celestial Mount
* 🌈 Bifrost Mount

---

## ✨ More Magical Abilities

Potential abilities include:

* Dash
* Double jump
* Magical teleportation
* Water walking
* Lightning abilities
* Special mount attacks
* Custom particle trails

---

# 🧑‍💻 Development

Equinox was created as a custom legendary mount system for Minecraft Paper servers.

The project focuses on creating immersive magical mounts while preserving normal Minecraft gameplay.

---

# 🐛 Bug Reports

If you discover a bug, please include the following information when reporting it:

* Minecraft version
* Paper version
* Equinox version
* Server console errors
* Steps to reproduce the problem
* Installed plugins that may conflict with Equinox

---

# 💡 Suggestions

Feature suggestions are welcome!

Ideas for:

* New mount abilities
* New magical effects
* New armor enchantments
* New mount types
* Flight improvements
* Resource pack models

are always appreciated.

---

# ❤️ Credits

**Developer:** Istiak Alam

**Project:** Equinox — Legendary Mount System

Built with ❤️ for Minecraft players who want their horses to become truly legendary.

---

# ⭐ Support the Project

If you enjoy **Equinox**, consider:

* ⭐ Starring the project on GitHub
* ❤️ Following the project on Modrinth
* 🐎 Sharing it with other Minecraft server owners
* 💬 Reporting bugs and suggesting new features

Your support helps Equinox continue to grow.

---

# 📄 License

Choose and add a license before publishing the project publicly.

Recommended options:

* MIT License — Open and permissive
* GPLv3 — Open source with stronger sharing requirements
* All Rights Reserved — Maximum control over redistribution

---

# 🐎✨ Equinox

### **Your horse is no longer just a horse.**

### **Bind it. Call it. Enchant it.**

### **Then take to the skies. 🪽✨**
