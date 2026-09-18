# Compatibility Architecture

Just Gui Things treats compatibility as part of the core architecture. Integrations should prefer shared Forge/Minecraft contracts and remain optional whenever possible.

## Integration priority

Use the least coupled mechanism that solves the integration:

1. Forge capabilities for runtime I/O:
   - `IEnergyStorage`
   - `IItemHandler`
   - `IFluidHandler`
2. Forge or Minecraft tags for shared materials and compatible item/fluid groups.
3. Data-driven JGT recipe serializers for processing support.
4. The small JGT Java contracts only when tags and capabilities are not expressive enough.
5. Dedicated `integration/<modid>` code only for behavior that truly requires another mod's API.

Core machine classes must not directly depend on Thermal, Mekanism, AllTheOres or another optional mod.

## Machine modules

Built-in module types are identified by namespaced IDs:

- `justguithings:speed`
- `justguithings:efficiency`
- `justguithings:buffer`
- `justguithings:batch`

A compatible item can participate in either of two ways:

- implement `IMachineModule` and return the desired module type; or
- be added to one of the datapack tags below.

Supported module tags:

- `justguithings:machine_modules/speed`
- `justguithings:machine_modules/efficiency`
- `justguithings:machine_modules/buffer`
- `justguithings:machine_modules/batch`

The tag route is preferred for simple compatibility because it does not require a compile-time dependency on JGT.

Machines still decide which module types they support. Supplying a valid module item does not make an unsupported module slot appear on a machine.

## Materials and processing recipes

JGT registers its common materials in Forge tags where a shared convention exists, for example:

- `forge:dusts/iron`
- `forge:dusts/gold`
- `forge:dusts/coal`
- `forge:plates/iron`
- `forge:plates/gold`
- `forge:ingots/steel`
- `forge:ingots/tempered_steel`

Processing recipe inputs use Minecraft `Ingredient`, so item tags can be used directly in Crusher, Stamping Press and Industrial Mixer recipes.

Quenching recipes accept either an exact fluid ID or a fluid tag.

Exact fluid:

```json
{
  "fluid": "minecraft:water"
}
```

Fluid tag:

```json
{
  "fluid": {
    "tag": "minecraft:water"
  }
}
```

Fluid tags are the preferred form when multiple equivalent fluids should be accepted.

## Side capabilities

Side configuration is capability-specific:

- `INPUT` and `OUTPUT` expose item handlers.
- `FLUID_INPUT` exposes fluid input.
- `ENERGY` exposes energy input.
- `ENERGY_OUTPUT` exposes energy output.
- `ENERGY_BOTH` exposes bidirectional energy.

Each machine provides only the side modes it actually supports. Side configuration NBT is versioned so old saves can be migrated when semantics change.

## Optional-mod recipes

Recipes that reference items owned by another mod must be soft dependencies. Use Forge recipe conditions such as:

```json
"conditions": [
  {
    "type": "forge:mod_loaded",
    "modid": "examplemod"
  },
  {
    "type": "forge:item_exists",
    "item": "examplemod:example_item"
  }
]
```

Do not add an unconditional recipe whose output or required ingredient belongs to an optional mod.

## Compatibility rule

A missing optional mod must never prevent JGT from loading. Compatibility should add behavior when another mod is present, not change the validity of the JGT core when it is absent.
