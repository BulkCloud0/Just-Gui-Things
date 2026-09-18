# Compatibility Architecture

Just Gui Things treats compatibility as part of the core architecture. Integrations should prefer shared Forge/Minecraft contracts and remain optional whenever possible.

## Integration priority

Use the least coupled mechanism that solves the integration:

1. Forge capabilities for runtime I/O: `IEnergyStorage`, `IItemHandler`, and `IFluidHandler`.
2. Forge or Minecraft tags for shared materials and compatible groups.
3. Data-driven JGT recipe serializers for processing support.
4. Small JGT Java contracts only when tags and capabilities are not expressive enough.
5. Dedicated `integration/<modid>` code only for behavior that truly requires another mod's API.

Core machine classes must not directly depend on Thermal, Mekanism, AllTheOres or another optional mod.

## Machine modules

Built-in module types:

- `justguithings:speed`
- `justguithings:efficiency`
- `justguithings:buffer`
- `justguithings:batch`
- `justguithings:power_coil`

Supported module tags mirror those IDs under `justguithings:machine_modules/<type>`. A compatible item can implement `IMachineModule` or join exactly one module tag. Machines still decide which module types they support.

The Resistive Furnace supports one `power_coil` module. It doubles processing speed while increasing FE/t to 2.5x, making it a throughput-versus-energy tradeoff rather than a tier.

## Materials and processing recipes

JGT uses shared Forge tags where conventions exist, including dusts, plates, steel ingots, rods, and wires. Current built-in families include:

- `forge:dusts/iron`, `forge:dusts/gold`, `forge:dusts/coal`
- `forge:plates/iron`, `forge:plates/gold`
- `forge:ingots/steel`
- `forge:rods/iron`, `forge:rods/gold`, `forge:rods/steel`
- `forge:wires/iron`, `forge:wires/gold`, `forge:wires/steel`

Processing recipe inputs use Minecraft `Ingredient`. Tag-based outputs use `RecipeOutput`, allowing compatible families such as `forge:ingots/<metal>`, `forge:rods/<metal>`, and `forge:wires/<metal>`.

## Machine-specific components

Machine-specific components can be replaced through datapack tags without a Java dependency:

- Rod Mill: `justguithings:machine_components/roller_assemblies`
- Wire Mill: `justguithings:machine_components/tensioning_spindles`

The built-in components are `Precision Roller Assembly` and `Tensioning Spindle`.

## Side capabilities

- `INPUT` and `OUTPUT` expose item handlers.
- `FLUID_INPUT` and `FLUID_OUTPUT` expose directional fluid handlers.
- `ENERGY`, `ENERGY_OUTPUT`, and `ENERGY_BOTH` expose energy according to direction.

Each machine exposes only the side modes it supports. Side-configuration NBT remains versioned.

## Optional-mod recipes

Recipes for optional material families use Forge tags and conditions so absent materials do not create invalid recipes. Direct references to another mod's items must remain guarded by appropriate Forge recipe conditions.

A missing optional mod must never prevent JGT from loading.


## Item-pipe routing

Basic Item Pipes keep Forge `IItemHandler` as the only inventory integration contract. Routing metadata belongs to the pipe face, not to the external inventory.

The Routing Controller has four editing modes. Right-clicking in the air cycles the active mode:

- `Target Priority`: cycles `NORMAL -> HIGH -> LOW -> NORMAL`;
- `Filter Sample`: copies the item in the other hand as the endpoint sample, or clears the sample when the other hand is empty;
- `Whitelist / Blacklist`: toggles whether a matching sample is accepted or rejected;
- `NBT Matching`: toggles exact item+NBT comparison versus item-only comparison.

The controller stores its current editing mode in its own item NBT. Each pipe face stores its priority and filter rule independently.

A missing filter sample means the endpoint accepts all items regardless of whitelist/blacklist mode. Existing v1 routing NBT is migrated as an exact-NBT whitelist.

The network always tries HIGH targets before NORMAL and LOW targets. Round-robin fairness is preserved between targets at the same priority. If a higher-priority target is full, rejects the current item, or fails its filter rule, routing falls through to the next target and then lower priorities.

Filter evaluation is implemented by the reusable `ItemRouteFilter` value object rather than by machine- or inventory-specific checks. Connected inventories remain completely unaware of JGT routing rules.
