package failgood.internal.given

import failgood.dsl.GivenDSL
import failgood.dsl.GivenLambda

class GivenDSLHandler<ParentType>(private val givens: List<GivenLambda<*, *>> = listOf()) :
    GivenDSL<ParentType> {

    override suspend fun given(): ParentType {
        val givenDSL: GivenDSL<*> = SimpleGivenDSL(givens[0])
        @Suppress("UNCHECKED_CAST") return givenDSL.(givens.last())() as ParentType
    }

    fun <GivenType> add(given: GivenLambda<ParentType, GivenType>): GivenDSLHandler<GivenType> {
        @Suppress("UNCHECKED_CAST")
        val newGivens: List<GivenLambda<*, *>> = givens.plus(given as GivenLambda<*, *>)
        return GivenDSLHandler(newGivens)
    }
}

class SimpleGivenDSL<Type>(val lambda: GivenLambda<*, Type>) : GivenDSL<Type> {
    override suspend fun given(): Type {
        return lambda()
    }
}
