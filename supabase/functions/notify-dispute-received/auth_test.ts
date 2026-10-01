import { assertEquals } from "jsr:@std/assert";

Deno.test("missing Authorization returns 401", async () => {
  const { requireClientDisputeNotifierAuth } = await import("./auth.ts");
  const response = await requireClientDisputeNotifierAuth(
    new Request("http://localhost", { method: "POST" }),
    "00000000-0000-0000-0000-000000000001",
    "00000000-0000-0000-0000-000000000002",
  );
  assertEquals(response instanceof Response, true);
  if (response instanceof Response) {
    assertEquals(response.status, 401);
  }
});
