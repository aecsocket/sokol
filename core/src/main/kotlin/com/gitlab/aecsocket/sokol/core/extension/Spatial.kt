package com.gitlab.aecsocket.sokol.core.extension

import com.gitlab.aecsocket.alexandria.core.physics.Quaternion
import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.core.physics.Vector3
import com.jme3.math.TransformDp
import com.simsilica.mathd.Quatd
import com.simsilica.mathd.Vec3d

fun Vector3.bullet() = Vec3d(x, y, z)
fun Vec3d.alexandria() = Vector3(x, y, z)

fun Quaternion.bullet() = Quatd(x, y, z, w)
fun Quatd.alexandria() = Quaternion(x, y, z, w)

fun Transform.bullet() = TransformDp(position.bullet(), rotation.bullet())
fun TransformDp.alexandria() = Transform(translation.alexandria(), rotation.alexandria())
