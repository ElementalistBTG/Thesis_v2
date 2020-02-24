package com.example.thesis_new.helper


import java.io.Serializable

data class CloudAnchor(
    val shortCode: Int,
    val coordinates: String,
    val bearing: Double,
    val distance: Double
) : Serializable