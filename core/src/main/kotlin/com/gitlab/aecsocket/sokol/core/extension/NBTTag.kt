package com.gitlab.aecsocket.sokol.core.extension

import com.gitlab.aecsocket.alexandria.core.physics.Quaternion
import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.core.physics.Vector3
import com.gitlab.aecsocket.sokol.core.NBTTag

fun NBTTag.ofVector(value: Vector3) = ofDoubleArray(doubleArrayOf(value.x, value.y, value.z))

fun NBTTag.asVector() = asDoubleArray().run { Vector3(get(0), get(1), get(2)) }

fun NBTTag.ofQuaternion(value: Quaternion) = ofDoubleArray(doubleArrayOf(value.x, value.y, value.z, value.w))

fun NBTTag.asQuaternion() = asDoubleArray().run { Quaternion(get(0), get(1), get(2), get(3)) }

private const val TRANSLATION = "translation"
private const val ROTATION = "rotation"

fun NBTTag.ofTransform(value: Transform) = ofCompound()
    .set(TRANSLATION) { ofVector(value.translation) }
    .set(ROTATION) { ofQuaternion(value.rotation) }

fun NBTTag.asTransform() = asCompound().run { Transform(
    get(TRANSLATION) { asVector() },
    get(ROTATION) { asQuaternion() },
) }
