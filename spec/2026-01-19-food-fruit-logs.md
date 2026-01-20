# Food, Fruit, Logs

## Context
- User report: agents not eating; fruit should be visible and near trees.
- Scope: fruit/log resource updates and initialization scatter. Building/EDN DSL work deferred.

## Requirements
- Fruit exists as its own ground resource and is visible on tiles.
- New worlds guarantee some ground fruit at start, plus existing stockpile.
- Tree fruit drops onto nearby tiles (not only the tree tile).
- Chop-tree jobs replace trees with log drops on nearby tiles.
- Eat jobs target fruit items first, then fruit stockpiles.

## Plan
### Phase 1
- Adjust backend job generation and eat consumption to use fruit items/stockpiles.
- Update tree fruit drop and chop-tree completion to place nearby fruit/log items.
- Scatter initial fruit in `initial-world`.

### Phase 2
- Render fruit/log items in the canvas and add colors.
- Allow fruit stockpile selection in build UI.

### Phase 3
- Update tests and docs notes for the new resource behaviors.

## Definition of done
- Fruit/logs appear on the canvas in the simulation snapshot.
- Agents can eat fruit from the ground or fruit stockpiles.
- New worlds spawn with scattered fruit items and an initial fruit stockpile.
- Chop-tree jobs produce log items instead of wood.
- Tests and docs notes updated for the new resource names.
