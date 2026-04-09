package failgood.android

import dalvik.system.DexFile
import failgood.Test
import java.util.Enumeration

internal fun interface AndroidTestClassFinder {
    fun findTestClasses(): List<String>
}

internal fun interface AndroidClassNameFinder {
    fun findClassNames(): List<String>
}

internal class ReflectiveAndroidTestClassFinder(
    private val classLoader: ClassLoader,
    private val classNameFinder: AndroidClassNameFinder,
) : AndroidTestClassFinder {
    override fun findTestClasses(): List<String> =
        classNameFinder.findClassNames().filter(::isFailgoodTestClass).sorted()

    private fun isFailgoodTestClass(className: String): Boolean {
        val clazz =
            try {
                classLoader.loadClass(className)
            } catch (_: ClassNotFoundException) {
                return false
            } catch (_: LinkageError) {
                return false
            }
        return clazz.isAnnotationPresent(Test::class.java)
    }
}

internal class ApkClassNameFinder(private val apkPath: String) : AndroidClassNameFinder {
    override fun findClassNames(): List<String> {
        val dexFile = DexFile(apkPath)
        try {
            return dexFile.entries().toList()
        } finally {
            dexFile.close()
        }
    }
}

private fun Enumeration<String>.toList(): List<String> {
    val classNames = mutableListOf<String>()
    while (hasMoreElements()) {
        classNames += nextElement()
    }
    return classNames
}
