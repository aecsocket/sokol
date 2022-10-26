package com.gitlab.aecsocket.sokol.core

import com.gitlab.aecsocket.sokol.core.util.Bits
import com.gitlab.aecsocket.sokol.core.util.MutableBag
import com.gitlab.aecsocket.sokol.core.util.emptyBag
import org.spongepowered.configurate.BasicConfigurationNode
import java.lang.invoke.MethodHandles
import java.util.*
import kotlin.Comparator
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.reflect.KClass
import kotlin.reflect.full.*
import kotlin.reflect.jvm.javaMethod

interface SokolSystem

@Target(AnnotationTarget.CLASS)
annotation class All(vararg val types: KClass<out SokolComponent>)

@Target(AnnotationTarget.CLASS)
annotation class One(vararg val types: KClass<out SokolComponent>)

@Target(AnnotationTarget.CLASS)
annotation class None(vararg val types: KClass<out SokolComponent>)

@Target(AnnotationTarget.CLASS)
annotation class Priority(val value: Int)

@Target(AnnotationTarget.FUNCTION)
annotation class Subscribe

const val PRIORITY_EARLIEST = -10000

const val PRIORITY_EARLY = -1000

const val PRIORITY_NORMAL = 0

const val PRIORITY_LATE = 1000

const val PRIORITY_LATEST = 10000

class SystemExecutionException(message: String? = null, cause: Throwable? = null)
    : RuntimeException(message, cause)

interface ComponentMapper<C : SokolComponent> {
    fun has(components: ComponentMap): Boolean

    fun mapOr(components: ComponentMap): C?

    fun map(components: ComponentMap): C
}

fun <C : SokolComponent> ComponentMapper<C>.has(entity: SokolEntity) = has(entity.components)

fun <C : SokolComponent> ComponentMapper<C>.mapOr(entity: SokolEntity) = mapOr(entity.components)

fun <C : SokolComponent> ComponentMapper<C>.map(entity: SokolEntity) = map(entity.components)


