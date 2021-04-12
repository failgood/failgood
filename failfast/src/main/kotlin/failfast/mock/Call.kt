package failfast.mock

import kotlin.reflect.KCallable
import kotlin.reflect.KFunction1
import kotlin.reflect.KFunction2
import kotlin.reflect.KFunction3
import kotlin.reflect.KFunction4
import kotlin.reflect.KFunction5
import kotlin.reflect.KFunction6
import kotlin.reflect.KSuspendFunction4

/*
 * these are necessary to work with classes that have overloaded methods
 */
fun <A, B> call(kFunction1: KFunction1<A, B>): FunctionCall = FunctionCall((kFunction1 as KCallable<*>).name, listOf())
fun <A, B, C> call(kFunction2: KFunction2<A, B, C>, b: B): FunctionCall =
    FunctionCall((kFunction2 as KCallable<*>).name, listOf(b))

fun <A, B, C, D> call(kFunction3: KFunction3<A, B, C, D>, b: B, c: C): FunctionCall =
    FunctionCall((kFunction3 as KCallable<*>).name, listOf(b, c))

fun <A, B, C, D, E> call(kFunction4: KFunction4<A, B, C, D, E>, b: B, c: C, d: D): FunctionCall =
    FunctionCall((kFunction4 as KCallable<*>).name, listOf(b, c, d))

@JvmName("call1")
fun <A, B, C, D, E> call(kFunction4: KSuspendFunction4<A, B, C, D, E>, b: B, c: C, d: D): FunctionCall =
    FunctionCall((kFunction4 as KCallable<*>).name, listOf(b, c, d))

fun <A, B, C, D, E, F> call(kFunction5: KFunction5<A, B, C, D, E, F>, b: B, c: C, d: D, e: E): FunctionCall =
    FunctionCall((kFunction5 as KCallable<*>).name, listOf(b, c, d, e))

fun <A, B, C, D, E, F, G> call(
    kFunction6: KFunction6<A, B, C, D, E, F, G>,
    b: B,
    c: C,
    d: D,
    e: E,
    f: F
): FunctionCall = FunctionCall((kFunction6 as KCallable<*>).name, listOf(b, c, d, e, f))
