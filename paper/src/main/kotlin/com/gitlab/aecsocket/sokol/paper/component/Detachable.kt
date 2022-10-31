package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.*
import com.gitlab.aecsocket.alexandria.paper.extension.*
import com.gitlab.aecsocket.craftbullet.core.physPosition
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.core.extension.bullet
import com.gitlab.aecsocket.sokol.paper.HoldAttach
import com.gitlab.aecsocket.sokol.paper.HoldPlaceState
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import com.jme3.bullet.collision.shapes.SphereCollisionShape
import com.jme3.bullet.objects.PhysicsGhostObject
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable

data class Detachable(val profile: Profile) : PersistentComponent {
    companion object {
        val Key = SokolAPI.key("detachable")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = Detachable::class
    override val key get() = Key

    override fun write(ctx: NBTTagContext) = ctx.makeCompound()

    override fun write(node: ConfigurationNode) {}

    @ConfigSerializable
    data class Profile(
        // todo detach axis
        val a: Boolean = true,
    ) : NonReadingComponentProfile {
        override fun readEmpty() = Detachable(this)
    }
}

@All(Detachable::class)
@Before(OnInputSystem::class)
class DetachableSystem(mappers: ComponentIdAccess) : SokolSystem {
    companion object {
        val Detach = SokolAPI.key("detachable/detach")
    }

    private val mDetachable = mappers.componentMapper<Detachable>()

    @Subscribe
    fun on(event: OnInputSystem.Build, entity: SokolEntity) {
        val detachable = mDetachable.get(entity)

//        event.addAction(Detach) { (_, _, cancel) ->
//
//        }
    }
}
