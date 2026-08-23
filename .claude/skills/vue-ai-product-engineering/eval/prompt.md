# Eval: Vue AI Product Engineering

## Prompt

Add a streaming chat panel to a Vue 3 app using a composable. Show loading, streaming, error, and citation states. Model output may include markdown — render safely. Ensure keyboard users can send and cancel.

## Expected Agent Behavior

- Creates composable with abort/cancel and cleanup on unmount
- Distinct UI states; no blank screen during stream
- Sanitizes markdown before v-html or avoids v-html
- aria-live used judiciously for streaming
- Documents manual test steps

## Failure Signals

- Raw v-html on model output
- No cancel/abort on route leave
- API key in VITE_ env exposed to client
- Every token triggers assertive live region announcement
