# CLAUDE.md

**Apocalypse Toolkit** (`apocalypse_toolkit`): a standalone NeoForge mod for Minecraft 26.2: guns and ammo,
throwables, field medicine, sentry turrets, and improvised survival gear. It replaces Gamingbarn's Guns in
LegendQuest: ZARP and ships on its own. **Read `docs/DESIGN.md` first**: Sable's decisions, the phase plan, and
everything ZARP must rewire when it switches.

- Build: `export JAVA_HOME=/home/sable/.gradle/jdks/eclipse_adoptium-25-amd64-linux.2; ./gradlew build`. Check the jar
  contains `META-INF/neoforge.mods.toml` (the template is `src/main/templates/META-INF/neoforge.mods.toml`, expanded
  from `gradle.properties`; a jar without it is not a mod).
- Art is our own and simple (Minecraft-styled); sounds are synthesised. Reuse nothing from Gamingbarn's Guns.
- Recipes ship vanilla-style; packs override them. Ship item tags so packs can gate and loot by tag.
- ZARP's pack glue (Threadwork, `../Threadwork`) and the pack (`../ZARP`) belong to the ZARP session: ask it, via
  ListAgents/SendMessage, for anything on the pack side.
- The LegendQuest ZARP instance carries `.sablecraft-no-deploy`: no jar goes there without Sable's say-so.
