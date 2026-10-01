package com.contractproof.app

import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertEquals

class ServiceFlowNavigationTest {
    @Test
    fun finishFromTodayReturnsToToday() {
        val stack = mutableListOf<NavKey>(
            TodayRoute,
            JobRoute("job-1"),
            TaskRoute("job-1", "req-1"),
            CompleteRoute("job-1"),
        )
        popCompletedServiceFlow(stack)
        assertEquals(listOf<NavKey>(TodayRoute), stack)
    }

    @Test
    fun finishFromDashboardReturnsToDashboard() {
        val stack = mutableListOf<NavKey>(
            DashboardRoute,
            JobRoute("job-1"),
            CoverageRoute("job-1"),
            CompleteRoute("job-1"),
        )
        popCompletedServiceFlow(stack)
        assertEquals(listOf<NavKey>(DashboardRoute), stack)
    }

    @Test
    fun finishKeepsRouteUnderTheJob() {
        val stack = mutableListOf<NavKey>(
            DashboardRoute,
            LocationsRoute,
            JobRoute("job-1"),
            CompleteRoute("job-1"),
        )
        popCompletedServiceFlow(stack)
        assertEquals(listOf(DashboardRoute, LocationsRoute), stack)
    }

    @Test
    fun finishDoesNotEmptyALoneServiceRoute() {
        val stack = mutableListOf<NavKey>(
            CompleteRoute("job-1"),
        )
        popCompletedServiceFlow(stack)
        assertEquals(listOf<NavKey>(CompleteRoute("job-1")), stack)
    }
}
