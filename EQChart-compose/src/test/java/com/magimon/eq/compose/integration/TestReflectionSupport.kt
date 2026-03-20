package com.magimon.eq.compose.integration

private val primitiveToWrapper = mapOf(
    Boolean::class.javaPrimitiveType to Boolean::class.java,
    Byte::class.javaPrimitiveType to Byte::class.java,
    Char::class.javaPrimitiveType to Char::class.java,
    Short::class.javaPrimitiveType to Short::class.java,
    Int::class.javaPrimitiveType to Int::class.java,
    Long::class.javaPrimitiveType to Long::class.java,
    Float::class.javaPrimitiveType to Float::class.java,
    Double::class.javaPrimitiveType to Double::class.java,
)

internal fun invokePrivateTopLevel(
    ownerClassName: String,
    methodName: String,
    vararg args: Any?,
): Any? {
    val owner = Class.forName(ownerClassName)
    val byNameAndCount = owner.declaredMethods.filter { candidate ->
        candidate.name == methodName && candidate.parameterCount == args.size
    }
    val method = when {
        byNameAndCount.size == 1 -> byNameAndCount.single()
        else -> byNameAndCount.singleOrNull { candidate ->
            candidate.parameterTypes.zip(args).all { (parameterType, arg) -> isCompatible(parameterType, arg) }
        } ?: error("Method not found: $ownerClassName#$methodName/${args.size}")
    }
    method.isAccessible = true
    return method.invoke(null, *args)
}

private fun isCompatible(parameterType: Class<*>, arg: Any?): Boolean {
    if (arg == null) return !parameterType.isPrimitive
    val effectiveType = if (parameterType.isPrimitive) {
        primitiveToWrapper[parameterType] ?: parameterType
    } else {
        parameterType
    }
    return effectiveType.isAssignableFrom(arg.javaClass)
}

@Suppress("UNCHECKED_CAST")
internal fun <T> Any.readField(name: String): T {
    var current: Class<*>? = javaClass
    while (current != null) {
        try {
            val field = current.getDeclaredField(name)
            field.isAccessible = true
            return field.get(this) as T
        } catch (_: NoSuchFieldException) {
            current = current.superclass
        }
    }
    error("Field not found: $name")
}
