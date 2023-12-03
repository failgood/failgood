package failgood.internal.execution.context

import failgood.dsl.GivenDSL
import failgood.dsl.GivenLambda

class GivenDSLHandler<ParentType>(val givens: List<GivenLambda<*, *>> = listOf()) :
    GivenDSL<ParentType> {

    override fun given(): ParentType {
        TODO("Not yet implemented")
    }

    fun <GivenType> add(given: GivenLambda<ParentType, GivenType>): GivenDSLHandler<GivenType> {
        @Suppress("UNCHECKED_CAST")
        val newGivens: List<GivenLambda<*, *>> = givens.plus(given as GivenLambda<*, *>)
        return GivenDSLHandler(newGivens)
    }
}
