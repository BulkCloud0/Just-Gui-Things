# Just Gui Things

A technology-focused Minecraft Forge mod for **Minecraft 1.16.5**.

## Current milestone: Core Industrial

The first milestone establishes reusable infrastructure for an industrial tech mod.

### Implemented

- Forge Energy-compatible internal storage
- DeferredRegister-based block, item, tile entity, container and recipe serializer registration
- Coal Generator with 100,000 FE buffer
- 40 FE/t generation from coal or charcoal
- Up to 200 FE/t automatic output to adjacent Forge Energy consumers
- Basic Energy Cable network with 500 FE/t transfer throughput
- Connected cable discovery and network-level distribution to adjacent consumers
- Persistent machine/cable energy and machine inventory state via NBT
- Forge item and energy capabilities
- Server-authoritative container/menu opening
- Synchronized machine GUI data
- Custom tech-style generator and Crusher screens
- Crusher with 100,000 FE buffer and two-slot processing inventory
- Data-driven Crusher recipes loaded from JSON/datapacks
- Per-recipe processing time and FE/t cost
- Initial ore doubling: Iron Ore -> 2 Iron Dust and Gold Ore -> 2 Gold Dust
- Utility crushing: Cobblestone -> Gravel -> Sand
- Iron Dust and Gold Dust smelting recipes
- Shift-click handling for machine input slots
- English and Brazilian Portuguese translations
- GitHub Actions build validation

### Crusher recipe format

Crusher recipes live under `data/<namespace>/recipes/` and use the `justguithings:crushing` serializer.

```json
{
  "type": "justguithings:crushing",
  "ingredient": { "item": "minecraft:iron_ore" },
  "result": { "item": "justguithings:iron_dust", "count": 2 },
  "processing_time": 100,
  "energy_per_tick": 20
}
```

Because the machine uses Minecraft's recipe manager, datapacks and other mods can provide additional Crusher recipes without changing Java code.

### Next

- JEI integration for Crusher recipes
- Machine tiers and upgrades
- Side configuration
- Energy storage blocks
- Additional processing machines and recipe types
- Fluids, heat/pressure and advanced industrial processing
- Proper custom cable/machine textures and models

## Environment

- Minecraft 1.16.5
- Forge 36.2.42
- Java 8 target
- ForgeGradle 6
- Gradle 8.4 toolchain

Development work happens on feature branches and is merged through pull requests.
