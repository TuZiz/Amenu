package cc.keer.amenu.platform

fun interface TaskHandle {
    fun cancel()

    companion object {
        @JvmField
        val NOOP: TaskHandle = TaskHandle {}
    }
}
