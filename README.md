# lyricscord
> they were charging for a vibecoded mod so here you go :sob:

26.1 for now with some extra fixes

## reqs

- Minecraft `26.1`
- Fabric Loader `0.17.3` or newer
- Fabric API

## ins

1. install Fabric Loader for Minecraft `26.1`.
2. install Fabric API.
3. dl `lyricscord-2.1.0+mc26.1.jar` from the [Releases](../../releases) page.
4. put the JAR in your Minecraft `mods` folder.
5. launch Minecraft with the mod.

## hash since its not a virus

SHA-256 for `lyricscord-2.1.0+mc26.1.jar`:

```text
D94EA86FA4791B6AA644E0263C4D7EE4EE334C59DF2100D69572421FDED65D80
```

## building from source

you need Java `25` and Gradle `9.4.0`.

the versions used here are:

- Minecraft `26.1`
- Fabric Loader `0.18.4`
- Fabric API `0.145.1+26.1`
- Fabric Loom `1.15.3`

then run:

```powershell
git clone https://github.com/neo067982-star/neon-lyrics.git
cd neon-lyrics
gradle build
```

the finished JAR will be in `build/libs/`.

## License

The mod metadata lists the project license as MIT. Font assets include their own attribution/license files under `src/main/resources/assets/neonlyrics/font/`.
