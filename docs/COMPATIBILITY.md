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

Shift-right-clicking in the air switches scope. Normal right-clicking in the air cycles only the modes supported by the current scope. On a supported conduit face, normal right-click applies the selected edit mode while shift-right-click performs a read-only inspection of the current scope. Inspection reports the face I/O mode and the routing fields relevant to that resource without changing pipe/cable NBT. Item and fluid inspections also list configured filter sample registry IDs, with tagged samples marked as `[NBT]`. All resource inspections report the endpoint's current active/inactive state after applying its face I/O mode and redstone condition.

The controller also provides explicit `COPY_RULE` and `PASTE_RULE` modes. Each controller item keeps independent TARGET and SOURCE clipboards in its own item NBT. Copy/paste transfers only routing policy; conduit face I/O mode remains owned by the Configurator and is never copied. A clipboard can only be pasted back into the same resource family (item, fluid or energy), and incompatible pastes leave the destination unchanged. The controller tooltip shows the stored resource type for both TARGET and SOURCE clipboards so copied state remains visible while switching scopes and modes. The controller persists separate target/source modes in its own item NBT and reads the old single `RoutingMode` value as the target mode for compatibility.

### Target rules

Each destination face stores one `ItemRoutingTargetRule` with:

- priority: `HIGH`, `NORMAL`, or `LOW`;
- up to 9 filter samples;
- whitelist or blacklist behavior;
- exact-NBT or item-only matching;
- redstone condition: `ALWAYS`, `REQUIRE_SIGNAL`, or `REQUIRE_NO_SIGNAL`.

The network always tries HIGH targets before NORMAL and LOW targets. Round-robin fairness is preserved between targets at the same priority. If a higher-priority target is full, rejects the current item, or fails its filter/redstone rule, routing falls through to the next target and then lower priorities. If a destination accepts only part of the current per-tick item budget, the remaining budget continues through other eligible destinations in the same tick before falling to lower priorities.

### Source rules

Each pull-capable source face stores one `ItemRoutingSourceRule` with:

- up to 9 extraction filter samples;
- whitelist or blacklist behavior;
- exact-NBT or item-only matching;
- redstone condition;
- minimum stock presets: `0 -> 1 -> 8 -> 16 -> 32 -> 64 -> 0`.

The source filter is evaluated before extraction. Minimum stock is counted across the entire exposed `IItemHandler` for the candidate item identity, not only the current slot. When NBT matching is enabled, each item+NBT variant keeps its own reserve; when NBT matching is disabled, variants of the same item contribute to the same reserve.

Source redstone only enables or disables extraction from that endpoint. Target redstone only enables or disables insertion into that endpoint. Source endpoints are traversed round-robin; after a successful tick, the next scan starts after the last source that actually contributed, so empty or temporarily blocked sources do not distort fairness.

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

Minimum reserve is counted across all exposed tanks for the candidate fluid identity. With NBT matching enabled, tagged fluid variants reserve independently; with NBT matching disabled, the same fluid type shares one reserve. Fluid source endpoints are traversed round-robin, with the next scan starting after the last source that actually contributed.

If a destination accepts only part of the current per-tick fluid budget, the remaining budget continues through other eligible destinations in the same tick before falling to lower priorities.

The pipe's recovery buffer obeys destination filter and priority rules when retrying a partial transfer. This prevents buffered fluid from bypassing endpoint routing policy.

External blocks remain integrated only through Forge `IFluidHandler`.


## Energy-cable routing

Basic Energy Cables keep Forge `IEnergyStorage` as the only external energy integration contract.

Energy routing models consumer targets and source-side ingress separately. Producers inject FE through the cable's exposed `IEnergyStorage`, so source routing gates whether a sided cable endpoint accepts that injection. JGT intentionally does not implement an energy source reserve: a passive Forge Energy receiver cannot safely guarantee how much FE remains inside an arbitrary remote producer.

Each cable face also stores a conduit transfer mode. The Configurator cycles:

- `BOTH`: accepts FE from external producers and exposes output/extraction;
- `PULL`: displayed to players as `INPUT`, accepts external FE but is not considered a consumer-output face;
- `PUSH`: displayed as `OUTPUT`, participates in consumer routing and does not accept external FE;
- `DISABLED`: hides the sided energy capability and excludes that external endpoint.

Cable-to-cable adjacency remains part of the same internal network regardless of endpoint mode; the mode applies to the external connection on that face. Existing saves without `SideConfig` load every face as `BOTH`, preserving previous behavior.

Each cable face can store one `EnergyRoutingTargetRule` with:

- priority: `HIGH`, `NORMAL`, or `LOW`;
- redstone condition: `ALWAYS`, `REQUIRE_SIGNAL`, or `REQUIRE_NO_SIGNAL`.

The Routing Controller supports two energy endpoint operations. In `TARGET` scope, `PRIORITY` and `REDSTONE` configure consumers and require an OUTPUT-capable face. In `SOURCE` scope, `REDSTONE` gates FE ingress and requires an INPUT-capable face. Filter/NBT/minimum-stock modes do not apply to energy and report that explicitly instead of silently changing semantics.

Consumer discovery is keyed by block position plus exposed side, preserving compatibility with sided `IEnergyStorage` implementations. Distribution checks HIGH consumers before NORMAL and LOW consumers, while the round-robin cursor preserves fairness within active endpoints.

Each input-capable face also stores an `EnergyRoutingSourceRule` containing `ALWAYS`, `REQUIRE_SIGNAL`, or `REQUIRE_NO_SIGNAL`. The sided `IEnergyStorage` evaluates this condition dynamically when producers query or inject FE. Existing cables load target and source redstone rules as ALWAYS, preserving previous networks until configured.


### Energy fairness and transfer safety

Consumers at the same routing priority use max-min fair allocation within the 500 FE/t network budget. Small demands are satisfied first up to their requested amount, then remaining FE is redistributed evenly among consumers that still have demand. The round-robin cursor rotates ordering so integer rounding does not permanently favor the same endpoint.

Priority remains strict: HIGH consumes its fair allocation before leftover FE is offered to NORMAL, then LOW.

Energy delivery uses a reserve/commit/refund sequence:

1. simulate the consumer's acceptance;
2. drain that amount from cable network buffers;
3. execute the consumer insertion;
4. return any execution-time difference to cable buffers.

This prevents non-stable `IEnergyStorage` implementations from creating FE when simulation and execution disagree. Redistribution retries are bounded.


## Conduit topology caching

Item pipes, fluid pipes and energy cables share the same network-topology core.

The full connected conduit network is cached for 100 ticks and invalidated when conduit blocks are added, removed or the cached network is explicitly invalidated. External endpoint topology is cached separately for 20 ticks. When a conduit connection state changes because an adjacent endpoint appears, disappears or becomes visually connectable, JGT invalidates only the shared external-endpoint cache for that network; the full conduit BFS remains cached.

The external endpoint cache stores only:

- the owning conduit node;
- the conduit face;
- the neighboring block position;
- the neighboring exposed side.

It never caches `IItemHandler`, `IFluidHandler`, `IEnergyStorage`, `LazyOptional`, or a neighboring `TileEntity`. The live block entity and capability are resolved again during transfer. This reduces repeated 6-direction network scans without retaining capabilities after another mod invalidates or replaces them.

Per-face side modes, routing filters, priorities and redstone rules remain evaluated dynamically and are not part of the topology cache.
