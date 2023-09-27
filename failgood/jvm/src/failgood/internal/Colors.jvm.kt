package failgood.internal

actual fun detectWindows() = System.getProperty("os.name").startsWith("Windows")
