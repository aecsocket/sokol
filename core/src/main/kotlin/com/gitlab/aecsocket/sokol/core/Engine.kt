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

interface SokolEntity {
    var flags: Int

    fun allComponents(): List<SokolComponent>

    fun hasComponent(id: Int): Boolean

    fun getComponent(id: Int): SokolComponent?

    fun setComponent(id: Int, component: SokolComponent)

    fun removeComponent(id: Int)
}

fun SokolEntity.hasFlag(flag: Int) = flags and flag != 0

fun SokolEntity.setFlag(flag: Int) {
    flags = flags or flag
}

fun SokolEntity.clearFlag(flag: Int) {
    flags = flags and flag.inv()
}

fun SokolEntity.setFlag(flag: Int, value: Boolean) {
    if (value) setFlag(flag) else clearFlag(flag)
}

fun SokolEntity.clearFlags() {
    flags = 0
}

interface ComponentIdAccess {
    fun countComponentTypes(): Int

    fun idOfOr(type: KClass<out SokolComponent>): Int?
}

fun ComponentIdAccess.idOf(type: KClass<out SokolComponent>) = idOfOr(type)
    ?: throw IllegalArgumentException("Component type ${type.simpleName ?: type} is not registered")

inline fun <reified C : SokolComponent> ComponentIdAccess.idOf() = idOf(C::class)

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

    fun remove(entity: SokolEntity) {
        entity.removeComponent(id)
    }
}

fun <C : SokolComponent> ComponentIdAccess.mapper(type: KClass<C>) = ComponentMapper(idOf(type), type)

inline fun <reified C : SokolComponent> ComponentIdAccess.mapper() = mapper(C::class)

