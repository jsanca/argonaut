package dev.jsanca.argonaut.core.testing;

import dev.jsanca.argonaut.core.experiment.ExperimentRequest;
import dev.jsanca.argonaut.core.experiment.ExperimentResult;

/**
 * A single-method contract representing one Argonaut experiment execution.
 *
 * <p>This interface is intentionally minimal: given an {@link ExperimentRequest}, produce an
 * {@link ExperimentResult}. It carries no orchestration semantics. Each framework module
 * implements its own orchestration and wires it behind this interface to participate in
 * use-case contract verification.
 *
 * <p>Future framework test classes should implement or lambda-adapt this interface to plug
 * into {@link ControlledLocalEvidenceContract#verify(ExperimentExecutor)}.
 */
@FunctionalInterface
public interface ExperimentExecutor {
    ExperimentResult execute(ExperimentRequest request);
}
