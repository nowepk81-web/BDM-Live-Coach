# BDM Live Coach 1.2

## Findings and changes

- The first SpeechRecognizer start was called inside the initializer before assignment to the recognizer field. The nullable field was still null, so startListening was never called. Assignment now precedes listening.
- Recognition errors no longer falsely mark the UI stopped while recognition is retrying.
- Starting a session replaces old recognition/backend instances and resets context.
- Backend HTTP failures and authentication failures are visible; request timeouts allow more time for a sleeping Render instance.
- Only one AI request runs at a time, avoiding an unbounded request queue. Closed sessions suppress late backend results.
- DeepSeek now uses Chat Completions with JSON output instead of the Responses endpoint. Server-side response validation is retained.
- Screen content scrolls, respects system insets, and displays version 1.2. Recognized text remains visible with local results.

## Verification

Server integration test: run `node --test test/integration.test.mjs` inside server.
It uses a local mock AI service, not a real API key. It checks health, wrong token, missing/short/oversized context, Chat Completions request format, successful output and invalid model output.

Android checks: `gradlew.bat assembleDebug lintDebug testDebugUnitTest`.
There are no Android unit tests in the existing project. Physical speech recognition and device UI require a connected phone; no device was attached during verification.

Health HTTP 200 alone does not prove token validity, DeepSeek configuration, account balance, or actual speech recognition on a phone.
