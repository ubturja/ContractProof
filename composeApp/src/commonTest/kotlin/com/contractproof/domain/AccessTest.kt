package com.contractproof.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AccessTest {
    @Test
    fun ownerCanManageFieldApp() {
        val access = Access.forRole(Role.Owner)
        assertEquals(Home.Dashboard, access.home)
        assertTrue(access.canOpenDashboard)
        assertTrue(access.canAddLocation)
        assertTrue(access.canWriteContracts)
        assertTrue(access.canWriteDisputes)
        assertTrue(access.canManageOrganization)
        assertTrue(access.canOpenClients)
        assertTrue(access.canWriteClients)
        assertTrue(Role.Owner.allowsLocation("loc", emptyList()))
    }

    @Test
    fun managerReadsDashboardButCannotWriteCompany() {
        val access = Access.forRole(Role.Manager)
        assertEquals(Home.Dashboard, access.home)
        assertTrue(access.canOpenDashboard)
        assertTrue(access.canOpenContracts)
        assertTrue(access.canOpenDisputes)
        assertFalse(access.canAddLocation)
        assertFalse(access.canWriteContracts)
        assertFalse(access.canWriteDisputes)
        assertFalse(access.canCreateCompany)
        assertFalse(access.canManageSubscription)
        assertFalse(access.canInviteMembers)
        assertTrue(access.canOpenClients)
        assertFalse(access.canWriteClients)
        assertFalse(Role.Manager.allowsLocation("loc", emptyList()))
        assertTrue(Role.Manager.allowsLocation("loc", listOf("loc")))
    }

    @Test
    fun cleanerStaysOnToday() {
        val access = Access.forRole(Role.Cleaner)
        assertEquals(Home.Today, access.home)
        assertFalse(access.canOpenDashboard)
        assertFalse(access.canOpenContracts)
        assertFalse(access.canOpenDisputes)
        assertFalse(access.canOpenClients)
        assertFalse(access.canAddLocation)
        assertTrue(access.canOpenAssignedJobs)
        assertTrue(access.canOpenSettings)
    }

    @Test
    fun clientHasNoFieldAppWrites() {
        val access = Access.forRole(Role.Client)
        assertEquals(Home.ClientHold, access.home)
        assertFalse(access.canOpenDashboard)
        assertFalse(access.canAddLocation)
        assertFalse(access.canWriteContracts)
        assertFalse(access.canWriteDisputes)
        assertFalse(access.canOpenAssignedJobs)
        assertFalse(access.canOpenSettings)
        assertFalse(access.canCreateCompany)
    }

    @Test
    fun unknownRoleIsClient() {
        assertEquals(Role.Client, Role.from("admin"))
        assertEquals(Home.ClientHold, Access.forMembership("admin").home)
        assertEquals(Home.ClientHold, Access.forMembership(null).home)
    }

    @Test
    fun unsignedMayCreateFirstCompany() {
        assertTrue(Access.unsigned.canCreateCompany)
        assertFalse(Access.unsigned.canOpenDashboard)
    }
}
