## What looks right

* **WS open**, ticking is working.
* **Event instances are being generated** and you can see them in both:

  * `Recent events (snapshot)` (historical ring buffer)
  * `Live event feed` (stream)
* Agents clearly have “activated” facets (they’re carrying a lot of `:winter/:cold/:fire/...` in their top facets list).

## What does *not* look right (important)

* **Traces = 0**
* **Ledger = 0**
* **Attribution is empty maps**

That combination means: **the “mention → ledger → attribution” pipeline is not firing**, even though events are happening.

In the earlier screenshot, you had temple-driven traces and a growing ledger. Here it’s silent.

## The most likely cause (based on what I’m seeing)

Your facet activations look **massively saturated** (values like `7.1`, `5.3`, etc). When activations get that large, the recall values tend to become “already maxed”, so:

* `drec = new - old` becomes tiny (or negative after decay)
* `drec > mention-delta` almost never happens
* therefore **no mentions → no ledger → no attribution**

So the sim is *alive*, but the myth engine is stuck in a “high heat equilibrium” where nothing registers as a noteworthy change anymore.

## Quick checks (takes ~30 seconds)

1. Temporarily print counts server-side each tick:

   * `mentions count`
   * `traces count`
   * `ledger entry count`

If mentions are actually happening but UI shows 0, this will reveal it immediately.

2. Verify your mention gate:

   * confirm `mention-delta` is still ~`0.17–0.18`
   * confirm recall isn’t NaN (NaN comparisons always fail)

## The fix I’d do next

Cap / saturate frontier updates so activations stay in **[0,1]** (or at least bounded), e.g. use diminishing returns:

[
a' = a + \Delta(1-a)
]

That one change typically makes “delta-based mention detection” work again, because “new info” creates real deltas instead of getting lost in giant numbers.
