package com.example.leirifit

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context.LOCATION_SERVICE
import android.content.Intent
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.example.leirifit.databinding.FragmentMainPageBinding
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.firebase.ml.common.FirebaseMLException
import com.google.firebase.ml.vision.common.FirebaseVisionImage
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

    // maps
    private var map: GoogleMap? = null
    private var mapFragment: SupportMapFragment? = null
    private var checkPointsDataSource = ArrayList<LatLng>();
    private var currentDataSourceIndex = 0;
    private var currentMarker: Marker? = null;

    // location
    private var locationManager: LocationManager? = null;
    private var currentCoords: LatLng? = null;
    private var route: Polyline? = null;

            override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val binding = DataBindingUtil.inflate<FragmentMainPageBinding>(
            inflater,
            R.layout.fragment_main_page, container, false
        )
        val args = MainFragmentArgs.fromBundle(arguments!!)
        Toast.makeText(
            context,
            "Name: ${args.participantName}, Idade: ${args.age}, Sexo: ${args.sex}",
            Toast.LENGTH_LONG
        ).show()

        try {
            classifier = ImageClassifier(requireActivity())
        } catch (e: FirebaseMLException) {
           // textView?.text = getString(R.string.fail_to_initialize_img_classifier)
        }
        textView = binding.legendTextView
        imagePreview = binding.imagePreview
        binding.cameraImageButton.setOnClickListener { takePhoto() }
                // map
        createDataSource()
        handleMapCreation()

        return binding.root
    }

    override fun onDestroy() {
        classifier?.close()
        super<Fragment>.onDestroy()
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
                FirebaseVisionImage.fromFilePath(requireContext(), Uri.fromFile(currentPhotoFile)).also {
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
        classifier?.classifyFrame(bitmap)?.
        addOnCompleteListener { task ->
            if (task.isSuccessful) {
                textView?.text = task.result
            } else {
                val e = task.exception
                Log.e(TAG, "Error classifying frame", e)
                textView?.text = e?.message
            }
        }
    }

    private fun handleMapCreation() {
        mapFragment = getChildFragmentManager().findFragmentById(R.id.mapView) as SupportMapFragment?
        handleLocationManagerCreation()
        mapFragment?.getMapAsync(OnMapReadyCallback {
                onMapReady(it)
            });
    }

    @SuppressLint("MissingPermission")
    private fun handleLocationManagerCreation() {
        locationManager = context?.getSystemService(LOCATION_SERVICE) as LocationManager?;

        var enable = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if(enable!!) {
            locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0.1f, LocationListener {
                handleLocationUpdates(it)
            })
        }
    }
    private fun handleLocationUpdates(location: Location) {
        if(location != null && location.latitude != null && location.longitude != null) {
            currentCoords = LatLng(location.latitude, location.longitude);
        }
    }

   override fun onMapReady(googleMap: GoogleMap?) {
       map = googleMap;
       handleNextCheckpointMapMovement()
    }

    private fun showNextCheckpoint() {
        currentMarker?.remove()
        ++currentDataSourceIndex
        handleNextCheckpointMapMovement()
        routeRequest()
    }

    private fun handleNextCheckpointMapMovement() {
        if(currentDataSourceIndex < checkPointsDataSource.count()) {
            currentMarker = map?.addMarker(MarkerOptions().position(checkPointsDataSource[currentDataSourceIndex]))

            map?.moveCamera(CameraUpdateFactory.newLatLng(checkPointsDataSource[currentDataSourceIndex]))

            map?.animateCamera(CameraUpdateFactory.zoomTo(15f), 2000, null);

        } else if(currentDataSourceIndex+1 == checkPointsDataSource.count()){
                // TODO: significa que terminou o percurso - mostrar estatísticas/mensagem de ganho
        }
    }

    private fun routeRequest() {
       /* if(currentCoords != null && currentMarker != null) {
            var url: String = "https://maps.googleapis.com/maps/api/directions/json?origin=" +
                    currentCoords!!.latitude.toString() + "," + currentCoords!!.longitude.toString() +
                    "&destination=" +
                    currentMarker!!.position.toString() +
                    "&key=" +
                    getString(R.string.map_key); */
            var url: String = "https://maps.googleapis.com/maps/api/directions/json?origin=" +
                    "39.746482" + "," + "-8.809401" +
                    "&destination=" +
                    "39.743068,-8.805635" +
                    "&key=" +
                    getString(R.string.map_key);

        AsyncTaskHandleJson().execute(url)

       // }
    }

    inner class AsyncTaskHandleJson: AsyncTask<String, String, String>() {
        override fun doInBackground(vararg params: String?): String {
            var text: String = ""
            val conn = URL(params[0]).openConnection() as HttpURLConnection
            var json: JSONObject? = null;

            try {
                conn.connect()
                text = conn.inputStream.use {
                    it.reader().use{reader -> reader.readText()}
                }

            } catch (e: Exception) {
                // TODO: catch error
            }

            drawRoute(text)

            return text;

        }
    }

    private fun drawRoute(response: String) {
        if(route != null) {
            route?.remove()
        }
        var json: JSONObject = JSONObject(response)

//        route = map?.addPolyline(())
    }

    private fun createDataSource() {
        checkPointsDataSource.add(LatLng(39.746482, -8.809401));
        checkPointsDataSource.add(LatLng(39.743068, -8.805635));
        checkPointsDataSource.add(LatLng(39.745650, -8.803727));
        checkPointsDataSource.add(LatLng(39.744278, -8.808344));
        checkPointsDataSource.add(LatLng(39.746168, -8.806836));
        checkPointsDataSource.add(LatLng(39.741834, -8.802860));
        // var startingPointLargoCandidoReis: LatLng =  LatLng(39.751778, -8.809620);
        checkPointsDataSource.add(LatLng(39.741347, -8.801447));
        checkPointsDataSource.add(LatLng(39.738997, -8.799663));
        checkPointsDataSource.add(LatLng(39.744753, -8.806314));
        checkPointsDataSource.add(LatLng(39.743775, -8.806916));
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

