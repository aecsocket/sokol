package com.gitlab.aecsocket.sokol.core

import com.gitlab.aecsocket.sokol.core.util.*
import com.google.common.graph.GraphBuilder
import java.lang.invoke.MethodHandles
import kotlin.reflect.KClass
import kotlin.reflect.full.*
import kotlin.reflect.jvm.javaMethod

interface SokolComponent {
    val componentType: KClass<out SokolComponent>
}

interface EntitySpace {
    val engine: SokolEngine

    fun entities(): Iterable<SokolEntity>
}

private class EntitySpaceImpl(
    override val engine: SokolEngine,
    val entities: Iterable<SokolEntity>
) : EntitySpace {
    override fun entities() = entities
}

fun entitySpaceOf(engine: SokolEngine, entities: Iterable<SokolEntity>): EntitySpace = EntitySpaceImpl(engine, entities)

class EntityCollection internal constructor(
    override val engine: SokolEngine,
    collection: MutableCollection<SokolEntity>
) : EntitySpace, MutableCollection<SokolEntity> by collection {
    override fun entities() = this
}

fun <E : SokolEvent> EntitySpace.call(event: E): E {
    val eventType = event::class.java

    // find all listeners for this event
    val systems = engine.systemsForEvent.computeIfAbsent(eventType) {
        engine.systems.mapNotNull {
            val listeners = it.listeners
                .filter { (listenedType) -> listenedType.isAssignableFrom(eventType) }
                .map { (_, listener) -> listener }
            if (listeners.isEmpty()) null
            else SokolEngine.EventSystem(it, listeners)
        }
    }

    // process all systems
    systems.forEach { (system, listeners) ->
        if (!system.system.processing) return@forEach

        val filter = system.filter
        entities().forEach { entity ->
            if (filter.matches(entity.archetype)) {
                listeners.forEach { it(event, entity) }
            }
        }
    }

    return event
}

class SokolEntity internal constructor(
    override val engine: SokolEngine,
    var flags: Int,
    val archetype: Bits = Bits(engine.countComponentTypes()),
) : EntitySpace {
    private val _components = arrayOfNulls<SokolComponent>(engine.countComponentTypes())
    val components get() = _components.filterNotNull()

    override fun entities() = setOf(this)

    fun hasComponent(componentId: Int) = archetype[componentId]

    fun getComponent(componentId: Int) = _components[componentId]

    fun setComponent(componentId: Int, component: SokolComponent) {
        _components[componentId] = component
        archetype.set(componentId)
    }

    fun removeComponent(componentId: Int) {
        _components[componentId] = null
        archetype.clear(componentId)
    }

    fun hasFlag(flag: Int) = flags and flag != 0

    fun setFlag(flag: Int) {
        flags = flags or flag
    }

    fun clearFlag(flag: Int) {
        flags = flags and flag.inv()
    }

    fun setFlag(flag: Int, value: Boolean) {
        if (value) setFlag(flag) else clearFlag(flag)
    }

    fun clearFlags() {
        flags = 0
    }

    override fun toString() = "Entity[${_components.filterNotNull().joinToString { it.componentType.simpleName ?: it.componentType.toString() }}]"
}

inline fun <reified C : SokolComponent> SokolEntity.hasComponent() = hasComponent(engine.idOf<C>())

inline fun <reified C : SokolComponent> SokolEntity.componentOr() = getComponent(engine.idOf<C>()) as C?

inline fun <reified C : SokolComponent> SokolEntity.component() = getComponent(engine.idOf<C>()) as C

interface ComponentIdAccess {
    fun countComponentTypes(): Int

    fun idOfOr(type: KClass<out SokolComponent>): Int?

    fun typeByOr(id: Int): KClass<out SokolComponent>?

    fun <C : SokolComponent> mapper(type: KClass<C>): ComponentMapper<C>
}

fun ComponentIdAccess.idOf(type: KClass<out SokolComponent>) = idOfOr(type)
    ?: throw IllegalArgumentException("Component type ${type.simpleName ?: type} is not registered")

fun ComponentIdAccess.typeBy(id: Int) = typeByOr(id)
    ?: throw IllegalArgumentException("Component ID $id does not map to a type")

inline fun <reified C : SokolComponent> ComponentIdAccess.idOf() = idOf(C::class)

inline fun <reified C : SokolComponent> ComponentIdAccess.mapper() = mapper(C::class)

fun ComponentIdAccess.idsToTypes(ids: Bits): ComponentTypeSet {
    val types = ids.mapIndexedNotNull { idx, value ->
        if (value) typeBy(idx) else null
    }
    return ComponentTypeSet(types)
}

