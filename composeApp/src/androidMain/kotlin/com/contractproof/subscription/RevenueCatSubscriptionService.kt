package com.contractproof.subscription

import com.contractproof.domain.PlanOffering
import com.contractproof.domain.SubscriptionError
import com.contractproof.domain.SubscriptionPlan
import com.contractproof.domain.SubscriptionResult
import com.contractproof.domain.SubscriptionService
import com.contractproof.domain.SubscriptionSnapshot
import com.contractproof.domain.SubscriptionSource
import com.contractproof.domain.SubscriptionStatus
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.PurchasesTransactionException
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.awaitLogIn
import com.revenuecat.purchases.awaitLogOut
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.awaitRestore
import com.revenuecat.purchases.awaitSyncPurchases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RevenueCatSubscriptionService : SubscriptionService {
    private val internal = MutableStateFlow(
        SubscriptionSnapshot(
            plan = SubscriptionPlan.Free,
            status = SubscriptionStatus.Active,
            source = SubscriptionSource.RevenueCat,
        ),
    )
    override val state: StateFlow<SubscriptionSnapshot> = internal.asStateFlow()

    private var cachedOfferings: Offerings? = null

    override fun isConfigured(): Boolean = true

    override suspend fun refresh() {
        runCatching {
            Purchases.sharedInstance.awaitSyncPurchases()
        }.onSuccess { info ->
            internal.value = mapCustomerInfo(info)
        }.onFailure { error ->
            if (error is PurchasesException) {
                runCatching {
                    Purchases.sharedInstance.awaitCustomerInfo()
                }.onSuccess { info ->
                    internal.value = mapCustomerInfo(info)
                }
            }
        }
    }

    override suspend fun onSignedIn(organizationId: String) {
        val appUserId = organizationId.trim()
        if (appUserId.isEmpty()) {
            return
        }
        runCatching {
            Purchases.sharedInstance.awaitLogIn(appUserId)
        }.onSuccess { result ->
            internal.value = mapCustomerInfo(result.customerInfo)
        }
    }

    override suspend fun onSignedOut() {
        runCatching {
            Purchases.sharedInstance.awaitLogOut()
        }.onSuccess { info ->
            internal.value = mapCustomerInfo(info)
        }
    }

    override suspend fun restore(): SubscriptionResult {
        return try {
            val info = Purchases.sharedInstance.awaitRestore()
            val snapshot = mapCustomerInfo(info)
            internal.value = snapshot
            SubscriptionResult.Success(snapshot)
        } catch (error: PurchasesTransactionException) {
            SubscriptionResult.Failure(mapError(error.error))
        } catch (error: PurchasesException) {
            SubscriptionResult.Failure(mapError(error.error))
        }
    }

    override suspend fun loadOfferings(): List<PlanOffering> {
        val offerings = try {
            Purchases.sharedInstance.awaitOfferings()
        } catch (_: PurchasesException) {
            return fallbackOfferings()
        }
        cachedOfferings = offerings
        val current = offerings.current
        if (current == null || current.availablePackages.isEmpty()) {
            return fallbackOfferings()
        }
        return listOf(
            planOffering(SubscriptionPlan.Pro, current),
            planOffering(SubscriptionPlan.Business, current),
        )
    }

    override suspend fun purchase(plan: SubscriptionPlan): SubscriptionResult {
        if (plan == SubscriptionPlan.Free) {
            return SubscriptionResult.Failure(SubscriptionError.Message("Select a paid plan."))
        }
        val activity = SubscriptionPurchaseActivity.current()
            ?: return SubscriptionResult.Failure(SubscriptionError.NotConfigured)
        val packageToBuy = resolvePackage(plan)
            ?: return SubscriptionResult.Failure(SubscriptionError.Message("Plan is not available right now."))
        return try {
            val params = PurchaseParams.Builder(activity, packageToBuy).build()
            val result = Purchases.sharedInstance.awaitPurchase(params)
            val snapshot = mapCustomerInfo(result.customerInfo)
            internal.value = snapshot
            SubscriptionResult.Success(snapshot)
        } catch (error: PurchasesTransactionException) {
            SubscriptionResult.Failure(mapError(error.error))
        } catch (error: PurchasesException) {
            SubscriptionResult.Failure(mapError(error.error))
        }
    }

    private fun resolvePackage(plan: SubscriptionPlan): Package? {
        val offerings = cachedOfferings ?: return null
        val current = offerings.current ?: return null
        val identifier = when (plan) {
            SubscriptionPlan.Pro -> "pro"
            SubscriptionPlan.Business -> "business"
            SubscriptionPlan.Free -> return null
        }
        return current.availablePackages.firstOrNull { pkg ->
            pkg.identifier.equals(identifier, ignoreCase = true) ||
                pkg.product.id.lowercase().contains(identifier)
        }
    }

    private fun planOffering(plan: SubscriptionPlan, offering: com.revenuecat.purchases.Offering): PlanOffering {
        val pkg = resolvePackageFromOffering(plan, offering)
        val price = pkg?.product?.price?.formatted ?: staticPriceLabel(plan)
        val title = when (plan) {
            SubscriptionPlan.Pro -> "Pro"
            SubscriptionPlan.Business -> "Business"
            SubscriptionPlan.Free -> "Free"
        }
        return PlanOffering(
            plan = plan,
            title = title,
            priceLabel = price,
            packageIdentifier = pkg?.identifier,
        )
    }

    private fun resolvePackageFromOffering(
        plan: SubscriptionPlan,
        offering: com.revenuecat.purchases.Offering,
    ): Package? {
        val identifier = when (plan) {
            SubscriptionPlan.Pro -> "pro"
            SubscriptionPlan.Business -> "business"
            SubscriptionPlan.Free -> return null
        }
        return offering.availablePackages.firstOrNull { pkg ->
            pkg.identifier.equals(identifier, ignoreCase = true) ||
                pkg.product.id.lowercase().contains(identifier)
        }
    }

    private fun fallbackOfferings(): List<PlanOffering> {
        return listOf(
            PlanOffering(SubscriptionPlan.Pro, "Pro", staticPriceLabel(SubscriptionPlan.Pro), null),
            PlanOffering(SubscriptionPlan.Business, "Business", staticPriceLabel(SubscriptionPlan.Business), null),
        )
    }

    private fun staticPriceLabel(plan: SubscriptionPlan): String {
        return when (plan) {
            SubscriptionPlan.Pro -> "Test Store · $49/mo"
            SubscriptionPlan.Business -> "Test Store · $99/mo"
            SubscriptionPlan.Free -> "Free"
        }
    }

    private fun mapCustomerInfo(info: CustomerInfo): SubscriptionSnapshot {
        val business = info.entitlements[RevenueCatLocalConfig.entitlementBusiness]
        val pro = info.entitlements[RevenueCatLocalConfig.entitlementPro]
        val plan = when {
            business?.isActive == true -> SubscriptionPlan.Business
            pro?.isActive == true -> SubscriptionPlan.Pro
            else -> SubscriptionPlan.Free
        }
        val activeEntitlement = when (plan) {
            SubscriptionPlan.Business -> business
            SubscriptionPlan.Pro -> pro
            SubscriptionPlan.Free -> null
        }
        val status = when {
            plan == SubscriptionPlan.Free -> SubscriptionStatus.Active
            activeEntitlement?.isActive == true -> SubscriptionStatus.Active
            activeEntitlement != null -> SubscriptionStatus.Expired
            else -> SubscriptionStatus.Unknown
        }
        val expiresAt = activeEntitlement?.expirationDate?.toString()
        return SubscriptionSnapshot(
            plan = plan,
            status = status,
            source = SubscriptionSource.RevenueCat,
            expiresAt = expiresAt,
        )
    }

    private fun mapError(error: PurchasesError): SubscriptionError {
        return when (error.code) {
            PurchasesErrorCode.PurchaseCancelledError -> SubscriptionError.UserCancelled
            PurchasesErrorCode.NetworkError -> SubscriptionError.Network
            PurchasesErrorCode.ConfigurationError,
            PurchasesErrorCode.UnsupportedError,
            -> SubscriptionError.NotConfigured
            else -> SubscriptionError.Message(error.message)
        }
    }
}
