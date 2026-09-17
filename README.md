# Just Gui Things

A technology-focused Minecraft Forge mod for **Minecraft 1.16.5**.

## Current milestone: Core Industrial

The first milestone establishes the reusable infrastructure for an industrial tech mod.

### Implemented

- Forge Energy-compatible internal storage
- DeferredRegister-based block, item, tile entity and container registration
- Coal Generator with 100,000 FE buffer
- 40 FE/t generation from coal or charcoal
- Up to 200 FE/t automatic output to adjacent Forge Energy consumers
- Persistent machine inventory, energy and burn state via NBT
- Forge item and energy capabilities
- Server-authoritative container/menu opening
- Synchronized burn and energy data for the machine GUI
- Custom tech-style generator screen with fuel and FE indicators
- Shift-click handling for the fuel slot
- English and Brazilian Portuguese translations
- GitHub Actions build validation

### Next

- Crusher and machine processing architecture
- Custom processing recipes
- Energy cables
- Machine tiers and upgrades
- Side configuration
- Fluids and advanced industrial processing

## Environment

- Minecraft 1.16.5
- Forge 36.2.42
- Java 8 target
- ForgeGradle 6
- Gradle 8.4 toolchain

Development work happens on feature branches and is merged through pull requests.
