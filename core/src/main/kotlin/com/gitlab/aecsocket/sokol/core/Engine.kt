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
    val entityId: Int

    var flags: Int

    val archetype: Bits

    fun allComponents(): List<SokolComponent>

    fun hasComponent(componentId: Int): Boolean

    fun getComponent(componentId: Int): SokolComponent?

    fun setComponent(componentId: Int, component: SokolComponent)

    fun removeComponent(componentId: Int)

    fun <E : SokolEvent> callSingle(event: E): E
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

    fun typeByOr(id: Int): KClass<out SokolComponent>?
}

fun ComponentIdAccess.idOf(type: KClass<out SokolComponent>) = idOfOr(type)
    ?: throw IllegalArgumentException("Component type ${type.simpleName ?: type} is not registered")

fun ComponentIdAccess.typeBy(id: Int) = typeByOr(id)
    ?: throw IllegalArgumentException("Component ID $id does not map to a type")

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
    val all: Bits,
    val one: Bits,
    val none: Bits
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

class ComponentTypeSet(val types: Iterable<KClass<out SokolComponent>>) {
    override fun toString() = "[${types.joinToString { it.simpleName ?: it.toString() }}]"
}

fun ComponentIdAccess.idsToTypes(ids: Bits): ComponentTypeSet {
    val types = ids.mapIndexedNotNull { idx, value ->
        if (value) typeBy(idx) else null
    }
    return ComponentTypeSet(types)
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

object ConstructEvent : SokolEvent

private typealias EventListener = (SokolEvent, SokolEntity) -> Unit

interface SokolSpaceAccess {
    val engine: SokolEngine

    val size: Int

    fun allEntities(): Bag<SokolEntity>

    fun getEntity(entityId: Int): SokolEntity?

    fun createEntity(flags: Int = 0): SokolEntity

    fun removeEntity(entityId: Int): SokolEntity

    fun withEntityFunction(function: (SokolEntity) -> Unit): SokolSpaceAccess
}

internal class WrappedSokolSpace(
    private val backing: SokolSpaceAccess,
    private val function: (SokolEntity) -> Unit
) : SokolSpaceAccess by backing {
    override fun createEntity(flags: Int): SokolEntity {
        val entity = backing.createEntity(flags)
        function(entity)
        return entity
    }

    override fun withEntityFunction(function: (SokolEntity) -> Unit) = WrappedSokolSpace(this, function)
}

class SokolSpace internal constructor(
    override val engine: SokolEngine,
    capacity: Int
) : SokolSpaceAccess {
    private val nComponentTypes = engine.countComponentTypes()
    private val components = Array<MutableBag<SokolComponent?>>(nComponentTypes) { emptyBag(capacity) }
    private val entities = emptyBag<SokolEntity>(capacity)
    private val freeIds = IntDeque()
    private var nextId = 0

    private val mComposite = engine.mapper<Composite>()

    private fun nextId() = if (freeIds.isEmpty()) nextId++ else freeIds.popFirst()

    override val size get() = entities.size

    override fun allEntities(): Bag<SokolEntity> = entities

    override fun getEntity(entityId: Int) = entities[entityId]

    override fun createEntity(flags: Int): SokolEntity {
        val entityId = nextId()
        val entity = EntityImpl(entityId, flags, Bits(nComponentTypes))
        entities[entityId] = entity
        return entity
    }

    override fun removeEntity(entityId: Int): SokolEntity {
        val entity = entities.removeAt(entityId)
        components.forEach { bag ->
            bag.removeAt(entityId)
        }
        return entity
    }

    private fun callInternal(event: SokolEvent, entities: Iterable<SokolEntity>) {
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
            entities.forEach { root ->
                fun callOn(target: SokolEntity) {
                    if (filter.matches(target.archetype)) {
                        listeners.forEach { it(event, target) }
                    }

                    // call on children of this entity as well
                    mComposite.getOr(target)?.forEach { callOn(it) }
                }

                callOn(root)
            }
        }
    }

    fun <E : SokolEvent> call(event: E): E {
        callInternal(event, entities)
        return event
    }

    override fun withEntityFunction(function: (SokolEntity) -> Unit): SokolSpaceAccess =
        WrappedSokolSpace(this, function)

    private inner class EntityImpl(
        override val entityId: Int,
        override var flags: Int,
        override var archetype: Bits
    ) : SokolEntity {
        override fun allComponents() = archetype.mapIndexedNotNull { componentId, value ->
            if (value) components[componentId][entityId] else null
        }

        override fun hasComponent(componentId: Int) = archetype[componentId]

        override fun getComponent(componentId: Int) = components[componentId][entityId]

        override fun setComponent(componentId: Int, component: SokolComponent) {
            components[componentId][entityId] = component
            archetype.set(componentId)
        }

        override fun removeComponent(componentId: Int) {
            components[componentId][entityId] = null
            archetype.clear(componentId)
        }

        override fun <E : SokolEvent> callSingle(event: E): E {
            callInternal(event, setOf(this))
            return event
        }

        override fun toString() = "Entity[${allComponents().joinToString { it.componentType.simpleName ?: it.componentType.toString() }}]"
    }
}

fun SokolSpace.construct() = call(ConstructEvent)

private typealias SystemFactory = (ComponentIdAccess) -> SokolSystem

class SokolEngine internal constructor(
    private val componentIds: Map<KClass<out SokolComponent>, Int>,
    private val idToType: Array<KClass<out SokolComponent>>,
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

    fun systems() = systems.map { it.system }

    override fun idOfOr(type: KClass<out SokolComponent>) = componentIds[type]

    override fun typeByOr(id: Int) = idToType[id]

    fun emptySpace(capacity: Int = 64) = SokolSpace(this, capacity)

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

        fun countComponentTypes() = componentTypes.size

        fun countSystemFactories() = systemFactories.size

        @Suppress("UnstableApiUsage")
        fun build(): SokolEngine {
            val componentIds = componentTypes.mapIndexed { idx, type -> type to idx }.associate { it }
            val idToType = componentIds.map { (type) -> type }.toTypedArray()
            val idAccess = object : ComponentIdAccess {
                override fun countComponentTypes() = componentIds.size
                override fun idOfOr(type: KClass<out SokolComponent>) = componentIds[type]
                override fun typeByOr(id: Int) = idToType[id]
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

            return SokolEngine(componentIds, idToType, systemGraph.topologicallySorted().toList())
        }
    }
}
