package com.gitlab.aecsocket.sokol.core.extension

import com.gitlab.aecsocket.alexandria.core.physics.Matrix3
import com.gitlab.aecsocket.alexandria.core.physics.Quaternion
import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.core.physics.Vector3
import com.jme3.math.Matrix3f
import com.jme3.math.TransformDp
import com.simsilica.mathd.Quatd
import com.simsilica.mathd.Vec3d

fun Vector3.bullet() = Vec3d(x, y, z)
fun Vec3d.alexandria() = Vector3(x, y, z)

fun Quaternion.bullet() = Quatd(x, y, z, w)
fun Quatd.alexandria() = Quaternion(x, y, z, w)

fun Transform.bullet() = TransformDp(position.bullet(), rotation.bullet())
fun TransformDp.alexandria() = Transform(translation.alexandria(), rotation.alexandria())

fun Matrix3.bulletSp() = Matrix3f(
    n00.toFloat(), n01.toFloat(), n02.toFloat(),
    n10.toFloat(), n11.toFloat(), n12.toFloat(),
    n20.toFloat(), n21.toFloat(), n22.toFloat()
)
fun Matrix3f.alexandria() = Matrix3(
    get(0, 0).toDouble(), get(0, 1).toDouble(), get(0, 2).toDouble(),
    get(1, 0).toDouble(), get(1, 1).toDouble(), get(1, 2).toDouble(),
    get(2, 0).toDouble(), get(2, 1).toDouble(), get(2, 2).toDouble()
)
