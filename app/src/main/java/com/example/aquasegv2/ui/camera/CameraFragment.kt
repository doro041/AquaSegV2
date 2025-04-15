package com.example.aquasegv2.ui.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.aquasegv2.R
import com.example.aquasegv2.databinding.FragmentCameraBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.provider.MediaStore
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.view.PreviewView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.aquasegv2.DrawImages
import com.example.aquasegv2.InstanceSegmentation
import com.example.aquasegv2.SegmentationResult
import com.example.aquasegv2.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileOutputStream

class CameraFragment : Fragment(), InstanceSegmentation.InstanceSegmentationListener {

    private lateinit var instanceSegmentation: InstanceSegmentation
    private lateinit var drawImages: DrawImages
    private lateinit var previewView: PreviewView

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private var segmentedBitmap: Bitmap? = null
    private var originalBitmap: Bitmap? = null
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Check camera permissions and start the camera if granted.
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(), REQUIRED_PERMISSIONS, 10
            )
        }

        drawImages = DrawImages(requireContext().applicationContext)

        instanceSegmentation = InstanceSegmentation(
            context = requireContext().applicationContext,
            modelPath = "yolo11n-seg_float16.tflite",
            labelPath = null,
            instanceSegmentationListener = this,
            message = {
                Toast.makeText(requireContext().applicationContext, it, Toast.LENGTH_SHORT).show()
            }
        )

        // Set up the capture button to take a photo when clicked.
        binding.captureButton.setOnClickListener {
            saveCombinedImage()
        }

        // Initialize the zoom slider
        setupZoomSlider()

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Reset the zoom bar progress to 0 when the camera starts
            binding.zoomBar.progress = 0

            // Set up the preview use case.
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            // Set up image capture use case.
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build().also {
                    it.setAnalyzer(Executors.newSingleThreadExecutor(), ImageAnalyzer())
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind all use cases before rebinding.
                cameraProvider.unbindAll()

                // Bind use cases to the lifecycle.
                camera = cameraProvider.bindToLifecycle(
                    viewLifecycleOwner, cameraSelector, preview, imageCapture, imageAnalyzer
                )

                // Apply zoom based on the current SeekBar progress (which is 0 by default)
                camera?.cameraControl?.setLinearZoom(binding.zoomBar.progress / 100f)

            } catch (exc: Exception) {

            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    /**
     * Sets up the zoom slider (SeekBar) to control the camera zoom.
     * The slider is set to start with its minimum value.
     */
    private fun setupZoomSlider() {
        binding.zoomBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Apply zoom after the camera is initialized
                camera?.cameraControl?.setLinearZoom(progress / 100f)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun saveCombinedImage() {
        val original = originalBitmap ?: run {
            Log.e("CameraX", "No original bitmap available!")
            Toast.makeText(requireContext().applicationContext, "No original frame to save.", Toast.LENGTH_SHORT).show()
            return
        }

        val segmented = segmentedBitmap ?: run {
            Log.e("CameraX", "No segmented bitmap available!")
            Toast.makeText(requireContext().applicationContext, "No segmentation result to save.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Combine the original and segmented bitmaps.
            val combinedBitmap =
                Bitmap.createBitmap(original.width, original.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(combinedBitmap)
            canvas.drawBitmap(original, 0f, 0f, null)
            canvas.drawBitmap(segmented, 0f, 0f, null)

            // Save the combined bitmap to the Download directory.
            val photoDirectory = File("/storage/emulated/0/Download").apply { mkdirs() }
            val timestamp = System.currentTimeMillis()
            val photoFile = File(photoDirectory, "combined_image_$timestamp.jpg")

            FileOutputStream(photoFile).use { out ->
                combinedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                out.flush()
            }

            addImageToGallery(photoFile)

            Toast.makeText(
                requireContext().applicationContext,
                "Combined Image Saved: ${photoFile.absolutePath}",
                Toast.LENGTH_SHORT
            ).show()
            Log.d("CameraX", "Combined Image saved successfully.")
        } catch (e: Exception) {
            Log.e("CameraX", "Error saving combined image: ${e.message}", e)
        }
    }

    private fun addImageToGallery(file: File) {
        try {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.DATA, file.absolutePath)
            }
            requireContext().contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            Log.d("CameraX", "Image added to gallery: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("CameraX", "Error adding image to gallery: ${e.message}", e)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onDetect(
        interfaceTime: Long,
        results: List<SegmentationResult>,
        preProcessTime: Long,
        postProcessTime: Long
    ) {
        if (!isAdded || activity == null || context == null) {
            // Fragment is no longer attached — skip everything
            return
        }

        if (results.isEmpty()) {
            Log.e("Segmentation", "No results detected!")

            requireActivity().runOnUiThread {
                if (!isAdded || context == null) return@runOnUiThread
                segmentedBitmap = null
                Toast.makeText(requireContext(), "No objects detected for segmentation", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val image = drawImages.invoke(results)
        Log.d("Segmentation", "Segmentation successful, results applied to bitmap.")

        requireActivity().runOnUiThread {
            if (!isAdded || context == null) return@runOnUiThread

            segmentedBitmap = image
            binding.tvPreprocess.text = preProcessTime.toString()
            binding.tvInference.text = interfaceTime.toString()
            binding.tvPostprocess.text = postProcessTime.toString()
            binding.ivTop.setImageBitmap(image)
            }
        }

    override fun onEmpty() {
        requireActivity().runOnUiThread {
            binding.ivTop.setImageResource(0)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        cameraExecutor.shutdown()
    }




    companion object {
        val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA
        ).toTypedArray()
    }

    inner class ImageAnalyzer : ImageAnalysis.Analyzer {
        override fun analyze(imageProxy: ImageProxy) {
            val bitmapBuffer = Bitmap.createBitmap(
                imageProxy.width,
                imageProxy.height,
                Bitmap.Config.ARGB_8888
            )

            imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }
            imageProxy.close()

            val matrix = Matrix().apply {
                postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
            }

            val rotatedBitmap = Bitmap.createBitmap(
                bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height,
                matrix, true
            )
            Log.d("ImageAnalyzer", "Captured frame analyzed. Passing to segmentation model.")
            originalBitmap = rotatedBitmap
            instanceSegmentation.invoke(rotatedBitmap)
        }
    }

    override fun onError(error: String) {
        requireActivity().runOnUiThread {
            Toast.makeText(requireContext().applicationContext, error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }


}
