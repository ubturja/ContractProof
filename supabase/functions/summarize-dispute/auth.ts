import { createClient, type SupabaseClient } from "@supabase/supabase-js";

export type AuthContext = {
  userId: string;
  organizationId: string;
  role: string;
  userClient: SupabaseClient;
};

export async function requireDisputeReaderAuth(req: Request): Promise<AuthContext | Response> {
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
    .select("organization_id, role")
    .eq("user_id", userData.user.id)
    .maybeSingle();
  if (membershipError || !membership) {
    return forbidden("Organization membership is required.");
  }
  if (membership.role !== "owner" && membership.role !== "manager") {
    return forbidden("Dispute summaries are not available for this role.");
  }
  return {
    userId: userData.user.id,
    organizationId: membership.organization_id,
    role: membership.role,
    userClient,
  };
}

function unauthorized(): Response {
  return new Response(
    JSON.stringify({
      ok: false,
      error: { message: "Sign in required." },
    }),
    { status: 401, headers: { "Content-Type": "application/json" } },
  );
}

function forbidden(message: string): Response {
  return new Response(
    JSON.stringify({
      ok: false,
      error: { message },
    }),
    { status: 403, headers: { "Content-Type": "application/json" } },
  );
}
