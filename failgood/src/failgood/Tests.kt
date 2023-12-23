package failgood

import failgood.dsl.ContextFunction
import failgood.dsl.ContextFunctionWithGiven
import failgood.internal.util.niceString
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * An unnamed collection of tests. It will get the name of the test class file in the reports. No root given is defined
 */
fun tests(
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    function: ContextFunction
) = tests(ignored, order, isolation, {}, function)

/** An unnamed collection of tests. It will get the name of the test class file in the reports. */
fun <RootGiven> tests(
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    given: suspend () -> RootGiven,
    function: ContextFunctionWithGiven<RootGiven>
) = RootContextWithGiven(
    "root",
    ignored,
    order,
    isolation,
    addClassName = true,
    given = given,
    function = function
)

/**
 * A collection of tests about a subject.
 * The Test name will be prefixed to the description to make it easy to see where the tests are defined
 * no give is defined
 */
fun testsAbout(
    description: String,
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    function: ContextFunction
) = testsAbout(description, ignored, order, isolation, {}, function)

/**
 * A collection of tests about a subject.
 * The Test name will be prefixed to the description to make it easy to see where the tests are defined
 */
fun <RootGiven> testsAbout(
    subjectDescription: String,
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    given: suspend () -> RootGiven,
    function: ContextFunctionWithGiven<RootGiven>
) = RootContextWithGiven(
    subjectDescription,
    ignored,
    order,
    isolation,
    addClassName = true,
    given = given,
    function = function
)

/**
 * A collection of tests about a class.
 * No give is defined
 */
fun testsAbout(
    subjectType: KClass<*>,
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    function: ContextFunction
) = testsAbout(subjectType, ignored, order, isolation, {}, function)

/**
 * A collection of tests about a class.
 */
fun <RootGiven> testsAbout(
    subjectType: KClass<*>,
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    given: suspend () -> RootGiven,
    function: ContextFunctionWithGiven<RootGiven>
) = RootContextWithGiven(
    "${subjectType.simpleName}",
    ignored,
    order,
    isolation,
    addClassName = true,
    given = given,
    function = function
)
