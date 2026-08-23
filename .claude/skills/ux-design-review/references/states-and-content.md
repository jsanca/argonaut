# States and Content

## Required UI states

Every data-driven screen should define:

| State | User need | Anti-pattern |
| --- | --- | --- |
| Loading | Know data is fetching | Blank screen, layout shift |
| Empty | Know nothing is wrong; see next step | "No data" with no action |
| Error | Know what failed and how to retry | Toast-only with no inline recovery |
| Partial | See what loaded vs what failed | Silent omission |
| Disabled | Know why action is unavailable | Grey button with no explanation |

## Microcopy patterns

**Buttons** — verb + object: "Save draft", "Invite member", not "Submit" or "OK".

**Errors** — `[What happened]. [What to do].` Example: "Payment failed. Check your card details or try another method."

**Empty states** — `[What this is]. [Why empty]. [Primary action].` Example: "No projects yet. Create a project to start tracking work."

**Confirmations** — name the consequence: "Delete workspace?" not "Are you sure?"

## Content review prompts

- Are headings descriptive (scannable) rather than clever?
- Do form labels stay visible (not placeholder-only)?
- Is help text near the field it describes?
- Are units, time zones, and formats explicit where ambiguity causes mistakes?
