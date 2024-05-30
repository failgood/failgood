package failgood.internal.util

import kotlin.reflect.KClass
import kotlin.reflect.KType

internal fun KType.niceString(): String {
    val kt = this
    return try {
        buildString {
            append((kt.classifier as KClass<*>).simpleName)
            if (kt.arguments.isNotEmpty()) {
                append(
                    kt.arguments.joinToString(prefix = "<", postfix = ">") {
                        (with(it.type) {
                            if (this == null) "*" else (this.classifier as KClass<*>).simpleName!!
                        })
                    }
                )
            }
        }
    } catch (e: Exception) {
        this.toString()
    }
}
