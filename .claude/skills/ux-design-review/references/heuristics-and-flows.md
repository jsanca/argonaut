# Heuristics and Flows

## Flow mapping template

For each flow, document:

| Step | User action | System response | Risk / friction |
| --- | --- | --- | --- |
| 1 | … | … | … |

Cover at minimum: entry point, primary success path, validation failure, permission denied, network failure, and exit/cancel.

## Heuristic quick reference

| Heuristic | Review question |
| --- | --- |
| Visibility of status | Does the user always know what the system is doing? |
| Match real world | Do labels match user vocabulary, not database column names? |
| User control | Can users undo, cancel, or go back without losing work? |
| Consistency | Do similar actions look and behave the same across the product? |
| Error prevention | Are destructive or irreversible actions guarded? |
| Recognition over recall | Are options visible instead of hidden behind memorized shortcuts? |
| Flexibility | Are there efficient paths for expert users without harming novices? |
| Minimalist design | Is every element on the screen earning its place? |
| Error recovery | Are error messages specific and actionable? |
| Help | Is contextual help available where confusion is likely? |

## When to escalate to IA change

- Users consistently land on the wrong screen after navigation changes.
- The same concept appears under multiple names in one product.
- Primary tasks require more than three levels of navigation.
- Settings and admin functions are mixed with daily workflows without separation.
