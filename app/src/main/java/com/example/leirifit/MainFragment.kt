package com.example.leirifit

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.example.leirifit.databinding.FragmentMainPageBinding
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.ml.common.FirebaseMLException
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


/**
 * Run page fragment
 */
// OnMapReadyCallback
class MainFragment : Fragment(), OnMapReadyCallback {

    private var currentPhotoFile: File? = null
    private var classifier: ImageClassifier? = null
    private var imagePreview: ImageView? = null
    private var textView: TextView? = null

    private var map: GoogleMap? = null
    private var mapFragment: SupportMapFragment? = null
    private var checkPointsDataSource = ArrayList<LatLng>();
    private var currentDataSourceIndex = 0;
    private var currentMarker: Marker? = null;

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
        showNextCheckpoint()
       /* Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
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
         }*/
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

        mapFragment?.getMapAsync(OnMapReadyCallback {
                onMapReady(it)
            });
    }

   override fun onMapReady(googleMap: GoogleMap?) {
       map = googleMap;
       handleNextCheckpointMapMovement()
    }

    private fun showNextCheckpoint() {
        currentMarker?.remove()
        ++currentDataSourceIndex
        handleNextCheckpointMapMovement()
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

