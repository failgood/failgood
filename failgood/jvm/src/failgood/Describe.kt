package failgood

import failgood.dsl.RootContextLambda
import failgood.internal.util.niceString
import kotlin.reflect.KClass
import kotlin.reflect.KType

@JvmName("describeWithDependency")
fun <RootContextDependency> describe(
    subjectDescription: String,
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    function: RootContextLambda<RootContextDependency>
): RootContext<RootContextDependency> =
    RootContext(
        subjectDescription,
        ignored,
        order,
        isolation,
        addClassName = true,
        function = function
    )

fun describe(
    subjectDescription: String,
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    function: RootContextLambda<Unit>
): RootContext<Unit> =
    RootContext(
        subjectDescription,
        ignored,
        order,
        isolation,
        addClassName = true,
        function = function
    )

@JvmName("describeWithDependency")
fun <RootContextDependency> describe(
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    function: RootContextLambda<RootContextDependency>
): RootContext<RootContextDependency> =
    RootContext("", ignored, order, isolation, addClassName = true, function = function)

fun describe(
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    function: RootContextLambda<Unit>
): RootContext<Unit> =
    RootContext("", ignored, order, isolation, addClassName = true, function = function)

/*@JvmName("describe2")
inline fun <reified T> describe(
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    noinline function: RootContextLambda
): RootContext = describe(typeOf<T>(), ignored, order, isolation, function)
*/
@JvmName("describeTypeWithDependency")
fun <RootContextDependency> describe(
    subjectType: KType,
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    function: RootContextLambda<RootContextDependency>
): RootContext<RootContextDependency> =
    RootContext(subjectType.niceString(), ignored, order, isolation, function = function)

fun describe(
    subjectType: KType,
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    function: RootContextLambda<Unit>
): RootContext<Unit> =
    RootContext(subjectType.niceString(), ignored, order, isolation, function = function)

@JvmName("describeWithDependency")
fun <RootContextDependency> describe(
    subjectType: KClass<*>,
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    function: RootContextLambda<RootContextDependency>
): RootContext<RootContextDependency> =
    RootContext("${subjectType.simpleName}", ignored, order, isolation, function = function)

fun describe(
    subjectType: KClass<*>,
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    function: RootContextLambda<Unit>
): RootContext<Unit> =
    RootContext("${subjectType.simpleName}", ignored, order, isolation, function = function)

suspend inline fun <reified Class> failgood.dsl.ContextDSL<*, *>.describe(
    tags: Set<String> = setOf(),
    isolation: Boolean? = null,
    ignored: Ignored? = null,
    noinline rootContextLambda: RootContextLambda<Unit>
) = this.describe(Class::class.simpleName!!, tags, isolation, ignored, rootContextLambda)
