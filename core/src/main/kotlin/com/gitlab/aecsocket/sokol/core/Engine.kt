package com.gitlab.aecsocket.sokol.core

import com.gitlab.aecsocket.alexandria.core.keyed.Keyed
import com.gitlab.aecsocket.sokol.core.util.Bits
import com.gitlab.aecsocket.sokol.core.util.topologicallySorted
import com.google.common.graph.GraphBuilder
import org.spongepowered.configurate.BasicConfigurationNode
import java.lang.invoke.MethodHandles
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
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
annotation class Before(vararg val types: KClass<out SokolSystem>)

@Target(AnnotationTarget.CLASS)
annotation class After(vararg val types: KClass<out SokolSystem>)

@Target(AnnotationTarget.FUNCTION)
annotation class Subscribe

class SystemExecutionException(message: String? = null, cause: Throwable? = null)
    : RuntimeException(message, cause)

interface ComponentMapper<C : SokolComponent> {
    fun has(components: ComponentMap): Boolean

    fun getOr(components: ComponentMap): C?

    fun get(components: ComponentMap): C

    fun set(components: MutableComponentMap, component: C)

    fun remove(components: MutableComponentMap)
}

fun <C : SokolComponent> ComponentMapper<C>.has(holder: ComponentMapHolder) = has(holder.components)

fun <C : SokolComponent> ComponentMapper<C>.getOr(holder: ComponentMapHolder) = getOr(holder.components)

fun <C : SokolComponent> ComponentMapper<C>.get(holder: ComponentMapHolder) = get(holder.components)

fun <C : SokolComponent> ComponentMapper<C>.set(holder: ComponentMapHolder, component: C) = set(holder.components, component)

fun <C : SokolComponent> ComponentMapper<C>.remove(holder: ComponentMapHolder) = remove(holder.components)

fun <C : SokolComponent> componentMapperOf(id: Int, type: KClass<out SokolComponent>) = object : ComponentMapper<C> {
    val typeName = type.java.name

    override fun has(components: ComponentMap) = components.has(id)

    @Suppress("UNCHECKED_CAST")
    override fun getOr(components: ComponentMap) = components.get(id) as? C

    override fun get(components: ComponentMap) = getOr(components)
        ?: throw SystemExecutionException("Entity does not have component type $typeName")

    override fun set(components: MutableComponentMap, component: C) {
        components.set(id, component)
    }

    override fun remove(components: MutableComponentMap) {
        components.removeById(id)
    }
}

interface ComponentIdAccess {
    fun countComponentIds(): Int

    fun componentId(type: KClass<out SokolComponent>): Int
}

data class EntityFilter(
    val all: Bits,
    val one: Bits,
    val none: Bits,
)

fun ComponentIdAccess.entityFilter(
    all: Iterable<KClass<out SokolComponent>> = emptySet(),
    one: Iterable<KClass<out SokolComponent>> = emptySet(),
    none: Iterable<KClass<out SokolComponent>> = emptySet(),
): EntityFilter {
    fun bitsOf(set: Iterable<KClass<out SokolComponent>>) = Bits(countComponentIds()).apply {
        set.forEach { set(componentId(it)) }
    }

    return EntityFilter(bitsOf(all), bitsOf(one), bitsOf(none))
}

fun <C : SokolComponent> ComponentIdAccess.componentMapper(type: KClass<out C>) = componentMapperOf<C>(componentId(type), type)

inline fun <reified C : SokolComponent> ComponentIdAccess.componentMapper() = componentMapper(C::class)

open class AbstractComponentIdAccess(
    protected val componentIds: Map<KClass<out SokolComponent>, Int>
) : ComponentIdAccess {
    override fun countComponentIds() = componentIds.size

    override fun componentId(type: KClass<out SokolComponent>) = componentIds[type]
        ?: throw IllegalArgumentException("Component type $type is not registered")
}

