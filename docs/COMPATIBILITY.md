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

Supported module tags mirror those IDs under `justguithings:machine_modules/<type>`. Data-only integrations should prefer those tags. A Java addon that needs an explicit code contract may implement `com.bulkcloud0.justguithings.api.machine.module.IMachineModule`; the standard identifiers are exposed by `com.bulkcloud0.justguithings.api.machine.module.MachineModuleTypes`. Machines still decide which module types they support.

Only contracts below `com.bulkcloud0.justguithings.api` are intended as stable Java addon surfaces. The older `machine.module.IMachineModule` and `machine.module.MachineModuleTypes` names remain as deprecated source/binary compatibility bridges, not as the preferred API. Processing recipes remain datapack-first; JGT recipe implementation classes are not currently part of the public Java API.

The Resistive Furnace supports one `power_coil` module. It doubles processing speed while increasing FE/t to 2.5x, making it a throughput-versus-energy tradeoff rather than a tier.

## Materials and processing recipes

JGT uses shared Forge tags where conventions exist, including ores, dusts, ingots, plates, rods, and wires. Coal-like generator fuels use the vanilla `minecraft:coals` item tag, so compatible mods can extend generator fuel support without a Java dependency. JGT-owned material forms currently contribute to:

- `forge:dusts/iron`, `forge:dusts/gold`, `forge:dusts/coal`
- `forge:plates/iron`, `forge:plates/gold`
- `forge:ingots/steel`
- `forge:rods/iron`, `forge:rods/gold`, `forge:rods/steel`
- `forge:wires/iron`, `forge:wires/gold`, `forge:wires/steel`

The generic compatibility datapack under `data/justguithings/recipes/compat/common` adds processing only when the required Forge tags are populated. Current common metal families include aluminum, copper, iridium, lead, nickel, osmium, platinum, silver, tin, uranium, and zinc. Steel is supported where compatible forms exist. Common alloy processing also covers bronze, constantan, electrum, invar, signalum, lumium, and enderium for the dust/ingot/plate forms exposed through Forge tags.

Processing recipe inputs use Minecraft `Ingredient`. Tag-based outputs use `RecipeOutput`. When resolving a tagged output, JGT first prefers a compatible result from the same registry namespace as the input when one exists; otherwise candidates are ordered deterministically by registry name. This keeps recipes data-driven while avoiding dependence on a particular material provider. Industrial Mixer recipes may set optional `primary_count` and `secondary_count` fields (default `1`) to consume multiple items from either input. Tagged Mixer outputs use the primary input as the namespace preference.

## Machine-specific components

Machine-specific components can be replaced through datapack tags without a Java dependency:

- Rod Mill: `justguithings:machine_components/roller_assemblies`
- Wire Mill: `justguithings:machine_components/tensioning_spindles`

The built-in components are `Precision Roller Assembly` and `Tensioning Spindle`.

## Side capabilities

- `INPUT` and `OUTPUT` expose item handlers.
- `FLUID_INPUT` and `FLUID_OUTPUT` expose directional fluid handlers.
- `ENERGY`, `ENERGY_OUTPUT`, and `ENERGY_BOTH` expose energy according to direction.
- `ITEM_INPUT_ENERGY_OUTPUT` is an explicit composite side mode used by the Coal Generator: the face accepts valid item fuel and exports energy.

Active machine-side energy export is loaded-chunk safe: Energy Cells and Coal Generators skip neighboring positions whose chunks are not currently loaded instead of probing their TileEntities. This avoids passive chunk loading from machine ticks while preserving normal capability discovery for loaded neighbors.

Owned JGT TileEntity capabilities follow the Forge lifecycle: custom `LazyOptional` handles are invalidated from `invalidateCaps()`, covering both block removal and chunk unload. This prevents external consumers from retaining apparently-valid handlers after the provider leaves the loaded world.

For side-configurable block capabilities, `side == null` does not represent a physical face and JGT intentionally returns an empty capability instead of exposing the full backing storage. This applies to the item/energy views inherited from `BaseMachineTileEntity`, the Basic Energy Cable energy view, and the Fluid Pump fluid view. Internal GUI and machine logic use their owned storage/handlers directly rather than relying on an unsided capability. JGT neighbor automation and conduit routing always query the concrete opposite face. The non-side-configurable Fluid Reservoir remains intentionally omnidirectional and exposes the same `IFluidHandler` both sided and unsided for generic Forge interoperability.

The portable Energy Cell item exposes Forge `IEnergyStorage` directly. Its item capability reads and writes the same `BlockEntityTag/Energy` value used by the placed block, keeps the block's 1,000,000 FE capacity and 2,000 FE per-call receive/extract limit, and therefore remains compatible with generic Forge Energy chargers and inventory automation without a JGT-specific adapter.

When a configurable face changes mode at runtime, JGT replaces that face's owned sided `LazyOptional` views, invalidates the previous handles, and notifies block neighbors on the server. Standard external automation can therefore drop cached capability references and rediscover newly enabled/disabled I/O without requiring a block replacement.

Machine FE extraction with a non-zero output limit shares one per-game-tick budget across active push logic and sided capability extraction. Simulations do not consume the budget, and active-push refunds restore budget when an executed receiver accepts less than simulated. This makes Coal Generator `MAX_OUTPUT_PER_TICK` and Energy Cell outbound transfer limits aggregate rather than per-call.

