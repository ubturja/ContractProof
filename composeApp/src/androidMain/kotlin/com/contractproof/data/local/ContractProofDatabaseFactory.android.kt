package com.contractproof.data.local

import app.cash.sqldelight.db.SqlDriver

actual fun createContractProofDatabase(driver: SqlDriver): ContractProofDatabase {
    return ContractProofDatabase(driver)
}
