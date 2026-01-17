Card format (so you can implement fast)
---------------------------------------

Each “move card” has the same shape:

*   **Phase**: `Night (champion asleep)` / `Day (champion action)` / `Always`

*   **Cost**: `Favor`, `Cred`, `Attention` (+ sometimes `Fatigue` for Day)

*   **Target**: a node/edge in the graph (person, group, location, route, narrative)

*   **World Effect**: changes state (routes, access, relationships, resources)

*   **Belief Effect**: changes confidence/alignment of claims in populations

*   **Telegraph**: what the player can notice beforehand (keeps it logical)

*   **Counters**: what rivals can do _causally_ to respond

*   **Aftermath Hooks**: what new candidates this creates (prevents “samey”)


* * *

Deck A — 12 playable “cards”
============================

1) Dream Courier
----------------

**Major: The Herald** — **Phase:** Night (asleep)

*   **Cost:** Favor 2, Attention 1

*   **Target:** a specific person with at least one social edge to your network

*   **World:** add `DreamSeed(target, theme)` (theme = _fear_, _hope_, _suspicion_, _desire_)

*   **Belief:** the next 1–2 rumors originating from target get `Spread +1` and `Confidence +1` (to their in-group)

*   **Telegraph:** target has a “restless night” tell; NPC mentions strange dreams

*   **Counters:**

    *   **Sun**: “Public Audit” (force testimony / daylight narrative)

    *   **Moon**: “Nightmare Twist” (convert to paranoia: Spread +1 but Confidence -1)

*   **Aftermath hooks:** creates candidate events like _tip-offs_, _secret meetings_, _panic buying_, _accusations_
    #herald #night #belief


2) Public Proclamation
----------------------

**Major: The Herald** — **Phase:** Day

*   **Cost:** Favor 1, Attention 3, Cred risk

*   **Target:** a forum/crowd + a claim (`Claim: X is true / X must be done`)

*   **World:** create `PublicClaim` node linked to witnesses + place

*   **Belief:**

    *   aligned crowds: `Confidence +2`

    *   opposed crowds: `Hostility +1` (they mobilize)

*   **Telegraph:** you can see the crowd size + who’s present (no cheap surprises)

*   **Counters:**

    *   **Sun**: demands proof / issues verdict

    *   **Moon**: undermines witnesses / injects doubt

*   **Aftermath hooks:** _duels, trials, petitions, riots, recruitment drives_
    #herald #day #public


3) Whisper Network
------------------

**Major: The Herald** — **Phase:** Always
Choose **one** mode:

*   **Accelerate** a route (latency -1 for next phase)

*   **Intercept** one message (delay or redirect once)

*   **Amplify** one rumor (Spread +2 but Attention +1)

*   **Cost:** Favor 1 (2 if Amplify), Attention 0–2

*   **Target:** a route edge OR a rumor edge

*   **World:** modifies the edge’s latency/visibility

*   **Belief:** indirect (belief changes because agents act on new info timing)

*   **Telegraph:** you see messenger traffic and “buzz” rising

*   **Counters:** rivals can contest the same edge; Sun can sanction channels; Moon can corrupt them

*   **Aftermath hooks:** _trade swings, ambushes, diplomacy openings, misinformation spirals_
    #herald #always #routes


* * *

4) Dawn Decree
--------------

**Major: The Sun** — **Phase:** Night (asleep)

*   **Cost:** Favor 2, Cred 1, Attention 2

*   **Target:** a Sun-aligned institution in a settlement

*   **World:** set `TomorrowLaw(modifier)` for next Day only

    *   examples: **Curfew**, **Weapon Ban**, **Debt Pause**, **Open Court**, **Road Patrols**

*   **Belief:** legitimacy of that institution `+1` **only if enforced**

*   **Telegraph:** increased patrol prep, bells, scribes working late

*   **Counters:**

    *   **Herald** routes around enforcement (markets move)

    *   **Moon** spawns exceptions/black markets (legitimacy damage if exposed)

*   **Aftermath hooks:** _arrests, protests, smugglers, court petitions, “rule of law” arcs_
    #sun #night #law


5) Writ of Authority
--------------------

**Major: The Sun** — **Phase:** Day

*   **Cost:** Favor 2, Attention 3, Fatigue 1

*   **Target:** an institution action that must be plausible (militia, council, tribunal, charter)

