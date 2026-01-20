# Harvest Buildings

## Context
- Resource harvest buildings (wood, fruit, grain, stone) act as both worker and stockpile.
- Jobs are specific to each building type.
- Deliver-food jobs should be bounded (one per stockpile).

## Requirements
- Add four harvest buildings: lumberyard, orchard, granary, quarry.
- Each building creates/owns jobs to gather its matching resource.
- Buildings act as stockpiles for their resource (raw only).
- Auto-build harvest buildings near campfire when demand exists.
- Deliver-food jobs capped to one per stockpile.

## Plan
### Phase 1
- Add building definitions + stockpile/resource handling for harvest buildings.
- Add job generation bound to building targets.
- Limit deliver-food jobs to stockpile demand (per stockpile).

### Phase 2
- Auto-build harvest buildings when demand exists.
- Update UI render/labels/colors for new structures/jobs.

### Phase 3
- Tests + docs notes.

## Definition of done
- Harvest buildings present and acting as stockpiles.
- Jobs per building target only when demand exists.
- Deliver-food jobs stay bounded to stockpile count.
