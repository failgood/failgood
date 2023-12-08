package failgood.internal.given

import failgood.dsl.GivenDSL
import failgood.dsl.GivenLambda

interface GivenDSLHandler<ParentType> : GivenDSL<ParentType> {
    override suspend fun given(): ParentType

    fun <GivenType> add(given: GivenLambda<ParentType, GivenType>): GivenDSLHandler<GivenType>
}

class RootGivenDSLHandler<GivenType>(private val given: suspend () -> GivenType) :
    GivenDSLHandler<GivenType> {

    override suspend fun given(): GivenType {
        val g = this.given
        return g()
    }

    override fun <GivenT> add(given: GivenLambda<GivenType, GivenT>): GivenDSLHandler<GivenT> {
        @Suppress("UNCHECKED_CAST")
        return ChildGivenDSLHandler(given as GivenLambda<*, GivenT>, this)
    }
}

private class ChildGivenDSLHandler<ParentType>(
    private val given: GivenLambda<*, ParentType>,
    private val parent: GivenDSLHandler<*>
) : GivenDSLHandler<ParentType> {

    override suspend fun given(): ParentType {
        val g = given
        return parent.g()
    }

    override fun <GivenType> add(
        given: GivenLambda<ParentType, GivenType>
    ): ChildGivenDSLHandler<GivenType> {
        @Suppress("UNCHECKED_CAST")
        return ChildGivenDSLHandler(given as GivenLambda<*, GivenType>, this)
    }
}
