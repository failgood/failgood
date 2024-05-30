package failgood

data class SourceInfo(val className: String, val fileName: String?, val lineNumber: Int) {
    fun likeStackTrace(testName: String) =
        "$className.${testName.replace(" ", "-")}($fileName:$lineNumber)"
}
