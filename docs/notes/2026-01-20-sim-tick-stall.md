# Simulation tick stall fix

- Fixed `update-needs` using `rand-nth` with an invalid arity that halted ticks after the first step.
