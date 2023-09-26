package failgood.assert

actual fun assert(b: Boolean) {
    if (!b)
        throw AssertionError()
}
