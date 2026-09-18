# Compatibility and Addon API

Just Gui Things uses a layered compatibility model so common interoperability does not require hard Java dependencies between mods.

## 1. Data and Forge APIs first

Prefer these integration surfaces whenever possible:

- Forge Energy through `IEnergyStorage`
- item automation through `IItemHandler`
- fluid automation through `IFluidHandler`
- Forge material tags such as `forge:ingots/copper`, `forge:dusts/copper` and `forge:plates/copper`
- Minecraft/Forge recipe JSON and datapacks

This layer requires no JGT Java API dependency.

Crusher and Stamping Press results may use either a concrete item or a tag:

```json
{
  "type": "justguithings:crushing",
  "ingredient": { "tag": "forge:ores/copper" },
  "result": { "tag": "forge:dusts/copper", "count": 2 },
  "processing_time": 100,
  "energy_per_tick": 20
}
```

For a tag result, JGT first looks for a matching output from the same namespace as the actual input item. If none exists, it chooses a deterministic fallback by registry ID. JEI displays the available alternatives.

Quenching recipes accept either an exact `fluid` or a `fluid_tag`:

```json
{
  "type": "justguithings:quenching",
  "ingredient": { "tag": "forge:ingots/steel" },
  "fluid_tag": "minecraft:water",
  "fluid_amount": 250,
  "result": { "item": "justguithings:tempered_steel_ingot" },
  "processing_time": 160,
  "energy_per_tick": 45
}
```

## 2. Public Java addon API

Java addons should depend only on classes below `com.bulkcloud0.justguithings.api` when a data-only integration is not sufficient.

Supported extension contracts currently include:

- `api.machine.module.IMachineModule`
- `api.machine.module.MachineModuleTypes`
- `api.recipe.MachineProcessingRecipe`

A custom item can participate in an existing JGT module slot by implementing `IMachineModule` and returning one of the standard identifiers in `MachineModuleTypes`. It does not need to subclass a JGT item.

```java
public final class MySpeedModuleItem extends Item implements IMachineModule {
    public MySpeedModuleItem(Properties properties) {
        super(properties);
    }

    @Override
    public ResourceLocation getMachineModuleType() {
        return MachineModuleTypes.SPEED;
    }
}
```

Using the Java API is an explicit addon integration and therefore requires JGT to be available at runtime.

## 3. Internal packages

Packages such as `world.tile`, `world.container`, `client`, `registry`, and the machine implementation classes are implementation details. Addons should not depend on them unless no supported extension point exists.

When a real integration cannot be expressed with Forge capabilities, tags, recipes, or the public API, add a small optional adapter rather than placing mod-specific checks throughout the machine core.
