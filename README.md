# lyricscord
> they were charging for a vibecoded mod so here you go :sob:

26.1, 26.1.1, 26.1.2, 26.2 and 26.3 with some extra fixes and features (almost everything u see is stock i just added a lyrics source)

## reqs

- Minecraft `26.1`, `26.1.1`, `26.1.2`, `26.2` or `26.3`
- Fabric Loader `0.20.1`
- Fabric API

## ins

1. install Fabric Loader for the Minecraft version you are using.
2. install Fabric API.
3. dl the matching version JAR from the [Releases](../../releases) page.
4. put the JAR in your Minecraft `mods` folder.
5. launch Minecraft with the mod.

## hash since its not a virus

SHA-256 checksums are included in `SHA256SUMS.txt` on each release.

for local builds, run:

```text
sha256sum lyricscord-*.jar
```

## building from source

you need Java `25`.

the versions used here are:

- 26.1: Gradle `9.4.0`, Loader `0.20.1`, Fabric API `0.145.1+26.1`, Loom `1.15.3`
- 26.1.1: Gradle `9.4.0`, Loader `0.20.1`, Fabric API `0.145.4+26.1.1`, Loom `1.15.3`
- 26.1.2: Gradle `9.4.0`, Loader `0.20.1`, Fabric API `0.155.3+26.1.2`, Loom `1.15.3`
- 26.2: Gradle `9.5.1`, Loader `0.20.1`, Fabric API `0.151.0+26.2`, Loom `1.17.1`
- 26.3: Gradle `9.8.0`, Loader `0.20.1`, Fabric API `0.162.0+26.3`, Loom `1.19-SNAPSHOT`

then run:

```powershell
git clone https://github.com/neo067982-star/neon-lyrics.git
cd neon-lyrics
gradle build
```

that builds the default 26.1 version. GitHub Actions builds all five versions automatically.

to build another version locally, override the version properties like this:

```powershell
gradle build `
  -Pminecraft_version=26.1.2 `
  -Ploader_version=0.20.1 `
  -Ploom_version=1.15.3 `
  '-Pfabric_api_version=0.155.3+26.1.2' `
  '-Pmod_version=2.1.1+mc26.1.2' `
  -Parchives_base_name=lyricscord-26.1.2
```

the finished JAR will be in `build/libs/`. GitHub Actions builds all five versions on pushes, pull requests, and `v*` release tags.

## License

The mod metadata lists the project license as MIT. Font assets include their own attribution/license files under `src/main/resources/assets/neonlyrics/font/`.
