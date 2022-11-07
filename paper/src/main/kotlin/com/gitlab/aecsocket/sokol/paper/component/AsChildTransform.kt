package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.core.extension.asTransform
import com.gitlab.aecsocket.sokol.core.extension.makeTransform
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

private const val RELATIVE = "relative"

data class AsChildTransform(
    val profile: Profile,
    private val dRelative: Delta<Transform>,
) : PersistentComponent {
    companion object {
        val Key = SokolAPI.key("as_child_transform")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = AsChildTransform::class
    override val key get() = Key

    override val dirty get() = dRelative.dirty

    constructor(
        profile: Profile,
        relative: Transform
    ) : this(profile, Delta(relative))

    var relative by dRelative

    fun fullTransform() = profile.transform + relative

    override fun write(ctx: NBTTagContext) = ctx.makeCompound()
        .set(RELATIVE) { makeTransform(relative) }

    override fun writeDelta(tag: NBTTag): NBTTag {
        val compound = tag.asCompound()
        dRelative.ifDirty { compound.set(RELATIVE) { makeTransform(it) } }
        return tag
    }

    override fun write(node: ConfigurationNode) {
        node.node(RELATIVE).set(relative)
    }

    @ConfigSerializable
    data class Profile(
        @Setting(nodeFromParent = true) val transform: Transform
    ) : ComponentProfile {
        override fun read(tag: NBTTag) = tag.asCompound { compound -> AsChildTransform(this,
            compound.getOr(RELATIVE) { asTransform() } ?: Transform.Identity) }

        override fun read(node: ConfigurationNode) = AsChildTransform(this,
            node.node(RELATIVE).get { Transform.Identity })

        override fun readEmpty() = AsChildTransform(this, Transform.Identity)
    }
}

class AsChildTransformForwardSystem(mappers: ComponentIdAccess) : SokolSystem {
    private val mComposite = mappers.componentMapper<Composite>()

    @Subscribe
    fun on(event: SokolEvent.Reset, entity: SokolEntity) {
        mComposite.forwardAll(entity, Reset)
    }

    @Subscribe
    fun on(event: SokolEvent.Remove, entity: SokolEntity) {
        mComposite.forwardAll(entity, Reset)
    }

    object Reset : SokolEvent
}

@All(AsChildTransform::class)
class AsChildTransformSystem(mappers: ComponentIdAccess) : SokolSystem {
    private val mAsChildTransform = mappers.componentMapper<AsChildTransform>()

    @Subscribe
    fun on(event: AsChildTransformForwardSystem.Reset, entity: SokolEntity) {
        val asChildTransform = mAsChildTransform.get(entity)
        asChildTransform.relative = Transform.Identity
    }
}
