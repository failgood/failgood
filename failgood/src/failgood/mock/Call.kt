package failgood.mock

import kotlin.reflect.KCallable
import kotlin.reflect.KFunction1
import kotlin.reflect.KFunction2
import kotlin.reflect.KFunction3
import kotlin.reflect.KFunction4
import kotlin.reflect.KFunction5
import kotlin.reflect.KFunction6
import kotlin.reflect.KSuspendFunction1
import kotlin.reflect.KSuspendFunction2
import kotlin.reflect.KSuspendFunction3
import kotlin.reflect.KSuspendFunction4
import kotlin.reflect.KSuspendFunction5
import kotlin.reflect.KSuspendFunction6

/*
 * these are necessary to work with classes that have overloaded methods
 */
fun <A, B> call(kFunction1: KFunction1<A, B>): FunctionCall<A> =
    FunctionCall((kFunction1 as KCallable<*>).name, listOf())

@JvmName("callSuspend")
fun <A, B> call(kFunction1: KSuspendFunction1<A, B>): FunctionCall<A> =
    FunctionCall((kFunction1 as KCallable<*>).name, listOf())

fun <A, B, C> call(kFunction2: KFunction2<A, B, C>, b: B): FunctionCall<A> =
    FunctionCall((kFunction2 as KCallable<*>).name, listOf(b))

@JvmName("callSuspend")
fun <A, B, C> call(kFunction2: KSuspendFunction2<A, B, C>, b: B): FunctionCall<A> =
    FunctionCall((kFunction2 as KCallable<*>).name, listOf(b))

fun <A, B, C, D> call(kFunction3: KFunction3<A, B, C, D>, b: B, c: C): FunctionCall<A> =
    FunctionCall((kFunction3 as KCallable<*>).name, listOf(b, c))

@JvmName("callSuspend")
fun <A, B, C, D> call(kFunction3: KSuspendFunction3<A, B, C, D>, b: B, c: C): FunctionCall<A> =
    FunctionCall((kFunction3 as KCallable<*>).name, listOf(b, c))

fun <A, B, C, D, E> call(kFunction4: KFunction4<A, B, C, D, E>, b: B, c: C, d: D): FunctionCall<A> =
    FunctionCall((kFunction4 as KCallable<*>).name, listOf(b, c, d))

@JvmName("callSuspend")
fun <A, B, C, D, E> call(
    kFunction4: KSuspendFunction4<A, B, C, D, E>,
    b: B,
    c: C,
    d: D
): FunctionCall<A> = FunctionCall((kFunction4 as KCallable<*>).name, listOf(b, c, d))

fun <A, B, C, D, E, F> call(
    kFunction5: KFunction5<A, B, C, D, E, F>,
    b: B,
    c: C,
    d: D,
    e: E
): FunctionCall<A> = FunctionCall((kFunction5 as KCallable<*>).name, listOf(b, c, d, e))

@JvmName("callSuspend")
fun <A, B, C, D, E, F> call(
    kFunction5: KSuspendFunction5<A, B, C, D, E, F>,
    b: B,
    c: C,
    d: D,
    e: E
): FunctionCall<A> = FunctionCall((kFunction5 as KCallable<*>).name, listOf(b, c, d, e))

fun <A, B, C, D, E, F, G> call(
    kFunction6: KFunction6<A, B, C, D, E, F, G>,
    b: B,
    c: C,
    d: D,
    e: E,
    f: F
): FunctionCall<A> = FunctionCall((kFunction6 as KCallable<*>).name, listOf(b, c, d, e, f))

@JvmName("callSuspend")
fun <A, B, C, D, E, F, G> call(
    kFunction6: KSuspendFunction6<A, B, C, D, E, F, G>,
    b: B,
    c: C,
    d: D,
    e: E,
    f: F
): FunctionCall<A> = FunctionCall((kFunction6 as KCallable<*>).name, listOf(b, c, d, e, f))

@Suppress("UNCHECKED_CAST")
@JvmName("getCalls3")
fun <A, B, C, D> List<FunctionCall<A>>.getCalls(
    function: KFunction3<A, B, C, D>
): List<Pair<B, C>> {
    return this.filter { it.function == function.name }
        .map { Pair(it.arguments[0], it.arguments[1]) } as List<Pair<B, C>>
}

@JvmName("getCalls2")
@Suppress("UNCHECKED_CAST")
fun <A, B, C> List<FunctionCall<A>>.getCalls(function: KFunction2<A, B, C>): List<B> =
    this.filter { it.function == function.name }.map { it.arguments.single() } as List<B>

@Suppress("UNCHECKED_CAST")
@JvmName("suspendGetCalls3")
fun <A, B, C, D> List<FunctionCall<A>>.getCalls(
    function: KSuspendFunction3<A, B, C, D>
): List<Pair<B, C>> {
    return this.filter { it.function == function.name }
        .map { Pair(it.arguments[0], it.arguments[1]) } as List<Pair<B, C>>
}

@JvmName("suspendGetCalls2")
@Suppress("UNCHECKED_CAST")
fun <A, B, C> List<FunctionCall<A>>.getCalls(function: KSuspendFunction2<A, B, C>): List<B> =
    this.filter { it.function == function.name }.map { it.arguments.single() } as List<B>