Active machine energy push is also fair across eligible loaded neighbors: the shared machine helper simulates each receiver's demand and uses the same resource-neutral `FairShareAllocator` as conduit energy routing. Energy Cells can chain directly when the configured faces establish a one-way path: Output -> Input/Both and Both -> Input are allowed. A direct `ENERGY_BOTH` <-> `ENERGY_BOTH` connection between two Energy Cells is skipped to prevent cell-to-cell ping-pong. Other receivers, including Energy Cells with directional faces and third-party Forge Energy blocks, use the normal concrete-face capability path. Partial execute acceptance is refunded and can be redistributed in bounded follow-up rounds.

Each machine exposes only the side modes it supports. Side-configuration NBT remains versioned. Side-config version 5 makes the Coal Generator's legacy combined behavior explicit: generator faces saved as `ENERGY_OUTPUT` under older versions migrate to `ITEM_INPUT_ENERGY_OUTPUT`, preserving their previous ability to accept coal/charcoal while exporting FE. New `ENERGY_OUTPUT` faces are output-only. The Configurator presents localized player-facing names for those modes: Item Input/Output, Fluid Input/Output, Energy Input/Output/Input + Output, and Disabled. Enum values and persisted ordinals remain internal implementation details.

Side-mode edits are explicitly synchronized to tracking clients through the standard TileEntity update-packet path. Machines and all three conduit families send that update only when `cycleSideMode()` runs, so the Configurator HUD/world overlay reflects the new mode without adding per-tick synchronization traffic.

Normal Configurator right-click continues to cycle the supported mode for the clicked face. Shift-right-click is read-only inspection: on machines, item pipes, fluid pipes and energy cables it reports the current localized I/O mode in chat without changing side configuration. Disabled conduit faces remain inspectable for diagnostics. Face directions are also localized through shared translatable components, so conduit/machine feedback no longer exposes raw NORTH/SOUTH/EAST/WEST/UP/DOWN enum names. Conduit inspection additionally reports Connected/Disconnected from the current multipart blockstate arm. This is a visual/physical connection indicator only; it does not imply that routing, redstone or filters currently allow transfer. While the Configurator is held and no GUI is open, the currently targeted configurable face receives a client-only world-space outline derived from its current side mode. The overlay reads the local TileEntity state only and adds no packets, NBT or blockstate properties. World overlays and machine side-config screens share a single client-side color palette so the same mode keeps the same visual identity. The HUD also shows the same localized target description used by Shift-inspection while the Configurator is aimed at a supported face, so I/O direction and resource type remain readable without relying on color alone. Compact machine side-config direction/mode codes are localized as well; the grid measures translated tokens before laying out columns so wider locale-specific abbreviations do not overlap.

## Optional-mod recipes

Compatibility data is split by coupling level:

- `recipes/compat/common`: recipes expressed entirely through shared Minecraft/Forge tags. These must not reference a specific optional mod ID or item ID. Each recipe must guard every required optional input/output family with Forge conditions such as `forge:tag_empty`.
- `recipes/compat/<modid>`: reserved for datapack integration that genuinely needs another mod's registry IDs or recipe conventions. Direct references must be guarded with `forge:mod_loaded` and any relevant tag/availability conditions.
- `integration/<modid>`: reserved for Java adapters only when tags, recipes, and standard Forge capabilities cannot represent the required behavior. Core machine classes must not import optional-mod APIs.

Thermal, Mekanism, AllTheOres, and similar material providers should therefore work through `compat/common` whenever they publish standard Forge tags. A dedicated integration layer should be added only for behavior that cannot be expressed through those shared contracts.

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

Fluid samples are resolved from the item in the other hand. JGT first queries Forge `IFluidHandlerItem`, so buckets and compatible portable tanks from other mods work without adapters. The portable JGT Fluid Reservoir now exposes that standard item capability directly, backed by the same `BlockEntityTag/Tank` data used when the block is placed. The NBT fallback remains for compatibility with legacy stacks or environments where an item capability is unavailable.

Minimum reserve is counted across all exposed tanks for the candidate fluid identity. With NBT matching enabled, tagged fluid variants reserve independently; with NBT matching disabled, the same fluid type shares one reserve. Fluid source endpoints are traversed round-robin, with the next scan starting after the last source that actually contributed.

If a destination accepts only part of the current per-tick fluid budget, the remaining budget continues through other eligible destinations in the same tick before falling to lower priorities.

The pipe's recovery buffer obeys destination filter and priority rules when retrying a partial transfer. This prevents buffered fluid from bypassing endpoint routing policy.

External blocks remain integrated only through Forge `IFluidHandler`.

The Configurator uses the same resource-neutral conduit transfer labels for item and fluid pipes: `Pull + Push`, `Pull`, `Push`, and `Disabled` (localized in-game). Energy cables keep their player-facing `Input + Output`, `Input`, `Output`, and `Disabled` terminology because passive Forge Energy ingress/consumer output semantics differ from active item/fluid extraction and insertion. That energy-specific display mapping is centralized in `ConduitTransferMode`, so Configurator feedback, Configurator inspection and Routing Controller inspection cannot drift apart.


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
