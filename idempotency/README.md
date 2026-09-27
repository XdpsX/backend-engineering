# Idempotency

## Problem

The same operation may be delivered or executed more than once.

Without protection, retries and redelivery can create duplicate side effects.

## Mental model

An idempotent operation can be repeated without changing the final result after the first successful execution.

## Approaches

To explore:

- Idempotency key
- Unique constraint
- Processed message table
- Natural idempotency
- Deduplication store

## Failure cases

Focus on boundaries around:

- database commit
- message acknowledgement
- external side effects

## Experiments

Add small experiments under [experiments](./experiments/).

## References

- Add references while studying.