data class ArchetypeFilter internal constructor(
    private val all: Bits,
    private val one: Bits,
    private val none: Bits
) {
    fun matches(archetype: Bits) = archetype.containsAll(all)
        && (one.isEmpty() || one.intersects(archetype))
        && (none.isEmpty() || !none.intersects(archetype))
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

interface ArchetypeFilterBuilder {
    fun all(vararg types: KClass<out SokolComponent>): ArchetypeFilterBuilder

    fun one(vararg types: KClass<out SokolComponent>): ArchetypeFilterBuilder

    fun none(vararg types: KClass<out SokolComponent>): ArchetypeFilterBuilder

    fun build(): ArchetypeFilter
}

fun ComponentIdAccess.filter() = object : ArchetypeFilterBuilder {
    var all: Iterable<KClass<out SokolComponent>> = emptySet()
    var one: Iterable<KClass<out SokolComponent>> = emptySet()
    var none: Iterable<KClass<out SokolComponent>> = emptySet()

    override fun all(vararg types: KClass<out SokolComponent>): ArchetypeFilterBuilder {
        all = types.asIterable()
        return this
    }

    override fun one(vararg types: KClass<out SokolComponent>): ArchetypeFilterBuilder {
        one = types.asIterable()
        return this
    }

    override fun none(vararg types: KClass<out SokolComponent>): ArchetypeFilterBuilder {
        none = types.asIterable()
        return this
    }

    override fun build() = filter(all, one, none)
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

interface SokolEvent {
    // when the entity is created from scratch
    object Construct : SokolEvent

    // when the entity is about to be persisted
    object Destruct : SokolEvent
}

private typealias EventListener = (SokolEvent, SokolEntity) -> Unit

/*
examples of spaces:
- a space for all worlds, chunks, mobs and tile entities
    this is because all their tag writes are independent
- a space for all items inside a shulker box
    once all writes for the held items are done, the shulker box can be written to
- a space for all items inside a chest
    same as shulker box but the chest block state can be written to
 */

class SokolSpace internal constructor(private val engine: SokolEngine, capacity: Int) {
    /*private val entities = emptyBag<SokolEntity>(capacity)
    private val freeIds = IntDeque()
    private var nextId = 0

    private val archetypes = HashMap<Archetype, Set<SokolEntity>>()

    private fun nextId() = if (freeIds.isEmpty()) nextId++ else freeIds.popFirst()

    fun allEntities(): Bag<SokolEntity> = entities

    fun addEntity(entity: SokolEntity): Int {
        val id = nextId()
        entities[id] = entity
        return id
    }

    fun removeEntity(id: Int) {
        entities.removeAt(id)
        freeIds.add(id)
    }

    fun removeAllEntities() {
        entities.clear()
        freeIds.clear()
        nextId = 0
    }

    fun entitiesFor(filter: ArchetypeFilter) = entities.filter { filter.matches(it.archetype) }

    fun <E : SokolEvent> call(event: E): E {
        val eventType = event::class.java

        val systems = engine.systemsForEvent.computeIfAbsent(eventType) {
            // find all listeners for this event
            engine.systems.mapNotNull {
                val listeners = it.listeners
                    .filter { (listenedType) -> listenedType.isAssignableFrom(eventType) }
                    .map { (_, listener) -> listener }
                if (listeners.isEmpty()) null
                else SokolEngine.EventSystem(it, listeners)
            }
        }

        systems.forEach { (system, listeners) ->
            // process systems
            if (!system.system.processing) return@forEach

            // TODO improve; compute entities for systems at add time
            entitiesFor(system.filter).forEach { entity ->
                listeners.forEach { it(event, entity) }
            }
        }

        return event
    }

    override fun toString() = "Space(${entities.size})"*/
}

private typealias SystemFactory = (ComponentIdAccess) -> SokolSystem

class SokolEngine internal constructor(
    private val componentIds: Map<KClass<out SokolComponent>, Int>,
    internal val systems: List<SystemDefinition>,
) : ComponentIdAccess {
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

    override fun countComponentTypes() = componentIds.size

    override fun idOfOr(type: KClass<out SokolComponent>) = componentIds[type]

    fun emptySpace(capacity: Int = 64) = SokolSpace(this, capacity)

    fun spaceOf(entities: Collection<SokolEntity>) = SokolSpace(this, entities.size).also { space ->
        entities.forEach { space.addEntity(it) }
    }

    class Builder {
        private val componentTypes = ArrayList<KClass<out SokolComponent>>()
        private val systemFactories = ArrayList<SystemFactory>()
        private val handleLookup = MethodHandles.lookup()

        fun componentType(type: KClass<out SokolComponent>): Builder {
            componentTypes.add(type)
            return this
        }

        inline fun <reified C : SokolComponent> componentType() = componentType(C::class)

        fun systemFactory(factory: SystemFactory): Builder {
            systemFactories.add(factory)
            return this
        }

        @Suppress("UnstableApiUsage")
        fun build(): SokolEngine {
            val componentIds = componentTypes.mapIndexed { idx, type -> type to idx }.associate { it }
            val idAccess = object : ComponentIdAccess {
                override fun countComponentTypes() = componentIds.size
                override fun idOfOr(type: KClass<out SokolComponent>) = componentIds[type]
            }

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
                ?: throw IllegalArgumentException("System ${system.simpleName} is referencing ${target.simpleName}, which is not registered")

            systems.forEach { (systemType, system) ->
                system.after.forEach { targetType ->
                    val target = target(systemType, targetType)
                    if (target.after.contains(systemType))
                        throw IllegalArgumentException("Cyclical dependency: ${targetType.simpleName} and ${systemType.simpleName} executing after each other")
                    systemGraph.putEdge(target.definition, system.definition)
                }

                system.before.forEach { targetType ->
                    val target = target(systemType, targetType)
                    if (target.before.contains(systemType))
                        throw IllegalArgumentException("Cyclical dependency: ${targetType.simpleName} and ${systemType.simpleName} executing before each other")
                    systemGraph.putEdge(system.definition, target.definition)
                }
            }

            return SokolEngine(componentIds, systemGraph.topologicallySorted().toList())
        }
    }
}
