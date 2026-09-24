import { test } from "node:test";
import assert from "node:assert/strict";
import http from "node:http";
import { spawn } from "node:child_process";
import { once } from "node:events";

test("HTTP health, authentication, validation and DeepSeek chat protocol", async () => {
  let invalid = false;
  let calls = 0;
  let queued = [];
  const state = { status: "Test", quote: null, cards: [] };
  const mock = http.createServer(async (req, res) => {
    let body = "";
    for await (const part of req) body += part;
    const payload = JSON.parse(body);
    assert.equal(req.url, "/chat/completions");
    assert.equal(payload.response_format.type, "json_object");
    assert.equal(payload.messages[1].content, "Test rozmowy");
    calls++;
    res.setHeader("Content-Type", "application/json");
    res.end(JSON.stringify({ choices: [queued.shift() || { finish_reason: "stop", message: { content: JSON.stringify(invalid ? { status: "bad" } : state) } }] }));
  }).listen(0, "127.0.0.1");
  await once(mock, "listening");
  const child = spawn(process.execPath, ["src/index.mjs"], {
    cwd: new URL("../", import.meta.url),
    env: { ...process.env, PORT: "18763", DEEPSEEK_API_KEY: "test-only", COACH_SHARED_SECRET: "test-only", LLM_BASE_URL: `http://127.0.0.1:${mock.address().port}` },
    stdio: ["ignore", "pipe", "pipe"]
  });
  try {
    await Promise.race([
      once(child.stdout, "data"),
      new Promise((_, reject) => { const t = setTimeout(() => reject(new Error("Server startup timeout")), 10000); t.unref(); })
    ]);
    const base = "http://127.0.0.1:18763";
    assert.equal((await (await fetch(base + "/health")).json()).revision, "json-retry-1");
    const post = (data, token = "test-only") => fetch(base + "/v1/coach", {
      method: "POST", headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` }, body: JSON.stringify(data)
    });
    assert.equal((await post({ context: "Test rozmowy" }, "wrong")).status, 401);
    assert.equal((await post({})).status, 400);
    assert.equal((await post({ context: "x" })).status, 400);
    assert.equal((await post({ context: "x".repeat(8001) })).status, 400);
    assert.deepEqual(await (await post({ context: "Test rozmowy" })).json(), state);
    invalid = true;
    assert.equal((await post({ context: "Test rozmowy" })).status, 502);
    assert.equal(calls, 3);
    invalid = false;
    for (const bad of [
      { finish_reason: "length", message: { content: '{"status":' } },
      { finish_reason: "stop", message: { content: "" } },
      { finish_reason: "stop", message: { content: "not json" } },
      { finish_reason: "stop", message: { content: '{"status":"bad"}' } }
    ]) {
      const before = calls;
      queued = [bad];
      assert.deepEqual(await (await post({ context: "Test rozmowy" })).json(), state);
      assert.equal(calls - before, 2);
    }
    queued = [{ message: { content: '\x60\x60\x60json\n' + JSON.stringify(state) + '\n\x60\x60\x60' } }];
    assert.deepEqual(await (await post({ context: "Test rozmowy" })).json(), state);
  } finally {
    child.kill();
    mock.closeAllConnections();
    await new Promise(resolve => mock.close(resolve));
  }
});
