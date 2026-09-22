# Just Gui Things

A technology-focused Minecraft Forge mod for **Minecraft 1.16.5**.

## Current milestone: Core Industrial

The project is building a reusable industrial foundation with broad Forge interoperability and no fixed machine tier ladder.

### Implemented

- Forge Energy storage and transport through standard capabilities
- Coal Generator, Basic Energy Cable and Energy Cell
- Item and fluid transport through Forge capabilities
- Basic Item Pipe, Basic Fluid Pipe, Fluid Reservoir, Fluid Pump and an 18-slot Item Buffer for configurable staging
- Vanilla comparator output for Energy Cell, Fluid Reservoir and Item Buffer fill levels
- Fluid Container Station for capability-driven filling and draining of Forge-compatible fluid containers
- Endpoint routing for item and fluid pipes with filters, priorities, redstone conditions, source reserves and target stock limits
- Shared conduit network/endpoint caching with loaded-chunk-safe topology scans, explicit per-face network segmentation and reactive cache invalidation
- Multipart conduit arms with enlarged connector collars for clearer endpoint connections and easier face selection
- Energy endpoint routing with consumer priority, source/target redstone control, fair distribution and Configurator-based per-face input/output modes
- Shared machine core for energy, inventory, side configuration, processing state and opt-in direct item auto-eject
- Crusher with data-driven primary/optional secondary outputs and Speed/Efficiency/Buffer/Batch modules
- Coal Generator for early power, Steam Generator for water-fed generation, and a datapack-driven Fluid Generator for liquid fuels
- Charging Station for capability-driven charging of Forge Energy items
- Industrial Mixer with backward-compatible item-only recipes plus optional fluid/tag inputs, Stamping Press with counted inputs for plates/gears, and Resistive Furnace
- Rod Mill for tag-driven ingot-to-rod forming
- Wire Mill for tag-driven rod-to-wire processing
- Industrial Assembler for data-driven automation of machine components, upgrades and repeatable infrastructure using 1-4 counted inputs
- Auto Crafter for locked 3x3 vanilla/mod crafting recipes with Forge item automation and container-item return handling
- Vacuum Collector for energy-backed collection of nearby dropped item entities into Forge item logistics
- Machine-specific components such as the Precision Roller Assembly and Tensioning Spindle
- Machine specialization through modules such as the Resistive Furnace Power Coil
- Configurator modes for per-face automation, active-machine redstone control, machine item auto-eject (including the Item Buffer) and conduit-to-conduit connection toggling
- Data-driven recipes loaded through Minecraft's Recipe Manager
- Forge tag compatibility for shared dusts, ingots, plates, gears, rods and wires
- Optional-mod recipes guarded by Forge conditions
- JEI categories for JGT processing recipes, including fluid-aware Industrial Mixer inputs
- English and Brazilian Portuguese translations
- GitHub Actions build validation

### Progression direction

Machines do not progress through a Basic/Reinforced/Advanced/Elite ladder. Progression is based on production roles, machine-specific components, specialization modules, material forms, logistics and automation.

Ingots are treated as normal material forms. JGT does not use temperature states, pressure states or a tempered-ingot progression.

### Compatibility

Compatibility is architectural rather than based on mandatory mod-specific patches:

- Forge Energy via `IEnergyStorage`
- Item automation via `IItemHandler`
- Fluid automation via `IFluidHandler`, including portable containers through `IFluidHandlerItem`
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
