package com.example.thesis_new.helper

import java.io.Serializable

data class Cell(
    val id: String,
    val signal_strength: Int
) : Serializable