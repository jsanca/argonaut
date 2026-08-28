package dev.jsanca.argonaut.vector.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Default {@link ActiveVectorProvider} backed by an {@link AtomicReference}.
 *
 * <p>Reads the initial provider from {@link VectorProperties} and validates it against the
 * registered adapters in {@link VectorProviderRegistry}. Switching is lock-free.
 */
@Component
public class RuntimeVectorProvider implements ActiveVectorProvider {

    private static final Logger log = LoggerFactory.getLogger(RuntimeVectorProvider.class);

    private final AtomicReference<VectorProvider> current;
    private final VectorProviderRegistry registry;

    public RuntimeVectorProvider(VectorProperties properties, VectorProviderRegistry registry) {
        this.registry = registry;
        VectorProvider initial = properties.vector().provider();
        if (!registry.contains(initial)) {
            throw new IllegalStateException(
                    "Configured initial provider " + initial + " is not available. Available: " + registry.available());
        }
        this.current = new AtomicReference<>(initial);
        log.info("Vector providers available: {}", registry.available());
        log.info("Initial active vector provider: {}", initial);
    }

    @Override
    public VectorProvider current() {
        return current.get();
    }

    @Override
    public void select(VectorProvider provider) {
        if (!registry.contains(provider)) throw new IllegalArgumentException(
                "Cannot select unavailable provider: " + provider + ". Available: " + registry.available());
        VectorProvider previous = current.getAndSet(provider);
        if (previous != provider) log.info("Vector provider changed: {} -> {}", previous, provider);
    }

    @Override
    public List<VectorProvider> available() {
        return registry.available();
    }
}
