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
import android.os.SystemClock
import android.provider.Settings
import android.telephony.*
import android.telephony.PhoneStateListener.LISTEN_CELL_INFO
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import com.android.volley.ParseError
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.thesis_new.R
import com.example.thesis_new.databinding.MapperActivityBinding
import com.example.thesis_new.helper.*
import com.example.thesis_new.viewModel.MapperActivityViewModel
import com.google.ar.core.*
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.core.Anchor.CloudAnchorState
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.android.synthetic.main.mapper_activity.*
import kotlinx.android.synthetic.main.mapper_activity.clear_button
import org.json.JSONArray
import org.json.JSONObject
import java.lang.Runnable

class MapperActivity : AppCompatActivity() {
    /**-- my variables --*/
    private val tag = "testDinos"
    var mApp = MyApplication()
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
    private lateinit var binding: MapperActivityBinding
    //view model
    private lateinit var viewModel: MapperActivityViewModel

    //for cloud anchors
    private val cloudAnchorManager = CloudAnchorManager()
    private var firebaseManager: FirebaseManager? = null
    private var anchorPendingUpload: Anchor? = null
    //anchors
    private val myCloudAnchors: ArrayList<Anchor?> = arrayListOf()
    private var cloudAnchorNode: AnchorNode? = null

    //values for storing the coordinates of cloud anchor in space
    private var roomNumber = 0
    private var point = 0

    private val FINE_LOCATION_PERMISSION_CODE = 1002
    private val READ_PHONE_STATE_PERMISSION = 1003

    private lateinit var progressDialog: ProgressDialog
    private var locationManager: LocationManager? = null


