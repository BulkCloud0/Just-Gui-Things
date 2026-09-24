# Release checklist

This checklist defines the minimum gates for promoting `dev/core-industrial` to `main` and publishing a JGT release.

## Pre-promotion automated candidate gates

These gates apply to the release-candidate SHA on `dev/core-industrial` before the promotion PR is merged.

- [ ] Feature/release-readiness PR CI is green.
- [ ] `./gradlew build --stacktrace --no-daemon` passes through the pinned Gradle Wrapper.
- [ ] JUnit tests pass.
- [ ] `validateCompatRecipes` passes.
- [ ] `validateRecipePhysicalLimits` passes.
- [ ] `validateOptionalIntegrationBoundaries` passes.
- [ ] `validateClientBoundaries` passes.
- [ ] `validateRetiredIndustrialConcepts` passes.
- [ ] The reobfuscated jar is uploaded as a GitHub Actions artifact.
- [ ] Dedicated server startup reaches the Minecraft ready state without JEI.
- [ ] Client startup reaches the resource-atlas-ready state with JEI.
- [ ] Client startup reaches the resource-atlas-ready state without JEI.
- [ ] Post-merge `dev/core-industrial` push CI is green for the exact candidate SHA.

## Manual in-game candidate verification

Use the exact jar produced by the successful `dev/core-industrial` push Build for the release-candidate SHA in a clean Forge 36.2.42 profile. This is the candidate jar that must be manually validated before promotion.

- [ ] Start Minecraft 1.16.5 with JGT only and create/open a world.
- [ ] Start Minecraft with JGT + JEI and confirm JGT JEI categories/recipes appear.
- [ ] Build one complete line: generator -> energy cable -> processing machine -> item pipe -> storage.
- [ ] Verify a fluid path using reservoir/pump/container station or a fluid-aware machine.
- [ ] Verify Configurator side I/O, redstone, auto-eject and conduit connection toggles.
- [ ] Verify Speed/Efficiency and at least one specialized module.
- [ ] Save/quit/reopen and confirm inventories, FE, fluids, modules and side configuration persist.
- [ ] Unload/reload chunks containing conduit networks and confirm routing resumes.
- [ ] Run `/reload` and confirm datapack recipes remain usable.
- [ ] Join a dedicated server running the same JGT jar and exercise at least one powered processing line.
- [ ] Check the client/server logs for JGT errors, missing registry entries, classloading failures and repeated severe warnings.

## Promotion gates

Complete these only after the automated candidate gates and manual candidate verification above are satisfied.

- [ ] Confirm `build.gradle` version is the intended release version.
- [ ] Confirm `CHANGELOG.md` describes the intended release scope.
- [ ] Reconfirm the exact heads of `dev/core-industrial`, `main` and the promotion PR.
- [ ] Review the final `main...dev/core-industrial` diff.
- [ ] Mark the promotion PR ready for review.
- [ ] Merge `dev/core-industrial` to `main` only as an explicit release action.
- [ ] Final `main` push Build is green for the exact promoted SHA, including runtime smoke tests and artifact upload.

## Release publication gates

The final `main` artifact is produced after promotion. The Release workflow publishes that exact validated artifact; it does not publish the pre-promotion `dev/core-industrial` artifact.

- [ ] The release commit is still the current `main` HEAD when publication starts.
- [ ] The Release workflow resolves a successful push-triggered Build for that exact `main` SHA.
- [ ] The Release workflow downloads the exact jar artifact produced by that successful `main` Build.
- [ ] A clean release build is byte-for-byte identical to the validated `main` CI artifact before publication.
- [ ] Run the **Release** workflow from `main` with tag `v<build.gradle version>`.
- [ ] Confirm the GitHub Release contains both the reobfuscated jar and its `.sha256` checksum.
- [ ] Keep subsequent development on feature branches targeting `dev/core-industrial`.
