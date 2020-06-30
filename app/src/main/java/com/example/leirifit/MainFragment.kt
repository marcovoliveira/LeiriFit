package com.example.leirifit

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.example.leirifit.database.Run
import com.example.leirifit.database.RunDatabase
import com.example.leirifit.databinding.FragmentMainPageBinding
import com.example.leirifit.datamodel.CheckpointModel
import com.example.leirifit.geofencing.GeofenceBroadcastReceiver
import com.example.leirifit.geofencing.GeofenceHelper
import com.example.leirifit.viewmodel.RunViewModel
import com.example.leirifit.viewmodel.RunViewModelFactory
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.ml.common.FirebaseMLException
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.maps.android.PolyUtil
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


/**
 * Run page fragment
 */

class MainFragment : Fragment(), OnMapReadyCallback {

    private var currentPhotoFile: File? = null
    private var classifier: ImageClassifier? = null
    private var imagePreview: ImageView? = null
    private var textView: TextView? = null
    private var chronometer: Chronometer? = null
    private var running: Boolean = false
    private var distanceValueTextView: TextView? = null
    private var nextCheckpointValueTextView: TextView? = null
    private var takePhotoButton: ImageButton? = null
    private var startButton: Button? = null

    // maps
    private var map: GoogleMap? = null
    private var mapFragment: SupportMapFragment? = null
    private var checkPointsDataSource = ArrayList<CheckpointModel>()
    private var currentDataSourceIndex = 0
    private var currentMarker: Marker? = null
    private var userCustomMarker: Marker? = null

    // geofencing circle
    private var circle: Circle? = null

    // location
    private var locationManager: LocationManager? = null
    private var currentCoords: LatLng? = null
    private var routes = ArrayList<Polyline>()

    // thread handling
    private var uiHandler: Handler? = null;

    // geo fencing
    private var geofencingClient: GeofencingClient? = null;
    private var geofenceHelper: GeofenceHelper? = null
    private var geoFenceId = "GEO_FENCE_01";
    private var geofence: Geofence? = null;

    // distance
    private var previousCoords: LatLng? = null
    private var distanceInM: Float = 0f
    private var distanceInKm: String? = null

    private var runViewModel: RunViewModel? = null;
    private var args: MainFragmentArgs? = null;


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val binding = DataBindingUtil.inflate<FragmentMainPageBinding>(
            inflater,
            R.layout.fragment_main_page, container, false
        )
        args = MainFragmentArgs.fromBundle(arguments!!)
        Toast.makeText(
            context,
            "Name: ${args!!.participantName}, Idade: ${args!!.age}, Sexo: ${args!!.sex}",
            Toast.LENGTH_LONG
        ).show()

        try {
            classifier = ImageClassifier(requireActivity())
        } catch (e: FirebaseMLException) {
            // textView?.text = getString(R.string.fail_to_initialize_img_classifier)
        }
        // get the bindings
        textView = binding.legendTextView
        distanceValueTextView = binding.distanceValueTextView
        nextCheckpointValueTextView = binding.nextStopValueTextView
        takePhotoButton = binding.cameraImageButton
        takePhotoButton?.isEnabled = false
        startButton = binding.startStopButton

        imagePreview = binding.imagePreview
        chronometer = binding.timeValueChronometer
        binding.cameraImageButton.setOnClickListener {
            takePhoto()
        }

        binding.startStopButton.setOnClickListener {
            startButton?.isEnabled = false; handleNextCheckpointMapMovement()
        }


        setupNecessaryComponentsForMap()
        setupNecessaryComponentsForFencing()

        // viewModel
        val application = requireNotNull(this.activity).application
        val dataSource = RunDatabase.getInstance(application).runDatabaseDao
        val viewModelFactory = RunViewModelFactory(dataSource, application)
        runViewModel =
            ViewModelProviders.of(
                this, viewModelFactory
            ).get(RunViewModel::class.java)

        binding.setLifecycleOwner(this)
        binding.runViewModel = runViewModel

