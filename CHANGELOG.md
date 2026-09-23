# Changelog

## 0.1.0-alpha

Initial Core Industrial alpha milestone.

### Industrial systems
- Forge Energy generation, storage and transport with Coal, Steam and Fluid generators, Energy Cell and Basic Energy Cable.
- Forge-capability item/fluid logistics with Basic Item Pipe, Basic Fluid Pipe, Fluid Reservoir, Fluid Pump, Fluid Container Station, Item Buffer and routing controls.
- Processing and automation machines including Crusher, Stamping Press, Industrial Mixer, Industrial Washer, Industrial Sawmill, Resistive Furnace, Rod Mill, Wire Mill, Industrial Assembler, Auto Crafter, Vacuum Collector and Industrial Separator.
- Speed, Efficiency, Buffer, Batch and machine-specific specialization modules without a fixed machine tier ladder.
- Configurator-driven side I/O, redstone control, auto-eject and conduit segmentation.

### Compatibility
- Datapack-driven machine recipes and fuels.
- Forge/Minecraft tags for interoperable material forms.
- Optional-mod boundaries that keep core code free of hard Thermal, Mekanism and AllTheOres API dependencies.
- Optional JEI categories for JGT processing recipes.
- English and Brazilian Portuguese localization.

### Reliability
- Deterministic recipe precedence.
- Physical item/fluid/energy recipe limits and bundled-recipe CI guards.
- Output stack-capacity preflight and overflow-safe processing scaling.
- Safe sided-capability recreation after world reload.
- Defensive recipe network validation.
- Shared conduit caches with loaded-chunk-safe topology handling.
- Unit coverage for processing scaling and fair-share energy allocation.
- Reproducible archive settings, Gradle Wrapper 8.4, runtime startup smoke tests and release artifact automation.
