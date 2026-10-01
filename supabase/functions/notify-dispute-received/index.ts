import { createServiceClient } from "../generate-dispute-report/admin.ts";
import { requireClientDisputeNotifierAuth } from "./auth.ts";

type RequestBody = {
  dispute_id?: string;
  organization_id?: string;
};

Deno.serve(async (req) => {
  if (req.method !== "POST") {
    return new Response("Method not allowed", { status: 405 });
  }
  const fcmKey = Deno.env.get("FCM_SERVER_KEY");
  if (!fcmKey) {
    return new Response(JSON.stringify({ ok: true, skipped: "fcm_not_configured" }), {
      status: 200,
      headers: { "Content-Type": "application/json" },
    });
  }
  let body: RequestBody;
  try {
    body = await req.json();
  } catch {
    return new Response(JSON.stringify({ ok: false, error: "invalid_json" }), { status: 400 });
  }
  const disputeId = body.dispute_id;
  const organizationId = body.organization_id;
  if (!disputeId || !organizationId) {
    return new Response(JSON.stringify({ ok: false, error: "missing_fields" }), { status: 400 });
  }
  const auth = await requireClientDisputeNotifierAuth(req, organizationId, disputeId);
  if (auth instanceof Response) {
    return auth;
  }
  const admin = createServiceClient();
  const { data: owners, error: ownersError } = await admin
    .from("organization_members")
    .select("user_id")
    .eq("organization_id", organizationId)
    .eq("role", "owner");
  if (ownersError || !owners?.length) {
    return new Response(JSON.stringify({ ok: true, sent: 0 }), {
      status: 200,
      headers: { "Content-Type": "application/json" },
    });
  }
  const ownerIds = owners.map((row) => row.user_id as string);
  const { data: tokens, error: tokensError } = await admin
    .from("push_device_tokens")
    .select("token")
    .in("user_id", ownerIds);
  if (tokensError || !tokens?.length) {
    return new Response(JSON.stringify({ ok: true, sent: 0 }), {
      status: 200,
      headers: { "Content-Type": "application/json" },
    });
  }
  let sent = 0;
  for (const row of tokens) {
    const token = row.token as string;
    const response = await fetch("https://fcm.googleapis.com/fcm/send", {
      method: "POST",
      headers: {
        Authorization: `key=${fcmKey}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        to: token,
        data: {
          type: "dispute_received",
          dispute_id: disputeId,
        },
      }),
    });
    if (response.ok) {
      sent += 1;
    }
  }
  return new Response(JSON.stringify({ ok: true, sent }), {
    status: 200,
    headers: { "Content-Type": "application/json" },
  });
});
