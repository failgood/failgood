package failgood.internal

// An exception that we use like a goto. to finish execution of a dsl block when we
// don't need to execute the rest of the dsl.
// This exception is caught with an empty catch block.
open class DSLGotoException : RuntimeException()
