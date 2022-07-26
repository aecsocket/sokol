package com.gitlab.aecsocket.sokol.core.feature

import com.gitlab.aecsocket.glossa.core.I18N
import com.gitlab.aecsocket.sokol.core.Feature
import com.gitlab.aecsocket.sokol.core.FeatureContext
import net.kyori.adventure.text.Component
import java.util.*

interface BaseFeature<
    S : Feature.State<S, D, C>,
    D : Feature.Data<S>,
        C : FeatureContext<*, *, *>
> : Feature.State<S, D, C> {
    fun <R> i18n(locale: Locale?, action: I18N<Component>.() -> R): R
}
