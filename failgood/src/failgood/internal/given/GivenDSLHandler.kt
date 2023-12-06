package failgood.internal.given

import failgood.dsl.GivenDSL
import failgood.dsl.GivenLambda

class GivenDSLHandler<ParentType>(
    private val given: GivenLambda<*, *> = {},
    private val parent: GivenDSLHandler<*>? = null
) : GivenDSL<ParentType> {

    override suspend fun given(): ParentType {
        val result =
            if (parent == null) Unit
            else {
                val g = given
                parent.g()
            }
        @Suppress("UNCHECKED_CAST") return result as ParentType
    }

    fun <GivenType> add(given: GivenLambda<ParentType, GivenType>): GivenDSLHandler<GivenType> {
        @Suppress("UNCHECKED_CAST") return GivenDSLHandler(given as GivenLambda<*, *>, this)
    }
}
