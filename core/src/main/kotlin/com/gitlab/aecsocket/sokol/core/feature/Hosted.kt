package com.gitlab.aecsocket.sokol.core.feature

import com.gitlab.aecsocket.sokol.core.ItemHolder
import com.gitlab.aecsocket.sokol.core.ItemHost
import com.gitlab.aecsocket.sokol.core.TreeState

class HostCreationException(message: String? = null, cause: Throwable? = null) : RuntimeException(message, cause)

interface ItemHoster<H : ItemHolder, T : TreeState, R : ItemHost> {
    fun itemHosted(holder: H, state: T): R
}