class SokolEngine internal constructor(
    private val systems: List<SystemDefinition>,
    componentIds: Map<KClass<out SokolComponent>, Int>
) : AbstractComponentIdAccess(componentIds) {
    internal data class SystemDefinition(
        val system: SokolSystem,
        val filter: EntityFilter,
        val eventListeners: Map<KClass<out SokolEvent>, (SokolEvent, SokolEntity) -> Unit>,
    )

    fun systems() = systems.map { it.system }

    fun emptyComponentMap(): MutableComponentMap =
        ComponentMapImpl(arrayOfNulls(componentIds.size))

    fun componentMap(components: Collection<SokolComponent>): MutableComponentMap =
        emptyComponentMap().apply {
            components.forEach { set(it) }
        }

    private fun makeComponents(profile: EntityProfile) = profile.componentProfiles.map { (_, profile) ->
        profile.readEmpty()
    }

    fun emptyBlueprint(profile: EntityProfile): EntityBlueprint {
        return EntityBlueprint(profile, componentMap(makeComponents(profile)))
    }

    fun emptyKeyedBlueprint(profile: KeyedEntityProfile): KeyedEntityBlueprint {
        return KeyedEntityBlueprint(profile, componentMap(makeComponents(profile)))
    }

    fun applies(filter: EntityFilter, archetype: Bits) = archetype.containsAll(filter.all)
        && (filter.one.isEmpty() || filter.one.intersects(archetype))
        && !filter.none.intersects(archetype)

    fun applies(filter: EntityFilter, components: ComponentMap) = applies(filter, components.archetype())

    fun applies(filter: EntityFilter, holder: ComponentMapHolder) = applies(filter, holder.components)

    fun buildEntity(blueprint: EntityBlueprint): SokolEntity {
        val entity = EntityImpl(blueprint.profile, blueprint.components.mutableCopy())
        entity.call(SokolEvent.Populate)
        return entity
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

        override fun set(id: Int, component: SokolComponent) {
            components[id] = component
            archetype.set(id)
        }

        override fun set(component: SokolComponent) {
            set(componentId(component.componentType), component)
        }

        override fun removeById(id: Int) {
            components[id] = null
            archetype.clear(id)
        }

        override fun removeByType(type: KClass<out SokolComponent>) {
            removeById(componentId(type))
        }

        override fun mutableCopy() = ComponentMapImpl(components.copyOf(), Bits(archetype))

        override fun toString() = components.filterNotNull().joinToString(prefix = "[", postfix = "]") {
            it.componentType.simpleName ?: "?"
        }
    }

    data class SystemExecutionResult(
        val system: SokolSystem,
        val executed: Boolean,
    )

    data class EventCallResult(
        val event: SokolEvent,
        val systems: List<SystemExecutionResult>
    )

    inner class EntityImpl(
        override val profile: EntityProfile,
        override val components: MutableComponentMap
    ) : SokolEntity {
        // TODO this should probably be a toggle because it can be intensive?
        // or another class which doesn't do logging
        val callResults = ArrayList<EventCallResult>()

        override fun <E : SokolEvent> call(event: E): E {
            val eventType = event::class
            val systemResults = ArrayList<SystemExecutionResult>()
            val callResult = EventCallResult(event, systemResults)
            systems.forEach { system ->
                if (applies(system.filter, components.archetype())) {
                    systemResults.add(SystemExecutionResult(system.system, true))
                    system.eventListeners.forEach { (type, listener) ->
                        if (eventType.isSubclassOf(type)) {
                            listener(event, this)
                        }
                    }
                } else {
                    systemResults.add(SystemExecutionResult(system.system, false))
                }
            }
            callResults.add(callResult)
            return event
        }

        override fun toString() = "EntityImpl(${if (profile is Keyed) profile.id else "<anonymous>" })$components"
    }

    class Builder {
        private val systemFactories = ArrayList<(ComponentIdAccess) -> SokolSystem>()
        private val componentTypes = ArrayList<KClass<out SokolComponent>>()
        private val handleLookup = MethodHandles.publicLookup()

        fun countSystemFactories() = systemFactories.size
        fun countComponentTypes() = componentTypes.size

        fun systemFactory(factory: (ComponentIdAccess) -> SokolSystem): Builder {
            systemFactories.add(factory)
            return this
        }

        fun componentType(type: KClass<out SokolComponent>): Builder {
            componentTypes.add(type)
            return this
        }

        @Suppress("UnstableApiUsage")
        fun build(): SokolEngine {
            val componentIds = componentTypes.mapIndexed { idx, type -> type to idx }.toMap()
            val componentIdAccess = object : ComponentIdAccess {
                override fun countComponentIds() = componentIds.size

                override fun componentId(type: KClass<out SokolComponent>): Int {
                    return componentIds[type]
                        ?: throw IllegalArgumentException("Component type $type is not registered")
                }
            }

            data class SystemPreDefinition(
                val definition: SystemDefinition,
                val before: Set<KClass<out SokolSystem>>,
                val after: Set<KClass<out SokolSystem>>,
            )

            val dependencyGraph = GraphBuilder.directed().build<SystemDefinition>()
            val systems = systemFactories.associate { factory ->
                val system = factory(componentIdAccess)
                val systemType = system::class

                val all = systemType.findAnnotation<All>()?.types?.toSet() ?: emptySet()
                val one = systemType.findAnnotation<One>()?.types?.toSet() ?: emptySet()
                val none = systemType.findAnnotation<None>()?.types?.toSet() ?: emptySet()
                val before = systemType.findAnnotation<Before>()?.types?.toSet() ?: emptySet()
                val after = systemType.findAnnotation<After>()?.types?.toSet() ?: emptySet()

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

                val definition = SystemDefinition(system, componentIdAccess.entityFilter(all, one, none), eventListeners)
                dependencyGraph.addNode(definition)
                systemType to SystemPreDefinition(definition, before, after)
            }

            fun target(system: KClass<out SokolSystem>, target: KClass<out SokolSystem>) = systems[target]
                ?: throw IllegalArgumentException("System ${system.simpleName} is referencing ${target.simpleName}, which is not registered")

            systems.forEach { (_, system) ->
                val systemType = system.definition.system::class
                system.after.forEach { targetType ->
                    val target = target(systemType, targetType)
                    if (target.after.contains(systemType))
                        throw IllegalArgumentException("Cyclical dependency: ${targetType.simpleName} and ${systemType.simpleName} executing after each other")
                    dependencyGraph.putEdge(target.definition, system.definition)
                }

                system.before.forEach { targetType ->
                    val target = target(systemType, targetType)
                    if (target.before.contains(systemType))
                        throw IllegalArgumentException("Cyclical dependency: ${targetType.simpleName} and ${systemType.simpleName} executing before each other")
                    dependencyGraph.putEdge(system.definition, target.definition)
                }
            }

            val sorted = dependencyGraph.topologicallySorted().toList()
            return SokolEngine(sorted, componentIds)
        }
    }
}

inline fun <reified C : SokolComponent> SokolEngine.Builder.componentType() = componentType(C::class)