*   **World:** force an **institutional action** NOW:

    *   convene council / deputize guards / seize contraband / grant amnesty / formalize alliance

*   **Belief:** creates `OfficialRecord` edges ⇒ makes the Sun’s version “sticky”

*   **Telegraph:** requires visible symbols (seal, title, allies); no symbol = higher failure risk

*   **Counters:**

    *   **Moon**: scandal the champion (“tyrant”, “hypocrite”)

    *   **Herald**: leak counter-evidence / stir rival forums

*   **Aftermath hooks:** _bureaucratic momentum, rival petitions, coups, legitimacy spikes_
    #sun #day #institutions


6) Seal of Sanction
-------------------

**Major: The Sun** — **Phase:** Always

*   **Cost:** Favor 1, Cred 1

*   **Target:** a claim / document / route / title with witnesses or paperwork

*   **World:** apply `Seal(+Compliance, -Corruption)` to one edge for 1–2 phases

*   **Belief:** boosts confidence in sealed narratives among “order-respecting” groups

*   **Telegraph:** seals are visible; forging becomes a real threat

*   **Counters:** Moon can forge/steal; Herald can leak contradictions; rivals can contest in court

*   **Aftermath hooks:** _forgery plots, audit arcs, smugglers adapting, legal escalation_
    #sun #always #legitimacy


* * *

7) Veilwalk
-----------

**Major: The Moon** — **Phase:** Night (asleep)

*   **Cost:** Favor 2, Attention 1

*   **Target:** a location/route/person tied to Moon influence (or neglected by Sun)

*   **World:** apply `Veil(-Detection, +Stealth, +UnknownUnknowns)` for next phase

*   **Belief:** lowers confidence globally (“accounts conflict”); raises fear in some groups

*   **Telegraph:** owls, fog, missing lanterns, watchers uneasy (signs, not RNG)

*   **Counters:**

    *   **Sun** increases patrol/audit (may burn Cred if fails)

    *   **Herald** triangulates via witnesses + message edges

*   **Aftermath hooks:** _kidnaps, secret rites, heists, shadow diplomacy_
    #moon #night #stealth


8) Quiet Step
-------------

**Major: The Moon** — **Phase:** Day

*   **Cost:** Favor 1–2, Attention 2, Fatigue 1

*   **Target:** a clandestine action in a watched space

*   **World:** perform one covert action **as if it were Night**:

    *   extraction, sabotage, theft, covert meeting, secret oath

*   **Belief:** if successful, creates `PlausibleDeniability` (blame is harder)

*   **Telegraph:** requires planning hooks (safehouse, disguise, distraction)

*   **Counters:** Sun can force testimony and “pin” blame; Herald can reconstruct timelines

*   **Aftermath hooks:** _vendettas, investigations, hidden alliances, blackmail arcs_
    #moon #day #covert


9) Mask & Doubt
---------------

**Major: The Moon** — **Phase:** Always

*   **Cost:** Favor 1

*   **Target:** an active `PublicClaim` or `Accusation`

*   **World:** attach `Doubt(-Confidence, +FactionSplit)` to that narrative for 1–2 phases

*   **Belief:** audiences polarize; consensus slows; canonization delayed

*   **Telegraph:** contradictory whispers, anonymous pamphlets, “I heard…”

*   **Counters:** Sun issues sealed verdict; Herald floods corroboration; rivals hunt the source

*   **Aftermath hooks:** _schisms, witch-hunts, propaganda wars, dueling witnesses_
    #moon #always #misinfo


* * *

10) Bind Contract
-----------------

**Lesser: The Covenant** (tied to Herald) — **Phase:** Always

*   **Conditions:** consent **or** witnessed/recorded agreement

*   **Cost:** Favor 1, Cred 1

*   **Target:** two parties + a term (trade, marriage, non-aggression, debt, service)

*   **World:** create `ContractEdge` with **enforceable consequences**:

    *   breaking it triggers reputation damage + retaliation candidates

*   **Belief:** oath-weight increases among groups that honor contracts

*   **Counterplay:** Moon finds loopholes; Sun adjudicates interpretation; Herald leaks betrayal early

*   **Aftermath hooks:** _breach crises, hostage clauses, trade booms, diplomatic webs_
    #lesser #contract #herald


11) Public Relief
-----------------

**Lesser: The Hearth** (tied to Sun) — **Phase:** Always