fun ComponentIdAccess.filter(
    all: Iterable<KClass<out SokolComponent>> = emptySet(),
    one: Iterable<KClass<out SokolComponent>> = emptySet(),
    none: Iterable<KClass<out SokolComponent>> = emptySet(),
): ArchetypeFilter {
    fun bitsOf(types: Iterable<KClass<out SokolComponent>>): Bits {
        val bits = Bits(countComponentTypes())
        types.forEach {
            bits.set(idOf(it))
        }
        return bits
    }

    return ArchetypeFilter(bitsOf(all), bitsOf(one), bitsOf(none))
}


class ComponentMapper<C : SokolComponent> internal constructor(
    private val id: Int,
    type: KClass<C>
) {
    private val typeName = type.simpleName ?: type.toString()

    fun has(entity: SokolEntity) = entity.hasComponent(id)

    @Suppress("UNCHECKED_CAST")
    fun getOr(entity: SokolEntity) = entity.getComponent(id) as? C

    fun get(entity: SokolEntity) = getOr(entity)
        ?: throw IllegalStateException("Entity does not have component $typeName")

    fun set(entity: SokolEntity, component: C) {
        entity.setComponent(id, component)
    }

    fun getOrSet(entity: SokolEntity, provider: () -> C): C {
        return getOr(entity) ?: run {
            val component = provider()
            set(entity, component)
            return component
        }
    }

    fun remove(entity: SokolEntity) {
        entity.removeComponent(id)
    }
}

data class ArchetypeFilter internal constructor(
    val all: Bits,
    val one: Bits,
    val none: Bits
) {
    fun matches(archetype: Bits) = archetype.containsAll(all)
        && (one.isEmpty() || one.intersects(archetype))
        && (none.isEmpty() || !none.intersects(archetype))
}

class ComponentTypeSet(private val types: Iterable<KClass<out SokolComponent>>) {
    override fun toString() = "[${types.joinToString { it.simpleName ?: it.toString() }}]"
}

interface SokolSystem {
    val processing: Boolean get() = true
}

@Target(AnnotationTarget.CLASS)
annotation class All(vararg val types: KClass<out SokolComponent>)

@Target(AnnotationTarget.CLASS)
annotation class One(vararg val types: KClass<out SokolComponent>)

@Target(AnnotationTarget.CLASS)
annotation class None(vararg val types: KClass<out SokolComponent>)

@Target(AnnotationTarget.CLASS)
annotation class Before(vararg val types: KClass<out SokolSystem>)

@Target(AnnotationTarget.CLASS)
annotation class After(vararg val types: KClass<out SokolSystem>)

@Target(AnnotationTarget.FUNCTION)
annotation class Subscribe

interface SokolEvent

private typealias EventListener = (SokolEvent, SokolEntity) -> Unit

typealias EngineSystemFactory = (ComponentIdAccess) -> SokolSystem

object ConstructEvent : SokolEvent

fun EntitySpace.construct() = call(ConstructEvent)

open class BaseComponentIdAccess(
    private val idToType: List<KClass<out SokolComponent>>
) : ComponentIdAccess {
    private val typeToId = idToType.mapIndexed { idx, type -> type to idx }.associate { it }
    private val mappers = Array<ComponentMapper<*>>(idToType.size) { ComponentMapper(it, idToType[it]) }

    override fun countComponentTypes() = idToType.size
    override fun idOfOr(type: KClass<out SokolComponent>) = typeToId[type]
    override fun typeByOr(id: Int) = idToType[id]
    @Suppress("UNCHECKED_CAST")
    override fun <C : SokolComponent> mapper(type: KClass<C>) = mappers[idOf(type)] as ComponentMapper<C>
}

