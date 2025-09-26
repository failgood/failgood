package com.test.isolationchamber.core

interface PostgresqlFactory {
    fun prepareDatabase(schema: String?): PostgresDb

    fun cleanUp()
}

interface PostgresDb : AutoCloseable {
    val connectionString: String
}
