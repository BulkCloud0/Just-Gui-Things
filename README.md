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
- Energy Cell with 1,000,000 FE storage and 2,000 FE/t bidirectional I/O
- Persistent machine/cable energy and machine inventory state via NBT
- Forge item, fluid and energy capabilities with capability-specific side modes
- Server-authoritative container/menu opening
- Synchronized machine GUI data
- Custom tech-style generator, Crusher and Energy Cell screens
- Crusher with 100,000 FE buffer and data-driven processing
- Extensible machine modules identified by namespaced module types, with Speed, Efficiency, Buffer and Batch support where applicable
- No fixed machine tier ladder: progression is driven by machine-specific modules, components and future specialization systems
- Configurator tool for per-face machine automation modes
- Versioned side configuration with item input/output, fluid input, energy input/output/bidirectional modes as supported by each machine
- Backward-compatible side configuration migration for older machine saves
- Data-driven Crusher, Stamping Press, Industrial Mixer and Quench Chamber recipes loaded from JSON/datapacks
- Per-recipe processing time and FE/t cost
- Forge-tag-based metal processing compatibility, including tag-based recipe outputs and deterministic output selection
- Utility crushing: Cobblestone -> Gravel -> Sand
- Iron Dust and Gold Dust smelting recipes
- Shift-click handling for machine input/module slots
- English and Brazilian Portuguese translations
- Fluid-tag-aware quenching recipes
- Public addon API contracts for machine modules and processing recipe metadata
- GitHub Actions build validation

### Crusher recipe format

Crusher recipes live under `data/<namespace>/recipes/` and use the `justguithings:crushing` serializer.

```json
{
  "type": "justguithings:crushing",
  "ingredient": { "tag": "forge:ores/copper" },
  "result": { "tag": "forge:dusts/copper", "count": 2 },
  "processing_time": 100,
  "energy_per_tick": 20
}
```

Results may use either `item` or `tag`. Tag outputs prefer an item from the same namespace as the actual input and otherwise use a deterministic registry-ID fallback. Because processing uses Minecraft's recipe manager, datapacks and other mods can extend JGT without changing Java code.

See [Compatibility and Addon API](docs/compatibility.md) for the supported integration surfaces.

### Progression direction

Machines do not progress through a Basic/Reinforced/Advanced/Elite ladder. Instead, each machine has its own base characteristics and evolves through modules, better components, specialized processing paths and later industrial systems such as fluids, heat and pressure.

### Next

- Add automated recipe/datapack and capability regression coverage
- Add optional mod-specific adapters only where Forge capabilities and tags are insufficient
- Expand item and fluid logistics for automated factory lines
- Improve cable network caching/performance and connection visuals
- Add further processing systems only when they have a distinct progression role
- Proper custom cable/machine textures and models

## Environment

- Minecraft 1.16.5
- Forge 36.2.42
- Java 8 target
- ForgeGradle 6
- Gradle 8.4 toolchain

Development work happens on feature branches and is merged through pull requests.
