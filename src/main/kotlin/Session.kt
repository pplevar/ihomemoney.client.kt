package ru.levar

import ru.levar.api.HomemoneyApiService
import ru.levar.domain.Account
import ru.levar.domain.AccountGroup
import ru.levar.domain.Category
import ru.levar.domain.Transaction

/**
 * An authenticated handle to the iHomemoney API.
 *
 * A [Session] is the only way to reach authenticated operations: every data call hangs
 * off an instance, so "log in before calling anything" is carried by the type rather than
 * re-checked at runtime in each method. It is obtained from
 * [HomemoneyApiClient.authenticate]; its constructor is module-internal, so a caller cannot
 * fabricate one without going through authentication.
 *
 * The issued token is held privately here — the one place auth state lives — and attached to
 * each request.
 */
class Session internal constructor(
    private val apiService: HomemoneyApiService,
    private val token: String,
) {
    suspend fun accountGroups(): ApiResult<List<AccountGroup>> =
        interpret { apiService.getAccountGroups(token) }.map { it.listAccountGroupInfo }

    suspend fun accounts(): ApiResult<List<Account>> = accountGroups().map { groups -> groups.flatMap { it.listAccountInfo } }

    suspend fun categories(): ApiResult<List<Category>> = interpret { apiService.getCategories(token) }.map { it.listCategory }

    suspend fun transactions(topCount: Int?): ApiResult<List<Transaction>> =
        interpret { apiService.getTransactions(token, topCount) }.map { it.listTransaction }
}