        return binding.root
    }

    override fun onDestroy() {
        classifier?.close()
        super<Fragment>.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        activity!!.registerReceiver(receiver, IntentFilter("X"))
    }

    override fun onPause() {
        super.onPause()
        activity!!.unregisterReceiver(receiver)
    }

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {

            takePhotoButton?.isEnabled = true
            takePhoto()
            if (currentDataSourceIndex == 0) {
                startChronometer();
            }
        }
    }

    private fun setupNecessaryComponentsForMap() {
        uiHandler = Handler(Looper.getMainLooper())
        createDataSource()
        handleMapCreation()
    }

    private fun setupNecessaryComponentsForFencing() {
        geofencingClient = context?.let { LocationServices.getGeofencingClient(it) }
        geofenceHelper = GeofenceHelper(context)
    }


    private fun takePhoto() {
        Log.e(TAG, "Start take photo function")
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent.
            takePictureIntent.resolveActivity(requireActivity().packageManager)?.also {
                // Create the File where the photo should go.
                val photoFile: File? = try {
                    createImageFile()
                } catch (e: IOException) {
                    // Error occurred while creating the File.
                    Log.e(TAG, "Unable to save image to run classification.", e)
                    null
                }
                // Continue only if the File was successfully created.
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        requireContext(),
                        BuildConfig.APPLICATION_ID + ".provider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }
            }
        }
    }

    /** Create a file to pass to camera app */
    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir = requireActivity().cacheDir
        return createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents.
            currentPhotoFile = this
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK) return

        when (requestCode) {
            // Make use of FirebaseVisionImage.fromFilePath to take into account
            // Exif Orientation of the image files.
            REQUEST_IMAGE_CAPTURE -> {
                FirebaseVisionImage.fromFilePath(requireContext(), Uri.fromFile(currentPhotoFile))
                    .also {
                        classifyImage(it.bitmap)
                    }
            }
            REQUEST_PHOTO_LIBRARY -> {
                val selectedImageUri = data?.data ?: return
                FirebaseVisionImage.fromFilePath(requireContext(), selectedImageUri).also {
                    classifyImage(it.bitmap)
                }
            }
        }
    }

    private fun classifyImage(bitmap: Bitmap) {
        if (classifier == null) {
            textView?.text = "Unitialized"
            return
        }

        // Show image on screen.
        imagePreview?.setImageBitmap(bitmap)

        // Classify image.
        classifier?.classifyFrame(bitmap)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                textView?.text = task.result
                //TODO
                validarImagem(task.result);
            } else {
                val e = task.exception
                Log.e(TAG, "Error classifying frame", e)
                textView?.text = e?.message
            }
        }
    }

    private fun validarImagem(result: String?) {
        if (result == checkPointsDataSource[currentDataSourceIndex].name) {
            takePhotoButton?.isEnabled = false;
            showNextCheckpoint()
        } else {
            Toast.makeText(activity, "Imagem não reconhecida", Toast.LENGTH_LONG).show()
        }

    }

    private fun handleMapCreation() {

        mapFragment =
            getChildFragmentManager().findFragmentById(R.id.mapView) as SupportMapFragment?

        handleLocationManagerCreation()

        mapFragment?.getMapAsync(OnMapReadyCallback {
            onMapReady(it)
        });
    }

    @SuppressLint("MissingPermission")
    private fun handleLocationManagerCreation() {
        locationManager = context?.getSystemService(LOCATION_SERVICE) as LocationManager?;

        var enable = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (enable!!) {
            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                5000,
                0.1f,
                LocationListener {
                    handleLocationUpdates(it)
                })
        }
    }

    private fun handleLocationUpdates(location: Location) {
        if (location != null && location.latitude != null && location.longitude != null) {
            currentCoords = LatLng(location.latitude, location.longitude);

            if (previousCoords != null && running) {

                var previousLocation: Location = Location("")
                previousLocation.latitude = previousCoords?.latitude!!
                previousLocation.longitude = previousCoords?.longitude!!

                distanceInM += previousLocation.distanceTo(location)

                distanceInKm = "%.2f".format(distanceInM.div(1000F)).toString()

                distanceValueTextView?.text = distanceInKm

            }

            previousCoords = currentCoords

            routeRequest()
        }
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        map = googleMap;
    }

    private fun showNextCheckpoint() {
        ++currentDataSourceIndex
        handleNextCheckpointMapMovement()
    }

    private fun handleNextCheckpointMapMovement() {
        if (currentDataSourceIndex < checkPointsDataSource.count()) {

            currentMarker?.remove()

            circle?.remove()

            nextCheckpointValueTextView?.text = checkPointsDataSource[currentDataSourceIndex].nextCp

            currentMarker =
                map?.addMarker(checkPointsDataSource[currentDataSourceIndex].coords?.let {
                    MarkerOptions().position(
                        it
                    )
                })

            map?.moveCamera(CameraUpdateFactory.newLatLng(checkPointsDataSource[currentDataSourceIndex].coords))

            map?.animateCamera(CameraUpdateFactory.zoomTo(15f), 2000, null);

            checkPointsDataSource[currentDataSourceIndex].coords?.let { addGeofence(it, 70f) }

            checkPointsDataSource[currentDataSourceIndex].coords?.let {
                addGeofenceCircle(
                    it,
                    70.0
                )
            }

            routeRequest()

        } else if (currentDataSourceIndex == checkPointsDataSource.count()) {
            stopChronometer()
            Toast.makeText(context, "Parabéns!!! Percurso concluido com sucesso", Toast.LENGTH_LONG).show()
            var run = Run();
            run.age = args?.age.toString()
            run.name = args?.participantName.toString()
            if (args?.sex == 0) {
                run.sexo = "Masculino"
            } else {
                run.sexo = "Feminino"
            }
            run.distance = distanceInM.div(1000F)

            runViewModel?.insertRun(run)

        }
    }

    private fun routeRequest() {
        if (currentCoords != null && currentMarker != null) {

            var url: String =
                "https://maps.googleapis.com/maps/api/directions/json?mode=walking&origin=" + currentCoords!!.latitude.toString() + "," + currentCoords!!.longitude.toString() + "&destination=" + currentMarker!!.position.latitude.toString() + "," + currentMarker!!.position.longitude.toString() + "&key=" + getString(
                    R.string.map_key
                );

            AsyncTaskHandleJson().execute(url)
        }
    }

    inner class AsyncTaskHandleJson : AsyncTask<String, String, String>() {
        override fun doInBackground(vararg params: String?): String {
            var text: String = ""
            val conn = URL(params[0]).openConnection() as HttpURLConnection
            var json: JSONObject? = null;

            try {
                conn.connect()
                text = conn.inputStream.use {
                    it.reader().use { reader -> reader.readText() }
                }

            } catch (e: Exception) {
                var r = Runnable {
                    run {
                        Toast.makeText(
                            context,
                            "Error getting connection",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                uiHandler?.post(r)
            }

            drawRouteAndPersonCustomMarker(text)

            return text;

        }
    }

    private fun drawRouteAndPersonCustomMarker(response: String) {
        if (response != null && !response.isEmpty()) {
            if (routes != null && routes.count() > 0) {
                var r: Runnable = Runnable {
                    run {
                        for (i in 0..routes.count() - 1) {

                            routes[i].remove()
                        }
                        routes = ArrayList();
                    }
                }

                uiHandler?.post(r)
            }

            var j = JSONObject(response)

            var jA = j!!.getJSONArray("routes")
                .getJSONObject(0)
                .getJSONArray("legs")
                .getJSONObject(0)
                .getJSONArray("steps")

            var c = jA.length()

            var pA = ArrayList<String>()

            var j2: JSONObject

            for (i in 0..c - 1) {
                j2 = jA.getJSONObject(i)

                var p: String = j2.getJSONObject("polyline").getString("points")

                pA.add(p)
            }

            var c2 = pA.count()

            for (i in 0..c2 - 1) {
                var o2 = PolylineOptions()

                o2.color(Color.BLUE)

                o2.width(5f)

                o2.addAll(PolyUtil.decode(pA[i]))

                var r: Runnable = Runnable {
                    run {
                        var p = map?.addPolyline(o2);

                        if (p != null) {
                            routes.add(p)
                        }

                    }
                }

                uiHandler?.post(r)

            }

            var r: Runnable = Runnable {
                run {
                    userCustomMarker?.remove()

                    userCustomMarker =
                        map?.addMarker(currentCoords?.let { MarkerOptions().position(it) })

                    userCustomMarker?.setIcon(context?.let {
                        bitmapDescriptorFromVector(
                            it,
                            R.drawable.custom_marker_icon
                        )
                    })
                }
            }

            uiHandler?.post(r)

        }

    }

    private fun bitmapDescriptorFromVector(
        context: Context,
        @DrawableRes vectorDrawableResourceId: Int
    ): BitmapDescriptor? {
        val background =
            ContextCompat.getDrawable(context, R.drawable.custom_marker_icon)
        background!!.setBounds(0, 0, background.intrinsicWidth, background.intrinsicHeight)
        val vectorDrawable = ContextCompat.getDrawable(context, vectorDrawableResourceId)
        vectorDrawable!!.setBounds(
            40,
            20,
            vectorDrawable.intrinsicWidth + 40,
            vectorDrawable.intrinsicHeight + 20
        )
        val bitmap = Bitmap.createBitmap(
            background.intrinsicWidth,
            background.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        background.draw(canvas)
        vectorDrawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }


    private fun createDataSource() {
        //checkPointsDataSource.add(CheckpointModel("SeilA", LatLng(39.734144, -8.791863), "LA SEI"))
        //checkPointsDataSource.add(CheckpointModel("miradouro_ernesto", LatLng(39.746482, -8.809401), "Miradouro Ernesto Korrodi"))
        /*checkPointsDataSource.add(
            CheckpointModel(
                "fonte_tres_bicas",
                LatLng(39.743068, -8.805635),
                "Fonte das três bicas"
            )
        )
        checkPointsDataSource.add(
            CheckpointModel(
                "parque_aviao",
                LatLng(39.745650, -8.803727),
                "Parque do avião"
            )
        )
        checkPointsDataSource.add(
            CheckpointModel(
                "afonso_lopes_vieira",
                LatLng(39.744278, -8.808344),
                "Estátua Afonso Lopes Vieira"
            )
        )
        //checkPointsDataSource.add(CheckpointModel("se_leiria", LatLng(39.746168, -8.806836), "Sé de Leiria"))
        //checkPointsDataSource.add(CheckpointModel("mimo", LatLng(39.747533, -8.807219), "Museu da imagem e do movimento" ))
        checkPointsDataSource.add(
            CheckpointModel(
                "museu_leiria",
                LatLng(39.741834, -8.802860),
                "Museu de Leiria"
            )
        )
        checkPointsDataSource.add(
            CheckpointModel(
                "largo_candido_reis",
                LatLng(39.744464, -8.809540),
                "Largo Candido Reis (Terreiro)"
            )
        )
        checkPointsDataSource.add(
            CheckpointModel(
                "jardim_santo_agostinho",
                LatLng(39.741347, -8.801447),
                "Jardim Santo Agostinho"
            )
        )
        checkPointsDataSource.add(
            CheckpointModel(
                "encarnacao",
                LatLng(39.738997, -8.799663),
                "Capela da Nossa Senhora da Encarnação"
            )
        )
        checkPointsDataSource.add(
            CheckpointModel(
                "jardim_luis_camoes",
                LatLng(39.744753, -8.806314),
                "Jardim Luis de Camões"
            )
        )
        */

        checkPointsDataSource.add(
            CheckpointModel(
                "fonte_luminosa",
                LatLng(39.743775, -8.806916),
                "Fonte Luminosa"
            )
        )
    }


    @SuppressLint("MissingPermission")
    private fun addGeofence(latLng: LatLng, radius: Float) {
        geofence = geofenceHelper?.getGeofence(
            geoFenceId,
            latLng,
            radius,
            Geofence.GEOFENCE_TRANSITION_ENTER
        )

        var geofencingRequest = geofence?.let { geofenceHelper?.getGeofencingRequest(it) }

        var pendingIntent: PendingIntent? = geofenceHelper?.getPendingIntent()

        geofencingClient?.addGeofences(geofencingRequest, pendingIntent)
            ?.addOnSuccessListener {
                onSuccessGeofencingListener()
            }

            ?.addOnFailureListener {
                onFailtureGeofencingListener(it)
            }
    }

    private fun onSuccessGeofencingListener() {
        var r = Runnable {
            run {
                Toast.makeText(
                    context,
                    "Geofence added",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        uiHandler?.post(r)
    }

    private fun onFailtureGeofencingListener(e: Exception) {
        var errMessage: String? = geofenceHelper?.getErrorString(e)

        if (errMessage != null) {
            var r = Runnable {
                run {
                    Toast.makeText(
                        context,
                        "GEO_FENCE_ERROR:" + errMessage,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            uiHandler?.post(r)

        }
    }

    private fun addGeofenceCircle(coords: LatLng, radius: Double) {

        var circleOpt = CircleOptions()
            .center(coords)
            .radius(radius)
            .strokeColor(Color.BLACK)
            .fillColor(Color.BLUE)
            .strokeWidth(3f)


        circle = map?.addCircle(circleOpt)

    }

    fun startChronometer() {
        if (!running) {
            chronometer?.start()
            running = true;
        }
    }

    fun stopChronometer() {
        if (running) {
            chronometer?.stop()
            running = false;
        }
    }


    companion object {

        /** Tag for the [Log].  */
        private const val TAG = "StillImageActivity"

        /** Request code for starting photo capture activity  */
        private const val REQUEST_IMAGE_CAPTURE = 1

        /** Request code for starting photo library activity  */
        private const val REQUEST_PHOTO_LIBRARY = 2

    }
}

