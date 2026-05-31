# FireFight-OG

WorldGuard flag based fire fighting and fluid mechanics for combat regions. Maintained for Purpur 1.19.4 by [TrueOG Network](https://trueog.net)

## Build

```
./gradlew clean build eclipse --warning-mode all
```

Output: `build/libs/*.jar`. Config changes require a restart.

## Features

* **`fire-extinguish`** — punch out fire/soul fire in flagged regions, even when the `build` flag is denied.
* **`temporary-fluids`** — place water/lava buckets in flagged regions, even when the `build` flag is denied. Sources auto-remove after `fluid-lifetime-seconds` (or when picked back up). Water/lava cannot form obsidian, cobblestone, stone, nor basalt.

## Enable per region:

```
/rg flag <region> fire-extinguish allow
/rg flag <region> temporary-fluids allow
```