*   **Conditions:** only after major harm/unrest OR during scarcity

*   **Cost:** Favor 2, Attention 1

*   **Target:** a Sun-aligned institution with stores (granary, temple, clinic, guild)

*   **World:** convert stores into `Stability(+Recovery, -Unrest)`

*   **Belief:** legitimacy spikes **only if seen** (visibility matters)

*   **Counterplay:** Moon steals/frames it; Herald amplifies credit (or spreads “it was stolen”)

*   **Aftermath hooks:** _dependency politics, relief corruption plots, gratitude factions_
    #lesser #sun #stability


12) Warden’s Reckoning
----------------------

**Lesser: The Warden** (tied to Moon/wild) — **Phase:** Conditional

*   **Conditions:** taboo breach (cruelty/overhunting/sacred harm) **or** invoked after restraint to gain protection

*   **Cost:** Favor 0–2 (0 if triggered as punishment)

*   **Target:** offending group/region

*   **World:** starts a consequence chain:

    *   mounts refuse, guides vanish, predators migrate, disease risk, storms on a route

*   **Belief:** “the wild watches” becomes stronger, altering future behavior

*   **Counterplay:** Sun codifies conservation; Herald reframes blame (risk Cred); Moon offers hush bargains

*   **Aftermath hooks:** _revenge of nature arcs, cult of the wild, moral conflicts_
    #lesser #wild #taboo


* * *

Bonus pack — the “second copy” for each major ability (9 more cards, tighter write-up)
======================================================================================

These complete the “**2 per ability**” feel without bloating the first deck.

Herald — Night: **Translation Dream**
-------------------------------------

*   **Cost:** Favor 2, Cred 1

*   **Effect:** two cultures treat one concept as equivalent (“your oath-god = our oath-god”) ⇒ enables new diplomacy/ritual edges

*   **Counter:** Sun rejects as heresy; Moon twists into schism
    #herald #syncretism


Herald — Day: **Market Parley**
-------------------------------

*   **Cost:** Favor 1, Attention 2

*   **Effect:** force a negotiation at a market node; converts hostility into a tradable term (truce-for-goods, ransom, passage)

*   **Counter:** Moon sabotages talks; Sun demands formal charter
    #herald #trade


Herald — Always: **Leak Proof**
-------------------------------

*   **Cost:** Favor 1, Cred 1, Attention 2

*   **Effect:** reveal one hidden edge (secret meeting, forged seal, bribery) to a chosen audience

*   **Counter:** Moon masks source; Sun weaponizes in court
    #herald #exposure


Sun — Night: **Vigil Oath**
---------------------------

*   **Cost:** Favor 2, Attention 2

*   **Effect:** tomorrow, one cohort gets `Discipline +1` but `Mercy -1` (tight control, harsher outcomes)

*   **Counter:** Herald shifts crowds elsewhere; Moon incites backlash
    #sun #discipline


Sun — Day: **Public Trial**
---------------------------

*   **Cost:** Favor 2, Attention 3

*   **Effect:** converts a conflict into a trial event chain (testimony → verdict → enforcement)

*   **Counter:** Moon corrupts witnesses; Herald floods counter-testimony
    #sun #trial


Sun — Always: **Canon Ledger**
------------------------------

*   **Cost:** Favor 1, Cred 2

*   **Effect:** lock one claim as “canon” for Sun-aligned groups unless overturned by a major scandal/proof event

*   **Counter:** Moon aims for scandal; Herald aims for contradiction
    #sun #canon


Moon — Night: **Night Market**
------------------------------

*   **Cost:** Favor 2, Attention 1

*   **Effect:** spawn a temporary shadow market node that reroutes scarcity (relief with strings)

*   **Counter:** Sun raids; Herald advertises/booby-traps supply lines
    #moon #blackmarket


Moon — Day: **False Flag**
--------------------------

*   **Cost:** Favor 2, Cred risk, Attention 3

*   **Effect:** one hostile act is attributed to a third party (creates war/feud candidates)

*   **Counter:** Herald leaks truth; Sun investigates officially
    #moon #deception


Moon — Always: **Blackmail Thread**
-----------------------------------

*   **Cost:** Favor 1, Attention 2

*   **Effect:** convert one secret edge into leverage (demand, silence, forced alliance)

*   **Counter:** Sun grants amnesty; Herald publicizes early
    #moon #leverage
