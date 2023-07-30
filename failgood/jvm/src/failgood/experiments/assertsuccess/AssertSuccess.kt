package failgood.experiments.assertsuccess

import failgood.mock.FunctionCall
import kotlin.reflect.KSuspendFunction2

sealed interface CheckResult<T> {
    data class Success<T>(val result: T) : CheckResult<T>
    data class Failure<T>(val errorMessage: String) : CheckResult<T>
}

fun <T> assert(result: CheckResult<T>): T {
    return when (result) {
        is CheckResult.Success<T> -> result.result
        is CheckResult.Failure -> throw AssertionError(result.errorMessage)
    }
}

fun <THIS, P1, R> FunctionCall.isCallTo(function: KSuspendFunction2<THIS, P1, R>): CheckResult<P1> {
    @Suppress("UNCHECKED_CAST")
    return if (this.function == function.name)
        CheckResult.Success(this.arguments[0] as P1)
    else
        CheckResult.Failure("expected a call to ${function.name} but was ${this.function}")
}