class SokolEngine internal constructor(
    private val idAccess: ComponentIdAccess,
    internal val systems: List<SystemDefinition>,
) : ComponentIdAccess by idAccess {
    internal data class SystemDefinition(
        val system: SokolSystem,
        val filter: ArchetypeFilter,
        val listeners: Map<Class<out SokolEvent>, EventListener>,
    )

    internal data class EventSystem(
        val system: SystemDefinition,
        val listeners: List<EventListener>
    )

    internal val systemsForEvent = HashMap<Class<out SokolEvent>, List<EventSystem>>()

    fun systems() = systems.map { it.system }

    fun entity(flags: Int = 0) = SokolEntity(this, flags)

    fun spaceOf(entities: Iterable<SokolEntity>): EntitySpace = EntitySpaceImpl(this, entities)

    fun spaceOf(entity: SokolEntity): EntitySpace = EntitySpaceImpl(this, setOf(entity))

    fun emptySpace(size: Int = 64) = EntityCollection(this, ArrayList(size))

    class Builder {
        private val componentClass = ArrayList<KClass<out SokolComponent>>()
        private val systemFactories = ArrayList<EngineSystemFactory>()
        private val handleLookup = MethodHandles.lookup()

        fun componentClass(type: KClass<out SokolComponent>): Builder {
            if (componentClass.contains(type))
                throw IllegalArgumentException("Duplicate component class $type")
            componentClass.add(type)
            return this
        }

        inline fun <reified C : SokolComponent> componentClass() = componentClass(C::class)

        fun systemFactory(factory: EngineSystemFactory): Builder {
            systemFactories.add(factory)
            return this
        }

        fun countComponentTypes() = componentClass.size

        fun countSystemFactories() = systemFactories.size

        @Suppress("UnstableApiUsage")
        fun build(): SokolEngine {
            val idAccess = BaseComponentIdAccess(componentClass)

            data class SystemData(
                val definition: SystemDefinition,
                val before: Set<KClass<out SokolSystem>>,
                val after: Set<KClass<out SokolSystem>>
            )

            val systemGraph = GraphBuilder.directed().build<SystemDefinition>()
            val systems = systemFactories.associate { factory ->
                val system = factory(idAccess)
                val systemType = system::class

                val all = systemType.findAnnotation<All>()?.types?.toSet() ?: emptySet()
                val one = systemType.findAnnotation<One>()?.types?.toSet() ?: emptySet()
                val none = systemType.findAnnotation<None>()?.types?.toSet() ?: emptySet()
                val before = systemType.findAnnotation<Before>()?.types?.toSet() ?: emptySet()
                val after = systemType.findAnnotation<After>()?.types?.toSet() ?: emptySet()

                val listeners = HashMap<Class<out SokolEvent>, EventListener>()
                systemType.functions.forEach { function ->
                    if (function.hasAnnotation<Subscribe>()) {
                        val funcName = function.name
                        fun error(message: String, cause: Throwable? = null): Nothing =
                            throw IllegalArgumentException("${systemType.qualifiedName}.${funcName}(${function.parameters.joinToString { it.type.toString() }}) -> ${function.returnType}: $message", cause)

                        val params = function.parameters
                        if (params.size != 3)
                            error("Event listener must have parameters (event, entity)")

                        val eventType = params[1].type
                        if (!eventType.isSubtypeOf(SokolEvent::class.createType()))
                            error("Event type must extend ${SokolEvent::class}")

                        @Suppress("UNCHECKED_CAST")
                        val eventClass = (eventType.classifier as? KClass<out SokolEvent>)?.java
                            ?: error("Event type $eventType is not instance of ${KClass::class}")

                        if (listeners.contains(eventClass))
                            error("Duplicate event listener for type $eventClass")

                        val handle = try {
                            handleLookup.unreflect(function.javaMethod
                                ?: error("Function cannot be expressed as Java method"))
                        } catch (ex: Exception) {
                            error("Could not make method handle", ex)
                        }.bindTo(system)

                        listeners[eventClass] = { event, entity -> handle.invoke(event, entity) }
                    }
                }

                val definition = SystemDefinition(
                    system,
                    idAccess.filter(all, one, none),
                    listeners
                )
                systemGraph.addNode(definition)
                systemType to SystemData(definition, before, after)
            }

            fun target(system: KClass<out SokolSystem>, target: KClass<out SokolSystem>) = systems[target]
                ?: throw IllegalArgumentException("System ${system.simpleName} references ${target.simpleName}, which is not registered")

            systems.forEach { (systemType, system) ->
                system.after.forEach { targetType ->
                    val target = target(systemType, targetType)
                    if (target.after.contains(systemType))
                        throw IllegalArgumentException("Cyclical dependency: ${targetType.simpleName} and ${systemType.simpleName} execute after each other")
                    systemGraph.putEdge(target.definition, system.definition)
                }

                system.before.forEach { targetType ->
                    val target = target(systemType, targetType)
                    if (target.before.contains(systemType))
                        throw IllegalArgumentException("Cyclical dependency: ${targetType.simpleName} and ${systemType.simpleName} execute before each other")
                    systemGraph.putEdge(system.definition, target.definition)
                }
            }

            return SokolEngine(idAccess, systemGraph.topologicallySorted().toList())
        }
    }
}
