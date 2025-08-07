interface FactoryFor<T> {
    fun fromLine(line: String): T
}