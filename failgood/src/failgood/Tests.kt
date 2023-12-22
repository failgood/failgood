package failgood

import failgood.dsl.ContextFunction
import failgood.dsl.ContextFunctionWithGiven
import kotlin.reflect.KClass

fun <RootGiven> tests(
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
    given = given,
    function = function
)


fun tests(
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    function: ContextFunction
) = RootContext(
    "root",
    ignored,
    order,
    isolation,
    addClassName = true,
    function = function
)

fun tests(
    subjectDescription: String,
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    function: ContextFunction
) = RootContext(
    subjectDescription,
    ignored,
    order,
    isolation,
    addClassName = true,
    function = function
)

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

fun tests(
    subjectType: KClass<*>,
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    function: ContextFunction
) = RootContext("${subjectType.simpleName}", ignored, order, isolation, function = function)

