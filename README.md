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
3. dl `lyricscord 26.1.jar` from the [Releases](../../releases) page.
4. put the JAR in your Minecraft `mods` folder.
5. launch Minecraft with the mod.

## hash since its not a virus

SHA-256 for `lyricscord 26.1.jar`:

```text
2EC03F627AB627F9B028AEE84529A89F85C17B82C8B29BF076C57DE3D8BD0AEC
```

## building from source

you can't build this from source with one command yet because the repo does not have the Gradle project files (`build.gradle`, `gradlew`, or `gradle.properties`) in it yet.

once those files are added, the build command will be:

```powershell
git clone https://github.com/neo067982-star/neon-lyrics.git
cd neon-lyrics
.\gradlew.bat build
```

the finished JAR will be in `build/libs/`.

## License

The mod metadata lists the project license as MIT. Font assets include their own attribution/license files under `src/main/resources/assets/neonlyrics/font/`.
