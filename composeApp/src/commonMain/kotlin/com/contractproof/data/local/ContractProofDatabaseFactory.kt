package com.contractproof.data.local

import app.cash.sqldelight.db.SqlDriver

expect fun createContractProofDatabase(driver: SqlDriver): ContractProofDatabase
