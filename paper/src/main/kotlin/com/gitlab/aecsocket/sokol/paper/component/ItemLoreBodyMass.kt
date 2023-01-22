package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.extension.with
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.glossa.core.I18N
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.Sokol
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import com.gitlab.aecsocket.sokol.paper.persistentComponent
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Required
import org.spongepowered.configurate.objectmapping.meta.Setting
import kotlin.reflect.KClass

data class ItemLoreBodyMass(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("item_lore_body_mass")
        val Type = ComponentType.deserializing(Key, Profile::class)

        fun init(ctx: Sokol.InitContext) {
            ctx.persistentComponent(Type)
            ctx.system { ItemLoreBodyMassSystem(it).init(ctx) }
        }
    }

    override val componentType get() = ItemLoreBodyMass::class
    override val key get() = Key

    @ConfigSerializable
    data class Profile(
        @Required val baseKey: String
    ) : SimpleComponentProfile<ItemLoreBodyMass> {
        override val componentType get() = ItemLoreBodyMass::class

        override fun createEmpty() = ComponentBlueprint { ItemLoreBodyMass(this) }
    }
}

private const val SINGLE = "single"
private const val MULTIPLE = "multiple"

class ItemLoreBodyMassSystem(ids: ComponentIdAccess) : SokolSystem {
    companion object {
        val Lore = ItemLoreBodyMass.Key.with("lore")
    }

    private val mItemLoreBodyMass = ids.mapper<ItemLoreBodyMass>()
    private val mColliderRigidBody = ids.mapper<ColliderRigidBody>()
    private val mIsChild = ids.mapper<IsChild>()
    private val mComposite = ids.mapper<Composite>()

    private fun ItemLoreBodyMass.Profile.key(path: String) = "$baseKey.$path"

    internal fun init(ctx: Sokol.InitContext): ItemLoreBodyMassSystem {
        ctx.components.itemLoreManager.apply {
            provider(Lore, ::lore)
        }
        return this
    }

    private fun lore(entity: SokolEntity, i18n: I18N<Component>): List<Component> {
        val itemLoreBodyMass = mItemLoreBodyMass.getOr(entity)?.profile ?: return emptyList()

        val thisMass = mColliderRigidBody.getOr(entity)?.profile?.mass ?: 0.0
        val all = mComposite.all(mIsChild.root(entity))
        return if (all.size == 1) {
            i18n.safe(itemLoreBodyMass.key(SINGLE)) {
                icu("this_mass", thisMass)
            }
        } else {
            val totalMass = all.sumOf { mColliderRigidBody.getOr(it)?.profile?.mass ?: 0.0 }
            i18n.safe(itemLoreBodyMass.key(MULTIPLE)) {
                icu("this_mass", thisMass)
                icu("total_mass", totalMass)
            }
        }
    }
}