class SokolEngine internal constructor(
    private val componentIds: Map<KClass<out SokolComponent>, Int>
) {
    data class EntityFilter(
        val all: Bits,
        val one: Bits,
        val none: Bits,
    )

    data class SystemDefinition(
        val system: SokolSystem,
        val filter: EntityFilter,
        val priority: Int,
        internal val eventListeners: Map<KClass<out SokolEvent>, (SokolEvent, SokolEntity) -> Unit>,
    )

    private val systems = ArrayList<SystemDefinition>()

    private val handleLookup = MethodHandles.publicLookup()

    fun componentId(type: KClass<out SokolComponent>) = componentIds[type]
        ?: throw IllegalArgumentException("Component type $type is not registered")

    fun emptyComponentMap(): MutableComponentMap =
        ComponentMapImpl(arrayOfNulls(componentIds.size))

    fun componentMap(components: Collection<SokolComponent>): MutableComponentMap =
        emptyComponentMap().apply {
            components.forEach { set(it) }
        }

    fun emptyBlueprint(profile: EntityProfile): EntityBlueprint {
        val components = profile.componentProfiles.map { (_, profile) ->
            profile.read(BasicConfigurationNode.root())
        }
        return EntityBlueprint(profile, componentMap(components))
    }

    fun entityFilter(
        all: Iterable<KClass<out SokolComponent>>,
        one: Iterable<KClass<out SokolComponent>>,
        none: Iterable<KClass<out SokolComponent>>,
    ): EntityFilter {
        fun bitsOf(set: Iterable<KClass<out SokolComponent>>) = Bits(componentIds.size).apply {
            set.forEach { set(componentId(it)) }
        }

        return EntityFilter(bitsOf(all), bitsOf(one), bitsOf(none))
    }

    fun applies(filter: EntityFilter, archetype: Bits) =
        archetype.containsAll(filter.all)
                && (filter.one.isEmpty() || filter.one.intersects(archetype))
                && !filter.none.intersects(archetype)

    fun define(system: SokolSystem): SystemDefinition {
        val systemType = system::class

        val all = systemType.findAnnotation<All>()?.types?.asIterable() ?: emptySet()
        val one = systemType.findAnnotation<One>()?.types?.asIterable() ?: emptySet()
        val none = systemType.findAnnotation<None>()?.types?.asIterable() ?: emptySet()
        val priority = systemType.findAnnotation<Priority>()?.value ?: 0

        val eventListeners = HashMap<KClass<out SokolEvent>, (SokolEvent, SokolEntity) -> Unit>()
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
                val eventClass = eventType.classifier as? KClass<out SokolEvent>
                    ?: error("Event type $eventType is not instance of ${KClass::class}")

                if (eventListeners.contains(eventClass))
                    error("Duplicate event listener for type $eventClass")

                val handle = try {
                    handleLookup.unreflect(function.javaMethod
                        ?: error("Function cannot be expressed as Java method"))
                } catch (ex: Exception) {
                    error("Could not make method handle", ex)
                }.bindTo(system)

                eventListeners[eventClass] = { event, entity -> handle.invoke(event, entity) }
            }
        }

        return SystemDefinition(system, entityFilter(all, one, none), priority, eventListeners)
    }

    fun addSystem(definition: SystemDefinition) {
        systems.add(definition)
        systems.sortBy { it.priority }
    }

    fun buildEntity(blueprint: EntityBlueprint): SokolEntity {
        val entity = EntityImpl(blueprint.profile, blueprint.components.mutableCopy())
        entity.call(SokolEvent.Populate)
        return entity
    }

    fun <C : SokolComponent> componentMapper(type: KClass<out C>): ComponentMapper<C> =
        ComponentMapperImpl(componentId(type), type.java.name)

    private inner class ComponentMapperImpl<C : SokolComponent>(
        private val id: Int,
        private val typeName: String
    ) : ComponentMapper<C> {
        override fun has(components: ComponentMap) = components.has(id)

        @Suppress("UNCHECKED_CAST")
        override fun mapOr(components: ComponentMap) = components.get(id) as? C

        override fun map(components: ComponentMap) = mapOr(components)
            ?: throw SystemExecutionException("Entity does not have component type $typeName")
    }

    private inner class ComponentMapImpl(
        private val components: Array<SokolComponent?>,
        val archetype: Bits = Bits(componentIds.size).apply {
            components.forEach { component -> component?.let { set(componentId(it.componentType)) } }
        }
    ) : MutableComponentMap {
        override fun archetype() = Bits(archetype)

        override fun all() = components.filterNotNull()

        override fun has(id: Int) = archetype[id]

        override fun has(type: KClass<out SokolComponent>) = archetype[componentId(type)]

        override fun get(id: Int) = components[id]

        @Suppress("UNCHECKED_CAST")
        override fun <C : SokolComponent> get(type: KClass<out C>) =
            components[componentId(type)] as? C

        override fun set(component: SokolComponent) {
            val id = componentId(component.componentType)
            components[id] = component
            archetype.set(id)
        }

        override fun removeById(id: Int) {
            components[id] = null
            archetype.clear(id)
        }

        override fun removeByType(type: KClass<out SokolComponent>) {
            removeById(componentId(type))
        }

        override fun mutableCopy() = ComponentMapImpl(components.copyOf(), Bits(archetype))
    }

    private inner class EntityImpl(
        override val profile: EntityProfile,
        override val components: MutableComponentMap
    ) : SokolEntity {
        override fun <E : SokolEvent> call(event: E): E {
            val eventType = event::class
            systems.forEach { system ->
                if (applies(system.filter, components.archetype())) {
                    system.eventListeners.forEach { (type, listener) ->
                        if (eventType.isSubclassOf(type)) {
                            listener(event, this)
                        }
                    }
                }
            }
            return event
        }
    }

    class Builder {
        private val systemFactories = ArrayList<(SokolEngine) -> SystemDefinition>()
        private val componentTypes = ArrayList<KClass<out SokolComponent>>()

        fun countSystemFactories() = systemFactories.size
        fun countComponentTypes() = componentTypes.size

        fun systemFactory(factory: (SokolEngine) -> SystemDefinition): Builder {
            systemFactories.add(factory)
            return this
        }

        fun componentType(type: KClass<out SokolComponent>): Builder {
            componentTypes.add(type)
            return this
        }

        fun build(): SokolEngine {
            val engine = SokolEngine(componentTypes.mapIndexed { idx, type -> type to idx }.toMap())
            systemFactories.forEach { factory ->
                engine.addSystem(factory(engine))
            }
            return engine
        }
    }
}

inline fun <reified C : SokolComponent> SokolEngine.componentId() = componentId(C::class)

inline fun <reified C : SokolComponent> SokolEngine.componentMapper() = componentMapper(C::class)

inline fun <reified C : SokolComponent> SokolEngine.Builder.componentType() = componentType(C::class)
