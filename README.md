# FireFight-OG

WorldGuard flag based fire, fluid, and cobweb mechanics for combat regions. Maintained for Purpur 1.19.4 by [TrueOG Network](https://trueog.net)

## Build

```
./gradlew clean build eclipse --warning-mode all
```

Output: `build/libs/*.jar`. Config changes require a restart.

## Features

FireFight distinguishes **world** blocks (placed by builders in creative mode with WorldGuard bypass, or pre-existing/natural) from **FireFight-temporary** blocks (placed by survival players inside flagged regions). Only the temporary blocks are mutable by ordinary players; world blocks stay protected by WorldGuard exactly as before.

* **`fire-extinguish`** — survival players can ignite fire/soul fire with flint-and-steel or fire charges in flagged regions, and can punch out fire they (or other players) ignited. Builder/natural fire stays immutable.
* **`temporary-fluids`** — survival players can place water/lava buckets in flagged regions; placed sources and their flow auto-remove after `fluid-lifetime-seconds` (or when picked back up). Bucket-fill on natural fluid is denied. Water/lava cannot form obsidian, cobblestone, stone, or basalt. Builder fluid stays permanent.
* **`temporary-cobwebs`** — survival players can place cobwebs in flagged regions; placed cobwebs auto-remove after `fluid-lifetime-seconds` and drop one piece of string on expiry. Survival players can also break their own tracked cobwebs early (vanilla drops). Builder cobwebs stay immutable.

## Enable per region

```
/rg flag <region> fire-extinguish allow
/rg flag <region> temporary-fluids allow
/rg flag <region> temporary-cobwebs allow
```

## Config

```yaml
fire-extinguish: true
temporary-fluids: true
temporary-cobwebs: true
fluid-lifetime-seconds: 60
```

The lifetime value is shared across all three modules.
