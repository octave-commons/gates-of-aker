---
description:"Hack"
---
read @HACK.md
review @spec/ and @docs/planning

# Clojure backend status

## Recent logs
!`pm2 logs gates-backend --nostream --lines 50`

## test results
!`cd backend/ && clojure -X:test`

## clj-kondo lint backend
!`cd backend && clojure -X:lint`


# Vite frontend status
## type checks
!`cd web/ && npm run typecheck`
## Test results
!`cd web/ && npm run test`

# Open Isseus
!`gh issues list`

# Open PR
!`gh pr list`


Where are we at in our project?
Which milestone are we working on?
How much is left?
Are we making good time?
What's next?
