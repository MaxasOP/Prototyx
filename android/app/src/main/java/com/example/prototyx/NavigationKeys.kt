package com.example.prototyx

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Main : NavKey
@Serializable data object Optimizer : NavKey
@Serializable data object RiskMesh : NavKey
@Serializable data object Committee : NavKey
@Serializable data object Earnings : NavKey

