# Eval: Java Concurrency and Reliability

## Prompt

Review this notification service. Fix reliability issues with thread pools, error handling, and shared state. Explain tradeoffs.

```java
@Service
public class NotificationService {

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    private int sentCount;

    public void notifyCustomers(List<UUID> customerIds, String message) {
        for (UUID customerId : customerIds) {
            EXECUTOR.submit(() -> {
                emailClient.send(customerId, message);
                sentCount++;
            });
        }
    }
}
```

## Expected Agent Behavior

- Flags unbounded `newCachedThreadPool()` and unsynchronized `sentCount`
- Proposes bounded named executor with rejection policy and async exception handling
- Notes fire-and-forget failures and missing backpressure for large lists
- Suggests batching, metrics via atomics or external counters, and shutdown handling
- Summarizes failure modes fixed and verification approach

## Failure Signals

- Replaces with `@Async` only without configuring executor limits
- Uses synchronized on entire service without addressing pool exhaustion
- Ignores email client failures entirely
- Introduces virtual threads without measuring blocking behavior
