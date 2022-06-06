package failgood.mock

import kotlin.reflect.*

/*
 * these are necessary to work with classes that have overloaded methods
 */
fun <A, B> call(kFunction1: KFunction1<A, B>): FunctionCall = FunctionCall((kFunction1 as KCallable<*>).name, listOf())

@JvmName("callSuspend")
fun <A, B> call(kFunction1: KSuspendFunction1<A, B>): FunctionCall =
    FunctionCall((kFunction1 as KCallable<*>).name, listOf())

fun <A, B, C> call(kFunction2: KFunction2<A, B, C>, b: B): FunctionCall =
    FunctionCall((kFunction2 as KCallable<*>).name, listOf(b))

@JvmName("callSuspend")
fun <A, B, C> call(kFunction2: KSuspendFunction2<A, B, C>, b: B): FunctionCall =
    FunctionCall((kFunction2 as KCallable<*>).name, listOf(b))

fun <A, B, C, D> call(kFunction3: KFunction3<A, B, C, D>, b: B, c: C): FunctionCall =
    FunctionCall((kFunction3 as KCallable<*>).name, listOf(b, c))

@JvmName("callSuspend")
fun <A, B, C, D> call(kFunction3: KSuspendFunction3<A, B, C, D>, b: B, c: C): FunctionCall =
    FunctionCall((kFunction3 as KCallable<*>).name, listOf(b, c))

fun <A, B, C, D, E> call(kFunction4: KFunction4<A, B, C, D, E>, b: B, c: C, d: D): FunctionCall =
    FunctionCall((kFunction4 as KCallable<*>).name, listOf(b, c, d))

@JvmName("callSuspend")
fun <A, B, C, D, E> call(kFunction4: KSuspendFunction4<A, B, C, D, E>, b: B, c: C, d: D): FunctionCall =
    FunctionCall((kFunction4 as KCallable<*>).name, listOf(b, c, d))

fun <A, B, C, D, E, F> call(kFunction5: KFunction5<A, B, C, D, E, F>, b: B, c: C, d: D, e: E): FunctionCall =
    FunctionCall((kFunction5 as KCallable<*>).name, listOf(b, c, d, e))

@JvmName("callSuspend")
fun <A, B, C, D, E, F> call(kFunction5: KSuspendFunction5<A, B, C, D, E, F>, b: B, c: C, d: D, e: E): FunctionCall =
    FunctionCall((kFunction5 as KCallable<*>).name, listOf(b, c, d, e))

fun <A, B, C, D, E, F, G> call(
    kFunction6: KFunction6<A, B, C, D, E, F, G>,
    b: B,
    c: C,
    d: D,
    e: E,
    f: F
): FunctionCall = FunctionCall((kFunction6 as KCallable<*>).name, listOf(b, c, d, e, f))

@JvmName("callSuspend")
fun <A, B, C, D, E, F, G> call(
    kFunction6: KSuspendFunction6<A, B, C, D, E, F, G>,
    b: B,
    c: C,
    d: D,
    e: E,
    f: F
): FunctionCall = FunctionCall((kFunction6 as KCallable<*>).name, listOf(b, c, d, e, f))
