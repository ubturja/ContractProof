package com.contractproof.data.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver

object ContractProofTestDatabase {
    fun create(): ContractProofDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ContractProofDatabase.Schema.create(driver)
        return createContractProofDatabase(driver)
    }
}
