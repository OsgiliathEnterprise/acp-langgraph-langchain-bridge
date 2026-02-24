# ACP Session Bridge Streaming End-to-End Tests

## Overview
This test suite validates the complete streaming flow from `AcpSessionBridge` through to `LangChain4j`'s `TokenStream`, ensuring tokens are properly streamed and responses are handled correctly.

## Files Created

### 1. Feature File: `acp_streaming.feature`
Location: `src/test/resources/features/acp_streaming.feature`

Defines three end-to-end streaming scenarios:
- **Scenario 1**: Stream a prompt through ACP Session Bridge to TokenStream
  - Tests that partial tokens (`inprogress`) are streamed
  - Verifies completion with final response (`done`)
  
- **Scenario 2**: Multiple tokens streamed sequentially
  - Validates tokens are emitted in order
  - Ensures final response is present
  
- **Scenario 3**: Stream response completes successfully
  - Confirms no errors occur during streaming
  - Verifies completion handler is called
  - Checks final message is added to state

### 2. Step Definitions: `AcpSessionBridgeStreamingSteps.java`
Location: `src/test/java/.../cucumber/steps/AcpSessionBridgeStreamingSteps.java`

Implements all step definitions with:
- **Given**: Bridge initialization and active session creation
- **When**: Prompt submission, multi-part streaming, full bridge streaming
- **Then**: Token emission validation, completion verification, error checking

Key features:
- Uses `CountDownLatch` to wait for async streaming completion (5-second timeout)
- Captures all streamed tokens in a list for verification
- Tracks errors and final responses
- Integrates with the existing mock `AcpSessionBridge` from `CucumberSpringConfiguration`

## Integration Points

The steps work with the existing test infrastructure:

1. **AcpSessionBridge Mock** (CucumberSpringConfiguration)
   - `streamPrompt()` emits "Mock" + " response" tokens

2. **TokenConsumer Callback**
   - Steps implement the `onNext()` (partial tokens), `onComplete()`, and `onError()` callbacks

3. **LLMProcessorNode Mock** (tested separately)
   - The actual LangChain4j `TokenStream` emits "inprogress" then completes with "done"

## Running the Tests

```bash
cd /Users/charliemordant/Code/Sources/ERP/erp/acp-langraph-langchain-bridge
./gradlew test
```

Or run only the Cucumber tests:
```bash
./gradlew test --tests CucumberTestRunner
```

## Test Flow Diagram

```
Client Prompt
    ↓
AcpSessionBridge.streamPrompt()
    ↓
TokenConsumer Callbacks (onNext, onComplete)
    ↓
Streamed Tokens Collected
    ↓
Assertions Validate Token Stream
    ↓
✓ Test Passes
```

