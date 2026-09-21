# Just Gui Things

A technology-focused Minecraft Forge mod for **Minecraft 1.16.5**.

## Current milestone: Core Industrial

The project is building a reusable industrial foundation with broad Forge interoperability and no fixed machine tier ladder.

### Implemented

- Forge Energy storage and transport through standard capabilities
- Coal Generator, Basic Energy Cable and Energy Cell
- Item and fluid transport through Forge capabilities
- Basic Item Pipe, Basic Fluid Pipe, Fluid Reservoir and Fluid Pump
- Endpoint routing for item and fluid pipes with filters, priorities, redstone conditions and source reserves
- Shared conduit network/endpoint caching with loaded-chunk-safe topology scans, explicit per-face network segmentation and reactive cache invalidation
- Multipart conduit arms with enlarged connector collars for clearer endpoint connections and easier face selection
- Energy endpoint routing with consumer priority, source/target redstone control, fair distribution and Configurator-based per-face input/output modes
- Shared machine core for energy, inventory, side configuration and processing state
- Crusher with data-driven recipes and Speed/Efficiency/Buffer/Batch modules
- Coal Generator for early power and Steam Generator for water-fed intermediate generation
- Charging Station for capability-driven charging of Forge Energy items
- Industrial Mixer, Stamping Press and Resistive Furnace
- Rod Mill for tag-driven ingot-to-rod forming
- Wire Mill for tag-driven rod-to-wire processing
- Machine-specific components such as the Precision Roller Assembly and Tensioning Spindle
- Machine specialization through modules such as the Resistive Furnace Power Coil
- Configurator modes for per-face automation, active-machine redstone control and conduit-to-conduit connection toggling
- Data-driven recipes loaded through Minecraft's Recipe Manager
- Forge tag compatibility for shared dusts, ingots, plates, rods and wires
- Optional-mod recipes guarded by Forge conditions
- JEI categories for JGT processing recipes
- English and Brazilian Portuguese translations
- GitHub Actions build validation

### Progression direction

Machines do not progress through a Basic/Reinforced/Advanced/Elite ladder. Progression is based on production roles, machine-specific components, specialization modules, material forms, logistics and automation.

Ingots are treated as normal material forms. JGT does not use temperature states, pressure states or a tempered-ingot progression.

### Compatibility

Compatibility is architectural rather than based on mandatory mod-specific patches:

- Forge Energy via `IEnergyStorage`
- Item automation via `IItemHandler`
- Fluid automation via `IFluidHandler`
- Forge/Minecraft tags for shared materials
- Datapack-driven processing recipes
- Optional integration layers only when another mod exposes behavior that standard Forge APIs cannot represent

See [docs/COMPATIBILITY.md](docs/COMPATIBILITY.md) for extension points and soft-dependency rules.

### Next

- Add additional machine-specific specialization modules only where they create a distinct production tradeoff
- Continue production chains only when they add a clear industrial role
- Continue profiling large conduit networks and refine network diagnostics only where they expose a concrete automation bottleneck
- Improve machine/cable/pipe textures and models
- Continue broad compatibility through tags, capabilities and optional adapters

## Environment

- Minecraft 1.16.5
- Forge 36.2.42
- Java 8 target
- ForgeGradle 6
- Gradle 8.4 toolchain

Development work happens on feature branches and is merged through pull requests.
