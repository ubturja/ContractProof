import { createClient, type SupabaseClient } from "@supabase/supabase-js";

export type ClientNotifyAuthContext = {
  userId: string;
  organizationId: string;
  clientId: string;
  userClient: SupabaseClient;
};

export async function requireClientDisputeNotifierAuth(
  req: Request,
  organizationId: string,
  disputeId: string,
): Promise<ClientNotifyAuthContext | Response> {
  const authHeader = req.headers.get("Authorization");
  if (!authHeader?.startsWith("Bearer ")) {
    return unauthorized();
  }
  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const supabaseAnonKey = Deno.env.get("SUPABASE_ANON_KEY");
  if (!supabaseUrl || !supabaseAnonKey) {
    throw new Error("Supabase env is not configured.");
  }
  const userClient = createClient(supabaseUrl, supabaseAnonKey, {
    global: { headers: { Authorization: authHeader } },
  });
  const { data: userData, error: userError } = await userClient.auth.getUser();
  if (userError || !userData.user) {
    return unauthorized();
  }
  const { data: membership, error: membershipError } = await userClient
    .from("organization_members")
    .select("organization_id, role, client_id")
    .eq("user_id", userData.user.id)
    .maybeSingle();
  if (membershipError || !membership) {
    return forbidden("Organization membership is required.");
  }
  if (membership.role !== "client") {
    return forbidden("Only a client can request dispute notifications.");
  }
  if (!membership.client_id) {
    return forbidden("Client membership is incomplete.");
  }
  if (membership.organization_id !== organizationId) {
    return forbidden("Organization does not match your membership.");
  }
  const { data: dispute, error: disputeError } = await userClient
    .from("disputes")
    .select("id, organization_id, client_id, recorded_by")
    .eq("id", disputeId)
    .eq("organization_id", organizationId)
    .maybeSingle();
  if (disputeError || !dispute) {
    return forbidden("Dispute was not found.");
  }
  if (dispute.client_id !== membership.client_id) {
    return forbidden("Dispute does not belong to this client.");
  }
  if (dispute.recorded_by !== userData.user.id) {
    return forbidden("Dispute was not filed by the signed-in user.");
  }
  return {
    userId: userData.user.id,
    organizationId: membership.organization_id,
    clientId: membership.client_id,
    userClient,
  };
}

function unauthorized(): Response {
  return new Response(
    JSON.stringify({ ok: false, error: "unauthorized" }),
    { status: 401, headers: { "Content-Type": "application/json" } },
  );
}

function forbidden(message: string): Response {
  return new Response(
    JSON.stringify({ ok: false, error: message }),
    { status: 403, headers: { "Content-Type": "application/json" } },
  );
}
