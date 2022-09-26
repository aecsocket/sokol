package com.gitlab.aecsocket.sokol.core.extension

import com.gitlab.aecsocket.alexandria.core.physics.Quaternion
import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.core.physics.Vector3
import com.jme3.math.Vector3f

fun Vector3.bullet() = Vector3f(x.toFloat(), y.toFloat(), z.toFloat())
fun Vector3f.alexandria() = Vector3(x.toDouble(), y.toDouble(), z.toDouble())

fun Quaternion.bullet() = com.jme3.math.Quaternion(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())
fun com.jme3.math.Quaternion.alexandria() = Quaternion(x.toDouble(), y.toDouble(), z.toDouble(), w.toDouble())

fun Transform.bullet() = com.jme3.math.Transform(translation.bullet(), rotation.bullet())
fun com.jme3.math.Transform.alexandria() = Transform(translation.alexandria(), rotation.alexandria())
