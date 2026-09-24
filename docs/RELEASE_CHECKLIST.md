# Release checklist

This checklist defines the minimum gates for promoting `dev/core-industrial` to `main` and publishing a JGT release.

## Automated gates

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
- [ ] Post-merge `dev/core-industrial` CI is green.
- [ ] Final `main` push Build is green before release publication; the Release workflow enforces this for the exact current `main` SHA.
- [ ] The release commit is still the current `main` HEAD when publication starts.
- [ ] The Release workflow downloads the exact jar artifact produced by that successful `main` Build.
- [ ] A clean release build is byte-for-byte identical to the validated `main` CI artifact before publication.

## Manual in-game candidate verification

Use the exact jar produced by the final CI artifact in a clean Forge 36.2.42 profile. This is the same jar the Release workflow publishes after reproducibility verification.

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

## Release publication

1. Confirm `build.gradle` version is the intended release version.
2. Update `CHANGELOG.md` if the release scope changed.
3. Promote `dev/core-industrial` to `main` only after the gates above are satisfied.
4. Confirm the release candidate SHA is still the current `main` HEAD; the **Release** workflow rejects any other commit or tag and requires a successful push Build for that exact SHA.
5. From the GitHub Actions **Release** workflow on `main`, run the workflow with tag `v<build.gradle version>`.
6. Confirm the Release workflow downloads the exact final `main` artifact, reproduces it byte-for-byte with a clean build, and publishes that validated artifact.
7. Confirm the GitHub Release contains both the reobfuscated jar and its `.sha256` checksum.
8. Keep subsequent development on feature branches targeting `dev/core-industrial`.
