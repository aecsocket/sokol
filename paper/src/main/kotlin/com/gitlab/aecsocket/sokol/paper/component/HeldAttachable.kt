package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.extension.with
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.SokolAPI

object HeldAttachable : SimplePersistentComponent {
    override val key = SokolAPI.key("held_attachable")
    override val componentType get() = HeldAttachable::class
    val Type = ComponentType.singletonComponent(key, this)
}

@All(HeldAttachable::class, Held::class)
class HeldAttachableSystem(ids: ComponentIdAccess) : SokolSystem {
    companion object {
        val Attach = HeldAttachable.key.with("attach")
    }


}
