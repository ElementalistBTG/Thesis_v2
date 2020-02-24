package com.example.thesis_new.helper

import java.io.Serializable

data class Wifi (
    val ssid: String,
    val bssid: String,
    val power: Int
): Serializable