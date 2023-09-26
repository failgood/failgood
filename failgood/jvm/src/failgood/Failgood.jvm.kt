package failgood

import failgood.internal.execution.context.sourceInfo

actual fun getSourceInfo(): failgood.SourceInfo = sourceInfo()