    private val myPhoneStateListener: PhoneStateListener = object : PhoneStateListener(){
        override fun onCellInfoChanged(cellInfo: MutableList<CellInfo>?) {
            super.onCellInfoChanged(cellInfo)
            Log.d(tag,"on cell info changed called")
            //Log.d(tag,"cell info: ${cellInfo.toString()}")
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
                                Log.d(tag,"GSM found: $cell")

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
                                Log.d(tag,"cdma found: $cell")
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
                                Log.d(tag,"wcdma found: $cell")
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
//                                Log.d(tag,"LTE found: $cell")
//                                Log.d(tag,"pci: ${cell.cellIdentity.pci}")
//                                Log.d(tag,"bandwidth: ${cell.cellIdentity.bandwidth}")
//                                Log.d(tag,"earfcn: ${cell.cellIdentity.earfcn}")
//                                Log.d(tag,"dbm: ${cell.cellSignalStrength.dbm}")
//                                Log.d(tag,"rsrp: ${cell.cellSignalStrength.rsrp}")
                            }
                        }
                    }
                }
            }
            aggregateCellList.addAll(cellSet)
            Log.d(tag, "aggregateCellList is: $aggregateCellList")

        }
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.mapper_activity)
        binding = DataBindingUtil.setContentView(this, R.layout.mapper_activity)
        //get the view model
        viewModel = ViewModelProviders.of(this).get(MapperActivityViewModel::class.java)
        // Specify the current activity as the lifecycle owner of the binding. This is used so that
        // the binding can observe LiveData updates
        binding.lifecycleOwner = this

        myArFragment = supportFragmentManager.findFragmentById(R.id.mapper_ar_fragment) as CloudAnchorFragment
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
        telephonyManager.listen(myPhoneStateListener,LISTEN_CELL_INFO)
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager?
        if (!locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps()
        }

        progressDialog = ProgressDialog(this)

        clear_button.setOnClickListener {onClearButtonPressed()}
        clear_button.isEnabled = false
        upload_anchor_button.setOnClickListener { onUploadAnchorButtonPressed() }
        upload_anchor_button.isEnabled = false
        upload_signals_button.setOnClickListener { onUploadSignalsButtonPressed() }
        upload_signals_button.isEnabled = false

        place_anchor_button.setOnClickListener {onPlaceAnchorButtonPressed() }

        measureSignals_button.setOnClickListener { onMeasureButtonPressed() }
    }

    private fun onMeasureButtonPressed(){
        emptyLists()
        storeCoordinatesValues()
        measureSignals_button.isEnabled = false
        upload_signals_button.isEnabled = false
    }

    @Synchronized
    private fun onPlaceAnchorButtonPressed() {
        //enter coordinates
        placeAnchor()
        startTimer(30000)
        clear_button.isEnabled = true
        place_anchor_button.isEnabled = false
        measureSignals_button.isEnabled = false
    }

    @Synchronized
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
                    startTimer(20000)
                    gatherSignals()
                }
            }
        }
        dialog.show()
    }

    private fun startTimer(time : Int){
        //start the timer
        instructions_mapping.visibility = View.VISIBLE
        view_timer.visibility = View.VISIBLE
        view_timer.isCountDown = true
        view_timer.base = SystemClock.elapsedRealtime() + time

        view_timer.setOnChronometerTickListener(object : Chronometer.OnChronometerTickListener {
            override fun onChronometerTick(chronometer: Chronometer?) {
                val delta = chronometer!!.base - SystemClock.elapsedRealtime()
                if (delta <= 0L) {
                    chronometer.stop()
                    //Toast.makeText(applicationContext,"stoped",Toast.LENGTH_SHORT).show()
                    upload_anchor_button.isEnabled = true
                }
            }
        })
        view_timer.start()
    }

    private fun placeAnchor() {
        if (myArFragment.arSceneView.arFrame!!.camera.trackingState == TrackingState.TRACKING) {
            val cameraPose =
                myArFragment.arSceneView.arFrame!!.camera.displayOrientedPose.compose(
                    Pose.makeTranslation(0f, 0f, 0f)
                )
            // If an AnchorNode existed before, remove and nullify it.
            if (cloudAnchorNode != null) {
                myArSceneView.scene.removeChild(cloudAnchorNode!!)
                cloudAnchorNode = null
            }

            //create the new anchor
            val anchor = myArFragment.arSceneView.session!!.createAnchor(cameraPose)
            setNewAnchor(anchor)
            anchorPendingUpload = anchor
            Log.d(tag, "Anchor pending is: $anchorPendingUpload")

        } else {
            Log.d(tag, "post delayed called")
            Toast.makeText(this, "Wait because the camera is not yet ready", Toast.LENGTH_SHORT).show()
            //camera not ready yet
            Handler().postDelayed({
                onPlaceAnchorButtonPressed()
            }, 1000)
        }
    }

    @Synchronized
    private fun onClearButtonPressed() {
        Log.d(tag, "onClearButtonPressed")
        // Clear the anchor from the scene.
        cloudAnchorManager.clearListeners()

        roomNumber = 0
        point = 0

        place_anchor_button.isEnabled = true
        upload_anchor_button.isEnabled = false
        clear_button.isEnabled = false
        cloudAnchorNode = null
        eraseAnchors()
        view_timer.stop()
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
    private fun setNewAnchor(anchor: Anchor?) {
        Log.d(tag, "setNewAnchor")
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
    }

    private fun onUploadSignalsButtonPressed() {
        progressDialog.setCancelable(false)
        progressDialog.setTitle("Uploading Data")
        progressDialog.setMessage("Data are being uploaded to the cloud.")
        progressDialog.show()

        cellCounter = 0
        wifiCounter = 0
        Log.d(tag, "Now uploading signals")
        volleyWriteSignalstoDb()
        upload_anchor_button.isEnabled = false
    }

    private fun onUploadAnchorButtonPressed() {
        progressDialog.setCancelable(false)
        progressDialog.setTitle("Uploading Anchor")
        progressDialog.setMessage("Data are being uploaded to the cloud.")
        progressDialog.show()

        Log.d(tag, "Now hosting anchor")
        cloudAnchorManager.hostCloudAnchor(
            myArSceneView.session!!, anchorPendingUpload
        ) { this.onHostedAnchorAvailable(it) }
        anchorPendingUpload = null
        Toast.makeText(this, "Router is being hosted to the cloud", Toast.LENGTH_SHORT).show()
        upload_anchor_button.isEnabled = false
    }

    private fun gatherSignals() {
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
            //val cellInfo = telephonyManager.allCellInfo
            //Log.d(tag,"cellInfo = $cellInfo")

            val radioCallback = RadioCallback()
            telephonyManager.requestCellInfoUpdate(AsyncTask.SERIAL_EXECUTOR,radioCallback)

//            if (cellInfo != null && cellInfo.isNotEmpty()) {
//                for (cell in cellInfo) {
//                    Log.d(tag,cell.toString())
//                    when (cell) {
//                        is CellInfoGsm -> {
//                            if (!newCells.containsKey(cell.cellIdentity.cid)) {//use this map to prevent double values for dual sim phones
//                                newCells[cell.cellIdentity.cid] = 1//put random number to value
//                                cellSet.add(
//                                    Cell(
//                                        cell.cellIdentity.cid.toString(),
//                                        cell.cellSignalStrength.dbm
//                                    )
//                                )
//                            }
//                        }
//                        is CellInfoCdma -> {
//                            if (!newCells.containsKey(cell.cellIdentity.basestationId)) {
//                                newCells[cell.cellIdentity.basestationId] =
//                                    1//put random number to value
//                                cellSet.add(
//                                    Cell(
//                                        cell.cellIdentity.basestationId.toString(),
//                                        cell.cellSignalStrength.cdmaDbm
//                                    )
//                                )
//                            }
//                        }
//                        is CellInfoWcdma -> {
//                            if (!newCells.containsKey(cell.cellIdentity.cid)) {
//                                newCells[cell.cellIdentity.cid] = 1//put random number to value
//                                cellSet.add(
//                                    Cell(
//                                        cell.cellIdentity.cid.toString(),
//                                        cell.cellSignalStrength.dbm
//                                    )
//                                )
//                            }
//                        }
//                        is CellInfoLte -> {
//                            if (!newCells.containsKey(cell.cellIdentity.ci)) {
//                                newCells[cell.cellIdentity.ci] = 1//put random number to value
//                                cellSet.add(
//                                    Cell(
//                                        cell.cellIdentity.ci.toString(),
//                                        cell.cellSignalStrength.dbm
//                                    )
//                                )
//                            }
//                        }
//                    }
//                }
//            }
//            aggregateCellList.addAll(cellSet)
//            Log.d(tag, "aggregateCellList is: $aggregateCellList")
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
                //Log.d(tag,"result ${result.toString()}")
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
                            //didnt't find the same wifi 4 times
                        }else{
                            val averageSignal = wifis.map { s->s.power }.average().toInt()
                            resultWifiSet.add(Wifi(it.key.second,it.key.first,averageSignal))
                        }

                    }
                    Log.d(tag,"Result Wifi set is: $resultWifiSet")
                    upload_signals_button.isEnabled = true
                }
            }
        }
    }

    private fun volleyWriteSignalstoDb (){
        val url = "http://" + mApp.IPaddress + "/JavaWebApp_war/postsignals"
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
        upload_signals_button.isEnabled = false
        measureSignals_button.isEnabled = true
    }

    private fun volleyWritePointsToDb(shortCode: Int) {
        val url = "http://" + mApp.IPaddress + "/JavaWebApp_war/postsignals"
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
        jsonRequest.put("Wifis", jsonArrayWifis)
        jsonRequest.put("Cells", jsonArrayCells)
        jsonRequest.put("roomNumber", roomNumber)
        jsonRequest.put("shortCode", shortCode)
        //jsonRequest.put("coordinates", "(xCoordinate,$yCoordinate)")
        Log.d(tag, "data to write to room table $jsonRequest.toString()")
        val stringReq =
            //we want an object only, not an array. The backend will respond with the best object
            @SuppressLint("SetTextI18n")
            object : JsonObjectRequest(
                Method.POST, url, jsonRequest, Response.Listener { response ->
                    val strResp = response.toString()
                    Log.d(tag, "volleyWritePointsToDb response: $strResp")
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
    }

    private fun emptyLists() {
        //empty the lists for future searches
        aggregateCellList.clear()
        resultCellSet.clear()
        aggregateWifiList.clear()
        resultWifiSet.clear()
        wifiSet.clear()
        allWifiDataAcquired = false
        roomNumber = 0
        point = 0
    }

    @Synchronized
    private fun onHostedAnchorAvailable(anchor: Anchor) {
        Log.d(tag, "onHostedAnchorAvailable")
        val cloudState = anchor.cloudAnchorState
        if (cloudState == CloudAnchorState.SUCCESS) {
            val cloudAnchorId = anchor.cloudAnchorId

            firebaseManager!!.nextShortCode { shortCode ->
                if (shortCode != null) {
                    firebaseManager!!.storeUsingShortCode(shortCode, cloudAnchorId)
                    Toast.makeText(this, "Cloud Anchor Hosted. Short code: $shortCode", Toast.LENGTH_SHORT).show()
                    progressDialog.dismiss()
                   // volleyWritePointsToDb(shortCode)
                } else {
                    // Firebase could not provide a short code.
                    Toast.makeText(
                        this,
                        "Cloud Anchor Hosted, but could not get a short code from Firebase.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            Toast.makeText(this, "Error while hosting: $cloudState", Toast.LENGTH_SHORT).show()
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


}

