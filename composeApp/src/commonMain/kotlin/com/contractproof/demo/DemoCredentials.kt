package com.contractproof.demo

data class DemoCredentialLine(
    val role: String,
    val email: String,
)

val clearLineDemoCredentials: List<DemoCredentialLine> = listOf(
    DemoCredentialLine(role = "Owner", email = "owner@clearline.demo"),
    DemoCredentialLine(role = "Manager", email = "manager@clearline.demo"),
    DemoCredentialLine(role = "Cleaner", email = "cleaner@clearline.demo"),
    DemoCredentialLine(role = "Client", email = "client@clearline.demo"),
)
