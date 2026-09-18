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

### Routing Controller scopes

The Routing Controller has two editing scopes:

- `TARGET`: rules applied when the face is used as a push destination;
- `SOURCE`: rules applied when the face is used as a pull source.

Shift-right-clicking in the air switches scope. Normal right-clicking in the air cycles only the modes supported by the current scope. The controller persists separate target/source modes in its own item NBT and reads the old single `RoutingMode` value as the target mode for compatibility.

### Target rules

Each destination face stores one `ItemRoutingTargetRule` with:

- priority: `HIGH`, `NORMAL`, or `LOW`;
- up to 9 filter samples;
- whitelist or blacklist behavior;
- exact-NBT or item-only matching;
- redstone condition: `ALWAYS`, `REQUIRE_SIGNAL`, or `REQUIRE_NO_SIGNAL`.

The network always tries HIGH targets before NORMAL and LOW targets. Round-robin fairness is preserved between targets at the same priority. If a higher-priority target is full, rejects the current item, or fails its filter/redstone rule, routing falls through to the next target and then lower priorities.

### Source rules

Each pull-capable source face stores one `ItemRoutingSourceRule` with:

- up to 9 extraction filter samples;
- whitelist or blacklist behavior;
- exact-NBT or item-only matching;
- redstone condition;
- minimum stock presets: `0 -> 1 -> 8 -> 16 -> 32 -> 64 -> 0`.

The source filter is evaluated before extraction. Minimum stock is counted across the entire exposed `IItemHandler` for the candidate item identity, not only the current slot. When NBT matching is enabled, each item+NBT variant keeps its own reserve; when NBT matching is disabled, variants of the same item contribute to the same reserve.

Source redstone only enables or disables extraction from that endpoint. Target redstone only enables or disables insertion into that endpoint.

### Filter persistence

An empty filter list accepts all items regardless of whitelist/blacklist mode. Matching succeeds when any configured sample matches according to the NBT rule.

Existing routing data remains loadable:

- legacy `Filter*` data migrates to a target filter list;
- older `Rule*` data migrates to `TargetRule*`;
- current source rules are stored independently as `SourceRule*`.

Filter evaluation remains a JGT-side policy layer. Connected inventories never need JGT-specific interfaces or integration code.


## Fluid-pipe routing

Basic Fluid Pipes reuse the same resource-neutral routing primitives as item pipes: `RoutingPriority`, `RoutingFilterMode`, `RoutingFilterSampleChange` and `RoutingRedstoneMode`.

The existing Routing Controller configures fluid endpoints with the same TARGET/SOURCE scopes:

- target: priority, up to 9 fluid samples, whitelist/blacklist, NBT matching and redstone condition;
- source: up to 9 fluid samples, whitelist/blacklist, NBT matching, redstone condition and minimum fluid reserve;
- fluid reserve presets are `0 -> 250 -> 1000 -> 4000 -> 8000 -> 16000 mB`.

Fluid samples are resolved from the item in the other hand. JGT first queries Forge `IFluidHandlerItem`, so buckets and compatible portable tanks from other mods work without adapters. A fallback reads JGT's portable reservoir `BlockEntityTag/Tank` data.

Minimum reserve is counted across all exposed tanks for the candidate fluid identity. With NBT matching enabled, tagged fluid variants reserve independently; with NBT matching disabled, the same fluid type shares one reserve.

The pipe's recovery buffer obeys destination filter and priority rules when retrying a partial transfer. This prevents buffered fluid from bypassing endpoint routing policy.

External blocks remain integrated only through Forge `IFluidHandler`.
