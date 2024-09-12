@file:Suppress("unused")

package failgood

import failgood.dsl.ContextDSL
import failgood.dsl.ContextFunction
import failgood.dsl.ContextFunctionWithGiven
import failgood.internal.util.niceString
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@Deprecated(
    message = "moved to failgood.dsl",
    replaceWith = ReplaceWith("ContextDSL", "failgood.dsl.ContextDSL"))
typealias ContextDSL<T> = ContextDSL<T>

@Deprecated(
    message = "moved to failgood.dsl",
    replaceWith = ReplaceWith("ResourcesDSL", "failgood.dsl.ResourcesDSL"))
typealias ResourcesDSL = failgood.dsl.ResourcesDSL

@Deprecated(message = "renamed", replaceWith = ReplaceWith("TestCollection<Unit>"))
typealias RootContext = TestCollection<Unit>

@Deprecated(message = "renamed", replaceWith = ReplaceWith("TestCollection<Given>"))
typealias RootContextWithGiven<Given> = TestCollection<Given>

@Deprecated(
    message = "renamed",
    replaceWith =
        ReplaceWith(
            "TestCollection(name, ignored, order, isolation, sourceInfo, addClassName, given, function)"))
fun <RootGiven> RootContextWithGiven(
    name: String = "root",
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    sourceInfo: SourceInfo = callerSourceInfo(),
    addClassName: Boolean = false,
    given: (suspend () -> RootGiven),
    function: ContextFunctionWithGiven<RootGiven>
): TestCollection<RootGiven> =
    TestCollection(name, ignored, order, isolation, sourceInfo, addClassName, given, function)

@Deprecated(
    "going away before 1.0",
    replaceWith = ReplaceWith("testCollection(ignored, order, isolation, function)"))
fun tests(
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    function: ContextFunction
): TestCollection<Unit> = testCollection(ignored, order, isolation, {}, function)

/** An unnamed collection of tests. It will get the name of the test class file in the reports. */
@Deprecated(
    "going away before 1.0",
    replaceWith = ReplaceWith("testCollection(ignored, order, isolation, given, function)"))
fun <RootGiven> tests(
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    given: suspend () -> RootGiven,
    function: ContextFunctionWithGiven<RootGiven>
): TestCollection<RootGiven> =
    TestCollection(
        "root", ignored, order, isolation, addClassName = true, given = given, function = function)

/**
 * A collection of tests about a subject. The Test name will be prefixed to the description to make
 * it easy to see where the tests are defined no give is defined
 */
@Deprecated(
    "going away before 1.0",
    replaceWith = ReplaceWith("testCollection(description, ignored, order, isolation, function)"))
fun testsAbout(
    description: String,
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    function: ContextFunction
): TestCollection<Unit> = testCollection(description, ignored, order, isolation, {}, function)

/**
 * A collection of tests about a subject. The Test name will be prefixed to the description to make
 * it easy to see where the tests are defined
 */
@Deprecated(
    "going away before 1.0",
    replaceWith =
        ReplaceWith(
            "testCollection(subjectDescription, ignored, order, isolation, given, function)"))
fun <RootGiven> testsAbout(
    subjectDescription: String,
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    given: suspend () -> RootGiven,
    function: ContextFunctionWithGiven<RootGiven>
): TestCollection<RootGiven> =
    TestCollection(
        subjectDescription,
        ignored,
        order,
        isolation,
        addClassName = true,
        given = given,
        function = function)

/** A collection of tests about a class. No give is defined */
@Deprecated(
    "going away before 1.0",
    replaceWith = ReplaceWith("testCollection(subjectType, ignored, order, isolation, function)"))
fun testsAbout(
    subjectType: KClass<*>,
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    function: ContextFunction
): TestCollection<Unit> = testCollection(subjectType, ignored, order, isolation, {}, function)

/** A collection of tests about a class. */
@Deprecated(
    "going away before 1.0",
    replaceWith =
        ReplaceWith("testCollection(subjectType, ignored, order, isolation, given, function)"))
fun <RootGiven> testsAbout(
    subjectType: KClass<*>,
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    given: suspend () -> RootGiven,
    function: ContextFunctionWithGiven<RootGiven>
): TestCollection<RootGiven> =
    TestCollection(
        "${subjectType.simpleName}",
        ignored,
        order,
        isolation,
        addClassName = true,
        given = given,
        function = function)

@Deprecated(
    "going away before 1.0",
    replaceWith =
        ReplaceWith("testCollection(subjectDescription, ignored, order, isolation, function)"))
fun describe(
    subjectDescription: String,
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    function: ContextFunction
): TestCollection<Unit> = testCollection(subjectDescription, ignored, order, isolation, function)

@Deprecated(
    "going away before 1.0",
    replaceWith =
        ReplaceWith(
            "testCollection(subjectDescription, ignored, order, isolation, given, function)"))
fun <RootGiven> describe(
    subjectDescription: String,
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    given: (suspend () -> RootGiven),
    function: ContextFunctionWithGiven<RootGiven>
): TestCollection<RootGiven> =
    testCollection(subjectDescription, ignored, order, isolation, given, function)

@Deprecated(
    "going away before 1.0",
    replaceWith = ReplaceWith("testCollection(ignored, order, isolation, function)"))
fun describe(
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    function: ContextFunction
): TestCollection<Unit> = testCollection(ignored, order, isolation, function)

@Deprecated(
    "going away before 1.0",
    replaceWith = ReplaceWith("testCollection(ignored, order, isolation, given, function)"))
fun <RootGiven> describe(
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    given: (suspend () -> RootGiven),
    function: ContextFunctionWithGiven<RootGiven>
): TestCollection<RootGiven> = testCollection(ignored, order, isolation, given, function)

@JvmName("describe2")
@Deprecated(
    "going away before 1.0",
    replaceWith = ReplaceWith("testCollection(T::class, ignored, order, isolation, function)"))
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
    replaceWith = ReplaceWith("testCollection(subjectType, ignored, order, isolation, function)"))
fun describe(
    subjectType: KClass<*>,
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    function: ContextFunction
): TestCollection<Unit> = testCollection(subjectType, ignored, order, isolation, function)

@Deprecated(
    "This is going away in 0.9.1 because it causes problems for given support.",
    replaceWith =
        ReplaceWith(
            "this.describe(Class::class.simpleName!!, tags, isolation, ignored, contextFunction)"))
suspend inline fun <reified Class> ContextDSL<*>.describe(
    tags: Set<String> = setOf(),
    isolation: Boolean? = null,
    ignored: Ignored? = null,
    noinline contextFunction: ContextFunction
) = this.describe(Class::class.simpleName!!, tags, isolation, ignored, contextFunction)
