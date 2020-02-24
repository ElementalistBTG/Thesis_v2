package com.example.thesis_new.main

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.telephony.*
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.android.volley.ParseError
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.thesis_new.R
import com.example.thesis_new.databinding.LostActivityBinding
import com.example.thesis_new.helper.*
import com.google.ar.core.*
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.core.Anchor.CloudAnchorState
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.android.synthetic.main.location_layout_renderable.view.*
import kotlinx.android.synthetic.main.lost_activity.*
import kotlinx.android.synthetic.main.lost_activity.coordinatorLayout2
import org.json.JSONArray
import org.json.JSONObject
import java.lang.Runnable
import kotlin.math.*

class LostActivity : AppCompatActivity() {

    private val tag = "testDinos"
    private var mApp = MyApplication()
    //wifi variables
    lateinit var wifiManager: WifiManager
    var wifiResultList = ArrayList<ScanResult>()//list with the results from the scan
    private var wifiSet: MutableSet<Wifi> = mutableSetOf()
    private var aggregateWifiList = mutableListOf<Wifi>()//List with all the wifis from multiple readings
    private val resultWifiSet: MutableSet<Wifi> = mutableSetOf() //final set to use for the volley request
    //cell variables
    private lateinit var telephonyManager: TelephonyManager
    private var cellSet: MutableSet<Cell> = mutableSetOf()
    private var newCells = mutableMapOf<Int, Int>() //used for uniqueness of cells in case of dual sim phone
    private var aggregateCellList = mutableListOf<Cell>()//List with all the cells from multiple readings
    private val resultCellSet: MutableSet<Cell> = mutableSetOf() //final set to use for the volley request
    private var allWifiDataAcquired = false
    private var intervalCounter = 4 //number of readings
    private var intervalTime = 5 //time between readings
    private var cellCounter = 0
    private var wifiCounter = 0
    private var receiverTriggeredCounter = 0 // a counter for wifi receiver calls
    //handler
    private var myHandler = Handler()
    //execute function once when all data are gathered
    private var runFunctionOnce = false

    //ar core needed variables
    //these values are used for ArCore calls
    private lateinit var myArFragment: CloudAnchorFragment
    private lateinit var myArSceneView: ArSceneView
    //runnable for updates
    private val resumeArElementsTask = Runnable {
        myArSceneView.resume()
    }
    //data binding
    private lateinit var binding: LostActivityBinding
    //for cloud anchors
    private val cloudAnchorManager = CloudAnchorManager()
    private var firebaseManager: FirebaseManager? = null
    //anchors
    private val myCloudAnchors: ArrayList<Anchor?> = arrayListOf()
    private var cloudAnchorNode: AnchorNode? = null
    //values used to measure distance of resolved cloud anchor
    private var currentAnchor: Anchor? = null
    private lateinit var currentAnchorNode: AnchorNode
    //progress dialog
    private lateinit var progressDialog: ProgressDialog
    private val FINE_LOCATION_PERMISSION_CODE = 1002
    private val READ_PHONE_STATE_PERMISSION = 1003
    private var bestDistance = 1000f

    private lateinit var adapterForList: ArrayAdapter<String> // adapter for wifi ListView
    private var cloudAnchorsArrayList = ArrayList<String>()
    private var locationManager: LocationManager? = null
    private var roomNumber = 0
    private var point = 0

