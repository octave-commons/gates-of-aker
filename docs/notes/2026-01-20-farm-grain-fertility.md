# 2026-01-20 Farm Grain Fertility

- Added `:farm` job building that produces grain based on biome fertility at the farm tile.
- Farm yield uses `round(fertility * 3)` with a minimum of 1 grain per job completion.
- Biome fertility defaults: forest 0.55, field 0.9, rocky 0.15.
