package failgood

import failgood.dsl.ContextFunction
import failgood.dsl.ContextFunctionWithGiven
import kotlin.reflect.KClass

/**
 * An unnamed collection of tests. It will get the name of the test class file in the reports. No root given is defined
 */
fun testCollection(
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    function: ContextFunction
): TestCollection<Unit> = testCollection(ignored, order, isolation, {}, function)

/** An unnamed collection of tests. It will get the name of the test class file in the reports. */
fun <RootGiven> testCollection(
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    given: suspend () -> RootGiven,
    function: ContextFunctionWithGiven<RootGiven>
): TestCollection<RootGiven> = TestCollection(
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
fun testCollection(
    description: String,
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    function: ContextFunction
): TestCollection<Unit> = testCollection(description, ignored, order, isolation, {}, function)

/**
 * A collection of tests about a subject.
 * The Test name will be prefixed to the description to make it easy to see where the tests are defined
 */
fun <RootGiven> testCollection(
    subjectDescription: String,
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    given: suspend () -> RootGiven,
    function: ContextFunctionWithGiven<RootGiven>
): TestCollection<RootGiven> = TestCollection(
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
fun testCollection(
    subjectType: KClass<*>,
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    function: ContextFunction
): TestCollection<Unit> = testCollection(subjectType, ignored, order, isolation, {}, function)

/**
 * A collection of tests about a class.
 */
fun <RootGiven> testCollection(
    subjectType: KClass<*>,
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    given: suspend () -> RootGiven,
    function: ContextFunctionWithGiven<RootGiven>
): TestCollection<RootGiven> = TestCollection(
    "${subjectType.simpleName}",
    ignored,
    order,
    isolation,
    addClassName = true,
    given = given,
    function = function
)