    private val myPhoneStateListener: PhoneStateListener = object : PhoneStateListener(){
        override fun onCellInfoChanged(cellInfo: MutableList<CellInfo>?) {
            super.onCellInfoChanged(cellInfo)
            Log.d(tag,"on cell info changed called")
            // Log.d(tag,"cell info: ${cellInfo.toString()}")
            if (cellInfo != null && cellInfo.isNotEmpty()) {
                for (cell in cellInfo) {
                    //Log.d(tag,cell.toString())
                    when (cell) {
                        is CellInfoGsm -> {
                            if (!newCells.containsKey(cell.cellIdentity.cid)) {//use this map to prevent double values for dual sim phones
                                newCells[cell.cellIdentity.cid] = 1//put random number to value
                                cellSet.add(
                                    Cell(
                                        cell.cellIdentity.cid.toString(),
                                        cell.cellSignalStrength.dbm
                                    )
                                )
                            }
                        }
                        is CellInfoCdma -> {
                            if (!newCells.containsKey(cell.cellIdentity.basestationId)) {
                                newCells[cell.cellIdentity.basestationId] =
                                    1//put random number to value
                                cellSet.add(
                                    Cell(
                                        cell.cellIdentity.basestationId.toString(),
                                        cell.cellSignalStrength.cdmaDbm
                                    )
                                )
                            }
                        }
                        is CellInfoWcdma -> {
                            if (!newCells.containsKey(cell.cellIdentity.cid)) {
                                newCells[cell.cellIdentity.cid] = 1//put random number to value
                                cellSet.add(
                                    Cell(
                                        cell.cellIdentity.cid.toString(),
                                        cell.cellSignalStrength.dbm
                                    )
                                )
                            }
                        }
                        is CellInfoLte -> {
                            if (!newCells.containsKey(cell.cellIdentity.ci)) {
                                newCells[cell.cellIdentity.ci] = 1//put random number to value
                                cellSet.add(
                                    Cell(
                                        cell.cellIdentity.ci.toString(),
                                        cell.cellSignalStrength.dbm
                                    )
                                )
                            }
                        }
                    }
                }
            }
            aggregateCellList.addAll(cellSet)
            Log.d(tag, "aggregateCellList is: $aggregateCellList")

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.mapper_activity)
        binding = DataBindingUtil.setContentView(this, R.layout.lost_activity)
        // Specify the current activity as the lifecycle owner of the binding. This is used so that
        // the binding can observe LiveData updates
        binding.lifecycleOwner = this

        myArFragment = supportFragmentManager.findFragmentById(R.id.lost_ar_fragment) as CloudAnchorFragment
        myArFragment.arSceneView.scene.addOnUpdateListener { frameTime -> cloudAnchorManager.onUpdate() }
        //disable the instruction hand in screen and the plane controller
        myArFragment.planeDiscoveryController.hide()
        myArFragment.planeDiscoveryController.setInstructionView(null)

        myArSceneView = myArFragment.arSceneView
        myArSceneView.planeRenderer.isEnabled = false
        //initialize Managers
        firebaseManager = FirebaseManager(applicationContext)
        telephonyManager =
            applicationContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        telephonyManager.listen(myPhoneStateListener, PhoneStateListener.LISTEN_CELL_INFO)
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager?
        if (!locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps()
        }

        progressDialog = ProgressDialog(this)

        reset_button.setOnClickListener { v -> onResetButtonPressed() }
        reset_button.isEnabled = false

        find_me_button.setOnClickListener { v -> onFindMeButtonPressed() }
        find_me_button.isEnabled = true

        button.setOnClickListener{updateAllDistances()}

        lost_measure_button.setOnClickListener{ onLostMeasureButtonPressed()}

    }

    private fun onLostMeasureButtonPressed(){
        emptyLists()
        storeCoordinatesValues()
        lost_measure_button.isEnabled = false
        reset_button.isEnabled = true
    }

