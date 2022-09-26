package com.gitlab.aecsocket.sokol.core.extension

import com.gitlab.aecsocket.alexandria.core.physics.Quaternion
import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.core.physics.Vector3
import com.gitlab.aecsocket.sokol.core.NBTTag
import com.gitlab.aecsocket.sokol.core.NBTTagContext

fun NBTTagContext.makeVector3(value: Vector3) = makeDoubleArray(doubleArrayOf(value.x, value.y, value.z))

fun NBTTag.asVector3() = asDoubleArray().run { Vector3(get(0), get(1), get(2)) }

fun NBTTagContext.makeQuaternion(value: Quaternion) = makeDoubleArray(doubleArrayOf(value.x, value.y, value.z, value.w))

fun NBTTag.asQuaternion() = asDoubleArray().run { Quaternion(get(0), get(1), get(2), get(3)) }

private const val TRANSLATION = "translation"
private const val ROTATION = "rotation"

fun NBTTagContext.makeTransform(value: Transform) = makeCompound()
    .set(TRANSLATION) { makeVector3(value.translation) }
    .set(ROTATION) { makeQuaternion(value.rotation) }

fun NBTTag.asTransform() = asCompound().run { Transform(
    get(TRANSLATION) { asVector3() },
    get(ROTATION) { asQuaternion() },
) }
