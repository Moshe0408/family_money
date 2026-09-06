package com.familymoney.data.bank

import com.familymoney.data.db.AccountEntity
import com.familymoney.data.db.CardEntity
import com.familymoney.data.db.TransactionEntity

/**
 * Section 12-13: Open Banking abstraction.
 *
 * Connecting to an Israeli bank requires a licensed TPP (Third Party Provider)
 * under the Financial Information Service Law. This interface is the seam that
 * such a provider plugs into: implement [connect] and [fetch] against the
 * provider's SDK and every screen above keeps working unchanged.
 */
interface BankProvider {
    val id: String
    val displayName: String
    val logoEmoji: String
    val kind: ProviderKind
    val requiresLicensedTpp: Boolean

    suspend fun connect(request: ConnectRequest): ConnectResult
    suspend fun fetch(connectionId: String): FetchResult
    suspend fun disconnect(connectionId: String): Boolean
}

enum class ProviderKind { BANK, CARD_ISSUER, MANUAL }

data class ConnectRequest(
    val providerId: String,
    val ownerName: String,
    val familyId: String,
    /**
     * Consent artefacts only. Bank passwords are never accepted, stored or
     * transmitted by this app (spec section 12 and 41).
     */
    val consentToken: String? = null
)

sealed interface ConnectResult {
    data class Success(val connectionId: String, val accounts: List<AccountEntity>, val cards: List<CardEntity>) : ConnectResult
    /** The provider needs the user to finish an OAuth/consent flow in a browser. */
    data class ConsentRequired(val consentUrl: String) : ConnectResult
    data class Failure(val message: String) : ConnectResult
    data class NotAvailable(val message: String) : ConnectResult
}

data class FetchResult(
    val accounts: List<AccountEntity> = emptyList(),
    val cards: List<CardEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val error: String? = null
)

/** The Israeli institutions the UI offers (section 12). */
object Providers {

    val banks = listOf(
        ProviderInfo("hapoalim", "בנק הפועלים", "🔴"),
        ProviderInfo("leumi", "בנק לאומי", "🔵"),
        ProviderInfo("discount", "בנק דיסקונט", "🟢"),
        ProviderInfo("mizrahi", "מזרחי טפחות", "🟠"),
        ProviderInfo("beinleumi", "הבנק הבינלאומי", "🟣"),
        ProviderInfo("massad", "בנק מסד", "🟡"),
        ProviderInfo("yahav", "בנק יהב", "🔷"),
        ProviderInfo("one_zero", "ONE ZERO", "⚫"),
        ProviderInfo("pepper", "פפר", "🌶️"),
        ProviderInfo("jerusalem", "בנק ירושלים", "🟤")
    )

    val cardIssuers = listOf(
        ProviderInfo("isracard", "ישראכרט", "💳"),
        ProviderInfo("cal", "כאל", "💳"),
        ProviderInfo("max", "מקס", "💳"),
        ProviderInfo("amex", "אמריקן אקספרס", "💳"),
        ProviderInfo("diners", "דיינרס", "💳")
    )
}

data class ProviderInfo(val id: String, val name: String, val emoji: String)
