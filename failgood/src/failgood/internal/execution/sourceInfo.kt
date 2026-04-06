package failgood.internal.execution

import failgood.SourceInfo

internal actual fun sourceInfo(): SourceInfo {
    val first =
        RuntimeException().stackTrace.first {
            !(it.fileName?.let { fileName ->
                fileName.endsWith("ContextVisitor.kt") ||
                    fileName.endsWith("TestCollectionExecutor.kt") ||
                    fileName.endsWith("sourceInfo.kt") ||
                    fileName.endsWith("ContextDSL.kt")
            } ?: true)
        }
    return first.let { SourceInfo(it.className, it.fileName!!, it.lineNumber) }
}
