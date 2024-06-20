package failgood

import failgood.dsl.ContextDSL
import failgood.dsl.ContextFunction
import failgood.dsl.ContextFunctionWithGiven
import failgood.internal.util.niceString
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@Deprecated(
    "going away before 1.0",
    replaceWith = ReplaceWith("testCollection(subjectDescription, ignored, order, isolation, function)")
)
fun describe(
    subjectDescription: String,
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    function: ContextFunction
): TestCollection<Unit> =
    testCollection(subjectDescription, ignored, order, isolation, function)

@Deprecated(
    "going away before 1.0",
    replaceWith = ReplaceWith("testCollection(subjectDescription, ignored, order, isolation, given, function)")
)
fun <RootGiven> describe(
    subjectDescription: String,
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    given: (suspend () -> RootGiven),
    function: ContextFunctionWithGiven<RootGiven>
): TestCollection<RootGiven> =
    testCollection(subjectDescription, ignored, order, isolation, given, function)

@Deprecated("going away before 1.0", replaceWith = ReplaceWith("testCollection(ignored, order, isolation, function)"))
fun describe(
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    function: ContextFunction
): TestCollection<Unit> = testCollection(ignored, order, isolation, function)


@Deprecated(
    "going away before 1.0",
    replaceWith = ReplaceWith("testCollection(ignored, order, isolation, given, function)")
)
fun <RootGiven> describe(
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    given: (suspend () -> RootGiven),
    function: ContextFunctionWithGiven<RootGiven>
): TestCollection<RootGiven> =
    testCollection(ignored, order, isolation, given, function)


@JvmName("describe2")
@Deprecated(
    "going away before 1.0",
    replaceWith = ReplaceWith("testCollection(T::class, ignored, order, isolation, function)")
)
inline fun <reified T> describe(
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    noinline function: ContextFunction
): TestCollection<Unit> = describe(typeOf<T>(), ignored, order, isolation, function)

@PublishedApi
internal fun describe(
    subjectType: KType,
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    function: ContextFunction
): TestCollection<Unit> =
    TestCollection(subjectType.niceString(), ignored, order, isolation, function = function)

@Deprecated(
    "going away before 1.0",
    replaceWith = ReplaceWith("testCollection(subjectType, ignored, order, isolation, function)")
)
fun describe(
    subjectType: KClass<*>,
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    function: ContextFunction
): TestCollection<Unit> =
    testCollection(subjectType, ignored, order, isolation, function)


@Deprecated(
    "This is going away in 0.9.1 because it causes problems for given support.",
    replaceWith = ReplaceWith("this.describe(Class::class.simpleName!!, tags, isolation, ignored, contextFunction)")
)
suspend inline fun <reified Class> ContextDSL<*>.describe(
    tags: Set<String> = setOf(),
    isolation: Boolean? = null,
    ignored: Ignored? = null,
    noinline contextFunction: ContextFunction
) = this.describe(Class::class.simpleName!!, tags, isolation, ignored, contextFunction)
