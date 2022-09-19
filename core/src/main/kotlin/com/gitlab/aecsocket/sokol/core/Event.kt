package com.gitlab.aecsocket.sokol.core

interface SokolEvent

object UpdateEvent : SokolEvent

interface HostEvent : SokolEvent

object HostByItemEvent : HostEvent

object HostByEntityEvent : HostEvent
