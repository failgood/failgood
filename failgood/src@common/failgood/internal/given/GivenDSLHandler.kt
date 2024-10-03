package failgood.internal.given

import failgood.dsl.GivenDSL
import failgood.dsl.GivenFunction

interface GivenDSLHandler<ParentType> : GivenDSL<ParentType> {
    override suspend fun given(): ParentType

    fun <GivenType> add(given: GivenFunction<ParentType, GivenType>): GivenDSLHandler<GivenType>
}

class RootGivenDSLHandler<GivenType>(private val given: suspend () -> GivenType) :
    GivenDSLHandler<GivenType> {

    override suspend fun given(): GivenType {
        val g = this.given
        return g()
    }

    override fun <GivenT> add(given: GivenFunction<GivenType, GivenT>): GivenDSLHandler<GivenT> {
        @Suppress("UNCHECKED_CAST")
        return ChildGivenDSLHandler(given as GivenFunction<*, GivenT>, this)
    }
}

private class ChildGivenDSLHandler<ParentType>(
    private val given: GivenFunction<*, ParentType>,
    private val parent: GivenDSLHandler<*>
) : GivenDSLHandler<ParentType> {

    override suspend fun given(): ParentType {
        val g = given
        return parent.g()
    }

    override fun <GivenType> add(
        given: GivenFunction<ParentType, GivenType>
    ): ChildGivenDSLHandler<GivenType> {
        @Suppress("UNCHECKED_CAST")
        return ChildGivenDSLHandler(given as GivenFunction<*, GivenType>, this)
    }
}
