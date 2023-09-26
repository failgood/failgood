fun assert(b: Boolean) {
    if (!b)
        throw AssertionError()
}
