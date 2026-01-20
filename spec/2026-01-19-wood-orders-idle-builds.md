# Wood Orders + Idle Builds

## Context
- Chop jobs should only exist to satisfy wood orders.
- Wood orders exist while stockpiles accept wood (logs) and have space.
- Idle agents can build stockpiles or random structures if no other jobs exist.

## Requirements
- Generate chop-tree jobs only when there is demand for wood/logs.
- Demand is driven by stockpiles that accept wood/logs.
- Idle behavior: when no jobs exist, agents may queue a low-priority build job.
- New job types remain compatible with existing job execution flow.

## Plan
### Phase 1
- Add demand calculation and limit chop job creation.
- Ensure log resources can be hauled to stockpiles.

### Phase 2
- Add idle structure job generation + completion.
- Render/update UI mappings for new job types.

### Phase 3
- Tests and docs notes updates.

## Definition of done
- Chop jobs only spawn while wood/log stockpiles have open capacity.
- Logs can be hauled to stockpiles with threshold 1.
- Idle agents can create a low-priority build job for structures.