    private fun storeCoordinatesValues() {
        val alertDialogBuilder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.popup_layout, null)
        val roomEditText = dialogLayout.findViewById<EditText>(R.id.room_editext)
        val pointEditText = dialogLayout.findViewById<EditText>(R.id.point_edittext)
        alertDialogBuilder.setView(dialogLayout)
        alertDialogBuilder.setCancelable(false)
        alertDialogBuilder.setPositiveButton(android.R.string.ok, null) //we override the button
        val dialog = alertDialogBuilder.create()
        dialog.setOnShowListener {
            val okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            okButton.setOnClickListener {
                if (roomEditText.text.isEmpty() ||
                    pointEditText.text.isEmpty()
                ) {
                    Toast.makeText(this, "Fill all the parameters first", Toast.LENGTH_SHORT).show()
                } else {
                    roomNumber = roomEditText.text.toString().toInt()
                    point = pointEditText.text.toString().toInt()
                    //Toast.makeText(this, "room number is $roomNumber, x=$xCoordinate, y=$yCoordinate", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    gatherSignals()
                }
            }
        }
        dialog.show()
    }

    private fun buildAlertMessageNoGps() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Your GPS seems to be disabled, you must enable it for the app to run smoothly")
            .setCancelable(false)
            .setPositiveButton("Go to settings") { _, _ ->
                startActivityForResult(
                    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    , 11
                )
            }
        val alert: AlertDialog = builder.create()
        alert.show()
    }

    @Synchronized
    private fun onResetButtonPressed() {
        Log.d(tag, "onResetButtonPressed")
        // Clear the anchor from the scene.
        cloudAnchorManager.clearListeners()

        lost_measure_button.isEnabled = true
        find_me_button.isEnabled = true
        reset_button.isEnabled = false
        cloudAnchorNode = null
        eraseAnchors()
        bestDistance = 1000f

        CoordinatesViaBestSignalTextview.text = "-"
        PointViaBestSignalTextview.text = "-"
        RoomViaBestSignalTextview.text = "-"
        RoomViaKNNTextview.text = "-"
        RoomViaWKNNTextview.text = "-"
        CoordinatesViaKNNTextview.text = "-"
        PointViaKNNTextview.text = "-"
        DistanceFromCloudAnchorTextview.text = "-"
        CloudAnchorsFoundTextview.text = "-"


        Toast.makeText(this, "All routers shown erased", Toast.LENGTH_SHORT).show()
    }

    private fun eraseAnchors() {
        //Log.d("testing",myCloudAnchors.toString())
        for (anchor in myCloudAnchors) {
            anchor?.detach()
        }
        myCloudAnchors.clear()

    }

    // Modify the renderables when a new anchor is available.
    @Synchronized
    private fun setOldAnchor(anchor: Anchor?, coordinates: String?) {
        Log.d(tag, "setOldAnchor")
        myCloudAnchors.add(anchor)
        // Create the Anchor Node
        cloudAnchorNode = AnchorNode(anchor)
        myArSceneView.scene!!.addChild(cloudAnchorNode!!)

        ModelRenderable.builder()
            .setSource(this, Uri.parse("gloworb.sfb"))
            .build()
            .thenAccept { modelRenderable ->
                // TransformableNode means the user to move, scale and rotate the model
                val transformableNode = TransformableNode(myArFragment.transformationSystem)
                transformableNode.renderable = modelRenderable
                transformableNode.setParent(cloudAnchorNode)
                myArFragment.arSceneView.scene.addChild(cloudAnchorNode)
                transformableNode.select()
            }

        //create the pin
        ViewRenderable.builder()
            .setView(this, R.layout.location_layout_renderable)
            .build()
            .thenAccept { viewRenderable ->
                val transformableNode = TransformableNode(myArFragment.transformationSystem)
                transformableNode.renderable = viewRenderable
                transformableNode.localPosition = Vector3(0f, 0.1f, 0f)//little up
                transformableNode.scaleController.maxScale = 0.3f
                transformableNode.scaleController.minScale = 0.29f
                val imageView = viewRenderable.view
                val pinCoordinates = imageView.CoordinatesTextView
                val pinDistance = imageView.DistancePinTextView
                pinCoordinates.text = coordinates
                pinDistance.text = "Distance: ${getDistance(anchor)}"

                transformableNode.setParent(cloudAnchorNode)
                myArFragment.arSceneView.scene.addChild(cloudAnchorNode)
            }
    }

    @Synchronized
    private fun onFindMeButtonPressed() {
        find_me_button.isEnabled = false
        reset_button.isEnabled = true
        //first we acquire the room we are in via the signals
       // gatherSignals()
        //then we acquire the short codes and coordinated of each point in the room
        //then we materialize all anchors and find our location via them
        materializeAnchor(1)
        materializeAnchor(2)
        materializeAnchor(3)
        materializeAnchor(4)
        materializeAnchor(5)
        materializeAnchor(6)
    }

    private fun gatherSignals() {
        progressDialog.setCancelable(false)
        progressDialog.setTitle("Signals Gathering")
        progressDialog.setMessage("Gathering data from wifi and cell signals... Please be patient")
        progressDialog.show()
        //run once and gather data for our current position
        runFunctionOnce = true
        checkDataRunnable.run()
        acquireCellDataRunnable.run()
        acquireWifiDataRunnable.run()
    }

    private val acquireCellDataRunnable = object : Runnable {
        override fun run() {
            scanCellular()
            Log.d(tag, "Run with cell counter: $cellCounter")
            cellCounter++
            if (cellCounter < intervalCounter) {
                //rerun every internal set
                myHandler.postDelayed(this, (intervalTime * 1000).toLong())
            }
        }
    }

    private val acquireWifiDataRunnable = object : Runnable {
        override fun run() {
            scanWifis()
            Log.d(tag, "Run with wifi counter: $wifiCounter")
            wifiCounter++
            if (wifiCounter < intervalCounter) {
                //rerun every internal set
                myHandler.postDelayed(this, (intervalTime * 1000).toLong())
            }
        }
    }

    private val checkDataRunnable = object : Runnable {
        override fun run() {
            Log.d(tag, "CheckData runnable")
            checkWifiCellData()
            if (runFunctionOnce) {
                myHandler.postDelayed(this, 5000)
            }
        }
    }

    private fun scanCellular() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not granted
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_PHONE_STATE),
                READ_PHONE_STATE_PERMISSION
            )
        } else {
            cellSet.clear()
            newCells.clear()
            val radioCallback = RadioCallback()
            telephonyManager.requestCellInfoUpdate(AsyncTask.SERIAL_EXECUTOR,radioCallback)
        }
    }//SCAN CELLULAR END

    @Suppress("DEPRECATION")
    private fun scanWifis() {
        applicationContext.registerReceiver(
            wifiReceiver,
            IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        )

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not granted
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                FINE_LOCATION_PERMISSION_CODE
            )
        } else {
            wifiManager.startScan()
        }
    }

    private val wifiReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            wifiResultList = wifiManager.scanResults as ArrayList<ScanResult>
            applicationContext.unregisterReceiver(this)
            //Log.d(tag, "new wifi readings")
            for (result in wifiResultList) {
                wifiSet.add(
                    Wifi(
                        result.SSID,
                        result.BSSID,
                        result.level
                    )
                )
            }
            aggregateWifiList.addAll(wifiSet)
            receiverTriggeredCounter++
            if(receiverTriggeredCounter >= intervalCounter){
                allWifiDataAcquired = true
            }
            Log.d(tag, "Aggregate WifiList is: $wifiSet")
        }
    }

    private fun checkWifiCellData() {
        if (aggregateWifiList.isNotEmpty() && aggregateCellList.isNotEmpty()) {
            if (cellCounter >= intervalCounter &&
                allWifiDataAcquired
            ) {
                myHandler.removeCallbacksAndMessages(acquireCellDataRunnable)
                myHandler.removeCallbacksAndMessages(acquireWifiDataRunnable)
                if (runFunctionOnce) {//first time it will check
                    myHandler.removeCallbacksAndMessages(checkDataRunnable)
                    runFunctionOnce = false
                    //find the average values for the data in aggregateCellList and put them in resultCellSet
                    /*
                    * First we group by the id.
                    * Then we take each group and calculate the average of the signals to pass it to the resultCellSet
                    * */
                    aggregateCellList.groupBy { it.id }.forEach{
                        val cells = it.value
                        val averageSignal = cells.map { s->s.signal_strength }.average().toInt()
                        resultCellSet.add(Cell(it.key,averageSignal))
                    }
                    Log.d(tag,"Result Cell set is: $resultCellSet")

                    //similarly for wifis
                    //In wifis we take the unique pairs of ssid-bssid
                    aggregateWifiList.groupBy { Pair(it.bssid,it.ssid) }.forEach{
                        val wifis = it.value
                        if (wifis.size<4){
                            //didnt't find the same wifi 4 times so we ignore it
                        }else{
                            val averageSignal = wifis.map { s->s.power }.average().toInt()
                            resultWifiSet.add(Wifi(it.key.second,it.key.first,averageSignal))
                        }
                    }
                    Log.d(tag,"Result Wifi set is: $resultWifiSet")
                   // volleyGetRoom()
                    volleyWriteSignalstoDb()
                }
            }
        }
    }

    private fun volleyWriteSignalstoDb (){
        val url = "http://" + mApp.IPaddress + "/JavaWebApp_war/postmeasuredsignals"
        Log.d(tag, "url used is $url")
        val queue = Volley.newRequestQueue(this)
        val jsonRequest = JSONObject()
        val jsonArrayWifis = JSONArray()
        for (wifi in resultWifiSet) {
            val jsonWifi = JSONObject()
            jsonWifi.put("bssid", wifi.bssid)
            jsonWifi.put("power", wifi.power)
            jsonArrayWifis.put(jsonWifi)
        }
        val jsonArrayCells = JSONArray()
        for (cell in resultCellSet) {
            val jsonCell = JSONObject()
            jsonCell.put("id", cell.id)
            jsonCell.put("power", cell.signal_strength)
            jsonArrayCells.put(jsonCell)
        }
        jsonRequest.put("shortCode",point)
        jsonRequest.put("Wifis", jsonArrayWifis)
        jsonRequest.put("Cells", jsonArrayCells)
        jsonRequest.put("roomNumber", roomNumber)
        //jsonRequest.put("coordinates", "($xCoordinate,$yCoordinate)")
        Log.d(tag, "data to write to signalsMapped table $jsonRequest")
        val stringReq =
            //we want an object only, not an array. The backend will respond with the best object
            @SuppressLint("SetTextI18n")
            object : JsonObjectRequest(
                Method.POST, url, jsonRequest, Response.Listener { response ->
                    val strResp = response.toString()
                    Log.d(tag, "volleyWriteSignalstoDb response: $strResp")
                    progressDialog.dismiss()
                },
                Response.ErrorListener { error ->
                    progressDialog.dismiss()
                    if (error is ParseError) {
                        Log.d(tag, "parse error (y)")
                    } else {
                        Log.d(tag, "got error response: $error")
                        Toast.makeText(this, "$error", Toast.LENGTH_SHORT).show()
                    }
                }) {

                override fun getHeaders(): Map<String, String> {
                    val headers = HashMap<String, String>()
                    headers["Content-Type"] = "application/json"
                    headers["Cache-control"] = "no-cache"
                    return headers
                }
            }
        queue.add(stringReq)
        emptyLists()
        lost_measure_button.isEnabled = true
    }

    private fun volleyGetRoom() {

        //if we use the W-KNN we use this approach
        val url = "http://" + mApp.IPaddress + "/JavaWebApp_war/getroom"
        //if we get the 20 best signals we use this line
        //val url = "http://" + mApp.IPaddress + "/JavaWebApp_war/getbestpoints"

        val queue = Volley.newRequestQueue(this)

        //send JSON object
        val jsonRequest = JSONObject()

        val jsonArrayWifis = JSONArray()
        for (wifi in resultWifiSet) {
            val jsonWifi = JSONObject()
            jsonWifi.put("bssid", wifi.bssid)
            jsonWifi.put("power", wifi.power)
            jsonArrayWifis.put(jsonWifi)
        }

        val jsonArrayCells = JSONArray()
        for (cell in resultCellSet) {
            val jsonCell = JSONObject()
            jsonCell.put("id", cell.id)
            jsonCell.put("power", cell.signal_strength)
            jsonArrayCells.put(jsonCell)
        }
        jsonRequest.put("Wifis", jsonArrayWifis)
        jsonRequest.put("Cells", jsonArrayCells)
        Log.d(tag, "get room request: ${jsonRequest.toString()}")

        val stringReq =
            //we want an object only, not an array. The backend will respond with the best object
            @SuppressLint("SetTextI18n")
            object : JsonObjectRequest(
                Method.POST, url, jsonRequest, Response.Listener { response ->
                    val strResp = response.toString()
                    Log.d(tag, "got response: $strResp")
                    val jsonObjectResponse = JSONObject(strResp)

                    RoomViaKNNTextview.text = "RoomViaKNN: "+jsonObjectResponse.getInt("roomViaKNN")
                    RoomViaBestSignalTextview.text = "RoomViaBestSignal: "+jsonObjectResponse.getInt("roomViaBestSignal")
                    RoomViaWKNNTextview.text = "RoomViaWKNN "+jsonObjectResponse.getInt("roomViaWKNN")
                    CoordinatesViaKNNTextview.text = "CoordinatesViaKNN: "+jsonObjectResponse.getString("coordinatesViaKNN")
                    CoordinatesViaBestSignalTextview.text = "CoordinatesViaBestSignal: "+jsonObjectResponse.getString("coordinatesViaBestSignal")
                    PointViaKNNTextview.text = "PointViaKNN: "+jsonObjectResponse.getInt("pointViaKNN")
                    PointViaBestSignalTextview.text = "PointViaBestSignal: "+jsonObjectResponse.getInt("pointViaBestSignal")


                    val shortCodesArray = jsonObjectResponse.getJSONArray("shortCodesArray")
                    val coordinatesArray = jsonObjectResponse.getJSONArray("coordinatesArray")
                    Toast.makeText(this, "Move your phone to pinpoint your location", Toast.LENGTH_LONG).show()
                    for (i in 0 until shortCodesArray.length()) {
                        materializeAnchorOld(shortCodesArray.get(i) as Int, coordinatesArray.get(i).toString())
                    }

                    if (progressDialog.isShowing) {
                        progressDialog.dismiss()
                    }
                },
                Response.ErrorListener { error ->
                    if (progressDialog.isShowing) {
                        progressDialog.dismiss()
                    }
                    if (error is ParseError) {
                        Toast.makeText(
                            this,
                            "No wifi or cell tower common in your area to run similarity matching",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Log.d(tag, "got error response: $error")
                        Toast.makeText(this, "$error", Toast.LENGTH_SHORT).show()
                    }
                }) {

                override fun getHeaders(): Map<String, String> {
                    val headers = HashMap<String, String>()
                    headers["Content-Type"] = "application/json"
                    headers["Cache-control"] = "no-cache"
                    return headers
                }
            }
        queue.add(stringReq)
        emptyLists()
    }

    private fun emptyLists() {
        //empty the lists for future searches
        aggregateCellList.clear()
        resultCellSet.clear()
        aggregateWifiList.clear()
        resultWifiSet.clear()
        wifiSet.clear()
        cellCounter =0
        wifiCounter =0
        allWifiDataAcquired = false
    }

    private fun materializeAnchor(shortCode: Int) {
        firebaseManager!!.getCloudAnchorId(shortCode) { cloudAnchorId ->
            Log.d(tag, "Cloud anchor id: $cloudAnchorId")
            if (cloudAnchorId != null && cloudAnchorId.isNotEmpty() && cloudAnchorId != "null") {
                //Toast.makeText(this, "Visualising existing routers in the cloud", Toast.LENGTH_SHORT).show()
                cloudAnchorManager.resolveCloudAnchor(myArSceneView.session!!, cloudAnchorId)
                { anchor -> onResolvedAnchorAvailable(anchor, shortCode, "test") }
            } else if (shortCode == 1) {
                find_me_button.isEnabled = true
                Toast.makeText(this, "No cloud anchors to load", Toast.LENGTH_SHORT).show()
                return@getCloudAnchorId
            } else {
                find_me_button.isEnabled = true
                return@getCloudAnchorId
            }
        }
    }

    private fun materializeAnchorOld(shortCode: Int, pointCoordinates: String) {
        firebaseManager!!.getCloudAnchorId(shortCode) { cloudAnchorId ->
            Log.d(tag, "Cloud anchor id: $cloudAnchorId")
            if (cloudAnchorId != null && cloudAnchorId.isNotEmpty() && cloudAnchorId != "null") {
                //Toast.makeText(this, "Visualising existing routers in the cloud", Toast.LENGTH_SHORT).show()
                cloudAnchorManager.resolveCloudAnchor(myArSceneView.session!!, cloudAnchorId)
                { anchor -> onResolvedAnchorAvailable(anchor, shortCode, pointCoordinates) }
            } else if (shortCode == 1) {
                find_me_button.isEnabled = true
                Toast.makeText(this, "No cloud anchors to load", Toast.LENGTH_SHORT).show()
                return@getCloudAnchorId
            } else {
                find_me_button.isEnabled = true
                return@getCloudAnchorId
            }
        }
    }

    @Synchronized
    private fun onResolvedAnchorAvailable(anchor: Anchor, shortCode: Int, pointCoordinates: String) {
        val cloudState = anchor.cloudAnchorState

        currentAnchor = anchor
        currentAnchorNode = AnchorNode(anchor)

        if (cloudState == CloudAnchorState.SUCCESS) {
            CloudAnchorsFoundTextview.append("$shortCode, ")
            Toast.makeText(this, "Cloud Anchor Resolved. Short code: $shortCode", Toast.LENGTH_SHORT).show()
            Log.d(tag, "Cloud Anchor Resolved. Short code: $shortCode")
            setOldAnchor(anchor, pointCoordinates)
            updateDistanceFromNode(shortCode)
        } else {
            Log.d(
                tag, "Error while resolving anchor with short code "
                        + shortCode
                        + ". Error: "
                        + cloudState.toString()
            )
            //when we pass through all anchors
            find_me_button!!.isEnabled = false
        }
    }

    override fun onResume() {
        super.onResume()
        checkAndRequestPermissions()
    }

    override fun onPause() {
        super.onPause()
        myArSceneView.session?.let {
            myArSceneView.pause()
        }
    }

    private fun checkAndRequestPermissions() {
        Log.d(tag, "checkAndRequestPermissions")
        if (!PermissionUtils.hasCameraPermission(this)) {
            PermissionUtils.requestCameraPermission(this)
        } else {
            setupSession()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, results: IntArray) {
        Log.d(tag, "onRequestPermissionsResult")
        if (!PermissionUtils.hasCameraPermission(this)) {
            if (!PermissionUtils.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                PermissionUtils.launchPermissionSettings(this)
            }
            finish()
        } else {
            setupSession()
        }
    }

    private fun setupSession() {
        Log.d(tag, "setupSession")

        try {
            resumeArElementsTask.run()
        } catch (e: CameraNotAvailableException) {
            Toast.makeText(this, "Unable to get camera", Toast.LENGTH_LONG).show()
            finish()
            return
        }

    }

    private fun updateDistanceFromNode(shortCode: Int) {

        val distanceMeters = getDistance(currentAnchor)
//        val bearingDegrees = getBearing(currentAnchor)
        if (distanceMeters < bestDistance) {
            bestDistance = distanceMeters
            DistanceFromCloudAnchorTextview.text = "Closest CA is: $shortCode at: $distanceMeters m"
        }
        Log.d(tag, "Distance from camera: $distanceMeters metres")
    }

    private fun getDistance(anchor: Anchor?) : Float{
        val frame: Frame? = myArFragment.arSceneView.arFrame
        val objectPose: Pose = anchor!!.pose
        val cameraPose: Pose? = frame?.camera!!.pose

        val dx = objectPose.tx() - cameraPose!!.tx() //sideways
        val dz = objectPose.tz() - cameraPose.tz() //front
        // ty is for up
        return sqrt(dx * dx + dz * dz)
    }

    //this function is useless since it is based on the specific frame
    //We can probably take the magnetometer readings and apply them (change the rotation of the camera pose) to always have the same bearing
    private fun getBearing(anchor: Anchor?): Double{

        //Define the bearing angle θ from a point A(a1,a2) to a point B(b1,b2) as the angle measured in the clockwise direction from the north line with A as the origin to the line segment AB.
//        val frame: Frame? = myArFragment.arSceneView.arFrame
        val objectPose: Pose = anchor!!.pose //point B
//        val cameraPose: Pose? = frame?.camera!!.pose //point A
        val worldPose = myArFragment.arSceneView.arFrame!!.androidSensorPose

        val b1 = objectPose.tx()
        val b2 = objectPose.tz()

//        val a1=  cameraPose!!.tx()
//        val a2 = cameraPose.tz()
//        val bearing = atan2(b1-a1,b2-a2).toDouble()
//        val myDegrees = Math.toDegrees(bearing)
//        Log.d(tag,"bearing: $myDegrees")

        val a1new = worldPose.tx()
        val a2new = worldPose.tz()
        val bearingNew = atan2(b1-a1new,b2-a2new).toDouble()
        val myDegreesNew = Math.toDegrees(bearingNew)
        Log.d(tag,"bearingNew: $myDegreesNew")

        return myDegreesNew
    }

    private fun updateAllDistances(){

        //create the popup
        val inflater: LayoutInflater =
            this.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView = inflater.inflate(R.layout.cloudanchors_popup_layout, null)
        // create the popup window
        val width = LinearLayout.LayoutParams.WRAP_CONTENT
        val height = LinearLayout.LayoutParams.WRAP_CONTENT
        val focusable = true
        val popupWindow: PopupWindow = PopupWindow(popupView, width, height, focusable)
        //create shadow
        popupWindow.elevation = 20f

        cloudAnchorsArrayList.clear()
        for(anchor in myCloudAnchors){
            cloudAnchorsArrayList.add("Anchor $anchor at distance: ${getDistance(anchor)} meters")
        }

        //create the list view
        val myListView = popupView.findViewById(R.id.cloud_anchors_List) as ListView
        adapterForList = ArrayAdapter(applicationContext,android.R.layout.simple_list_item_1,cloudAnchorsArrayList)
        myListView.adapter = adapterForList

        // show the popup window
        // which view you pass in doesn't matter, it is only used for the window tolken
        popupWindow.showAtLocation(coordinatorLayout2, Gravity.CENTER, 0, 0)
    }

}

