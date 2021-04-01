package failfast.problematic

// sometimes we have non failfast tests that are called *Test because the test suite is not yet fully converted to failfast
class JunitTest {
    // @Test
    fun `i am a junit test`() {

    }
}
