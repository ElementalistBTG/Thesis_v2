package com.example.thesis_new.viewModel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

class MapperActivityViewModel : ViewModel() {

    //for coroutines
    private val myJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + myJob)

    override fun onCleared() {
        super.onCleared()
        myJob.cancel()
    }

//    //we can take measurements only for the active connection of the user
//    private suspend fun take3wifiReadings(wifiManager: WifiManager): Float {
//        var sum = 0f
//        for (i in 1..3) {
//            // launch a coroutine in viewModelScope
//            viewModelScope.launch(Dispatchers.IO) {
//                // slowFetch()
//            }
//            sum += takeReading(wifiManager)
//        }
//        binding.wifiSignalStrength.text = getString(R.string.current_signal_strength_textview, sum / 3)
//        return (sum / 3)
//    }
//
//    suspend fun takeReading(wifiManager: WifiManager):Int{
//        val wifiInfo: WifiInfo = wifiManager.connectionInfo
//        val rssi = wifiInfo.rssi
//        Thread.sleep(500)
//        return rssi
//    }
}