package com.test.isolationchamber.core

import kotlin.test.Test
import kotlin.test.assertNotNull

class PostgresqlFactoryTest {
    @Test
    fun `test factory interface exists`() {
        // Dummy test to verify test compilation
        assertNotNull(PostgresqlFactory::class)
    }
}
