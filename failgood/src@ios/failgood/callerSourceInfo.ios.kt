package failgood

// Kotlin/Native does not expose a portable caller lookup here.
internal actual fun callerSourceInfo(): SourceInfo = SourceInfo("unknown", null, -1)
