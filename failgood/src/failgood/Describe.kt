package failgood

import failgood.dsl.ContextDSL
import failgood.dsl.ContextFunction
import failgood.dsl.ContextFunctionWithGiven
import failgood.internal.util.niceString
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

fun describe(
    subjectDescription: String,
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    function: ContextFunction
): RootContext =
    RootContext(
        subjectDescription,
        ignored,
        order,
        isolation,
        addClassName = true,
        function = function
    )

fun <RootGiven> describe(
    subjectDescription: String,
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    given: (suspend () -> RootGiven),
    function: ContextFunctionWithGiven<RootGiven>
): RootContextWithGiven<RootGiven> =
    RootContextWithGiven(
        subjectDescription,
        ignored,
        order,
        isolation,
        given = given,
        function = function
    )

fun describe(
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    function: ContextFunction
): RootContext =
    RootContext("root", ignored, order, isolation, addClassName = true, function = function)

fun <RootGiven> describe(
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    given: (suspend () -> RootGiven),
    function: ContextFunctionWithGiven<RootGiven>
): RootContextWithGiven<RootGiven> =
    RootContextWithGiven(
        "root",
        ignored,
        order,
        isolation,
        addClassName = true,
        given = given,
        function = function
    )

@JvmName("describe2")
inline fun <reified T> describe(
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    noinline function: ContextFunction
): RootContext = describe(typeOf<T>(), ignored, order, isolation, function)

fun describe(
    subjectType: KType,
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    function: ContextFunction
): RootContext =
    RootContext(subjectType.niceString(), ignored, order, isolation, function = function)

fun describe(
    subjectType: KClass<*>,
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    function: ContextFunction
): RootContext =
    RootContext("${subjectType.simpleName}", ignored, order, isolation, function = function)

suspend inline fun <reified Class> ContextDSL<*>.describe(
    tags: Set<String> = setOf(),
    isolation: Boolean? = null,
    ignored: Ignored? = null,
    noinline contextFunction: ContextFunction
) = this.describe(Class::class.simpleName!!, tags, isolation, ignored, contextFunction)
