# World drawable assets (Vis-A / Vis-B)

Top-down RimWorld-style soft watercolor tiles and chibi pawns.
Vis-B: tiles are drawn **seamless** (gap=0); pawn placement uses continuous
`AgentVisual.displayX/Y` interpolation between hourly cells (no extra sprites).

| File | Use |
|------|-----|
| `tile_grass.png` | GRASS land |
| `tile_farm.png` | FARM land, pending = 0 |
| `tile_farm_ripe.png` | FARM land, pending > 0 |
| `tile_empty.png` | EMPTY / fallow |
| `tile_camp.png` | Center camp cell |
| `pawn_male_idle.png` | Male default (blue tunic) |
| `pawn_female_idle.png` | Female default (pink dress) |
| `pawn_*_work.png` | TILLING / HARVESTING (hoe pose) |
| `pawn_*_carry.png` | carriedFood > 0 (sack pose) |

Generated with Imagine; style: soft painted cozy, pastel greens/earths.
Pawns: transparent PNG 96×96 (no solid key color in final files).
Tiles resized to 128×128.

## Hotfix (Vis-B visual bugs)

| Bug | Cause | Fix |
|-----|--------|-----|
| Pink square behind character | `pawn_male_idle` shipped with solid hot-pink bg (not keyed) | Edge flood-fill chroma key → alpha |
| Gender flips on till/harvest | Idle files had **swapped gender art** vs `*_work` | Swap idle contents so `male_*`/`female_*` match; align female work/carry poses |

`WorldAssets.pawnFor` gender logic was already correct; only assets were wrong.
