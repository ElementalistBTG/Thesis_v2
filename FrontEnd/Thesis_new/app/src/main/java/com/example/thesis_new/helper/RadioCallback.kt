package com.example.thesis_new.helper

import android.telephony.*
import android.util.Log

class RadioCallback : TelephonyManager.CellInfoCallback() {

    private val tag = "testDinos"
    private var cellSet: MutableSet<Cell> = mutableSetOf()
    private var newCells = mutableMapOf<Int, Int>() //used for uniqueness of cells in case of dual sim phone
    private var aggregateCellList = mutableListOf<Cell>()//List with all the cells from multiple readings

    override fun onCellInfo(cellInfo: MutableList<CellInfo>) {
        Log.d("testDinos","RadioCallback")
//        cellSet.clear()
//        newCells.clear()
//
//        if (cellInfo != null && cellInfo.isNotEmpty()) {
//            for (cell in cellInfo) {
//                //Log.d(tag,cell.toString())
//                when (cell) {
//                    is CellInfoGsm -> {
//                        if (!newCells.containsKey(cell.cellIdentity.cid)) {//use this map to prevent double values for dual sim phones
//                            newCells[cell.cellIdentity.cid] = 1//put random number to value
//                            cellSet.add(
//                                Cell(
//                                    cell.cellIdentity.cid.toString(),
//                                    cell.cellSignalStrength.dbm
//                                )
//                            )
//                        }
//                    }
//                    is CellInfoCdma -> {
//                        if (!newCells.containsKey(cell.cellIdentity.basestationId)) {
//                            newCells[cell.cellIdentity.basestationId] =
//                                1//put random number to value
//                            cellSet.add(
//                                Cell(
//                                    cell.cellIdentity.basestationId.toString(),
//                                    cell.cellSignalStrength.cdmaDbm
//                                )
//                            )
//                        }
//                    }
//                    is CellInfoWcdma -> {
//                        if (!newCells.containsKey(cell.cellIdentity.cid)) {
//                            newCells[cell.cellIdentity.cid] = 1//put random number to value
//                            cellSet.add(
//                                Cell(
//                                    cell.cellIdentity.cid.toString(),
//                                    cell.cellSignalStrength.dbm
//                                )
//                            )
//                        }
//                    }
//                    is CellInfoLte -> {
//                        if (!newCells.containsKey(cell.cellIdentity.ci)) {
//                            newCells[cell.cellIdentity.ci] = 1//put random number to value
//                            cellSet.add(
//                                Cell(
//                                    cell.cellIdentity.ci.toString(),
//                                    cell.cellSignalStrength.dbm
//                                )
//                            )
//                        }
//                    }
//                }
//            }
//        }
//        aggregateCellList.addAll(cellSet)
//        Log.d(tag, "aggregateCellList on radiocallback is: $aggregateCellList")


    }
}

