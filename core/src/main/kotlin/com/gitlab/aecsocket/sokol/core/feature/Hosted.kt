package com.gitlab.aecsocket.sokol.core.feature

import com.gitlab.aecsocket.sokol.core.DataNode
import com.gitlab.aecsocket.sokol.core.ItemHolder
import com.gitlab.aecsocket.sokol.core.ItemHost
import com.gitlab.aecsocket.sokol.core.TreeState

class HostCreationException(message: String? = null, cause: Throwable? = null) : RuntimeException(message, cause)

interface ItemHoster<N : DataNode, H : ItemHolder<N>, T : TreeState, R : ItemHost<N>> {
    fun itemHosted(holder: H, state: T): R
}
