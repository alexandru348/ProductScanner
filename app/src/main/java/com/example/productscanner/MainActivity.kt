package com.example.productscanner

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder



class MainActivity : AppCompatActivity() {

private lateinit var cameraBtn: MaterialButton
    private lateinit var galleryBtn: MaterialButton
    private lateinit var imageIv: ImageView
    private lateinit var scanBtn: MaterialButton
    private lateinit var cropBtn: MaterialButton
    private lateinit var cropOverlay: CropOverlayView
    private lateinit var resultTv: TextView
    private lateinit var productNameTv: TextView
    private lateinit var barcodeTv: TextView
    private lateinit var healthScoreTv: TextView
    private lateinit var healthLevelIndicator: View
    private lateinit var compareTv: TextView
    private lateinit var resultLabelTv: TextView


    companion object{

        private const val CAMERA_REQUEST_CODE = 100
        private const val STORAGE_REQUEST_CODE = 101

        private const val TAG ="MAIN_TAG"
    }

    private fun readImagePermission(): String =
        if(Build.VERSION.SDK_INT >= 33){
            Manifest.permission.READ_MEDIA_IMAGES
        }
    else{
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    private fun needsWriteExternal(): Boolean =
        Build.VERSION.SDK_INT <= 28




       private lateinit var cameraPermissions: Array<String>
       private lateinit var storagePermissions: Array<String>


       private var imageUri: Uri? = null

       private var previousImageUri: Uri? = null


       private var isScanning = false

       private val FALLBACK_MAX_SIZE = 1600

       private val takePhotoLauncher =
           registerForActivityResult(ActivityResultContracts.TakePicture()) {
            success ->
               if(success && imageUri != null) {
                   imageIv.setImageURI(imageUri)
                   scanBtn.isEnabled = true
                   updateCropAvailability(true)
                   previousImageUri = null
               }
               else {
                   imageUri = previousImageUri
                   imageIv.setImageURI(imageUri)

                   val hasImage = (imageUri != null)
                   scanBtn.isEnabled = hasImage
                   updateCropAvailability(hasImage)
                   previousImageUri = null
                   showToast("No picture was taken")
               }
           }

    private fun createImageUri(): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "scan_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ProductScanner")
            }
        }
        return contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues)
    }

    private fun startCameraCapture() {
        if(!checkCameraPermissions()) {
            requestCameraPermission()
            return
        }

        previousImageUri = imageUri // the old picture is saved

        scanBtn.isEnabled = false
        updateCropAvailability(false)

        imageUri = createImageUri()
        if(imageUri == null) {
            imageUri = previousImageUri
            imageIv.setImageURI(imageUri) // to make sure the UI is correct

            val hasImage = (imageUri != null)
            scanBtn.isEnabled = hasImage
            updateCropAvailability(hasImage)

            previousImageUri = null

            showToast("I can't create a file for the photo")
            return
        }

        takePhotoLauncher.launch(imageUri)
    }



       private var barcodeScannerOptions: BarcodeScannerOptions? = null
       private var barcodeScanner: BarcodeScanner? = null

       private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()){
          uri ->
           if(uri != null) {
               imageUri = uri
               imageIv.setImageURI(uri)
               scanBtn.isEnabled = true
               updateCropAvailability(true)
           }
           else {
               scanBtn.isEnabled = (imageUri != null)
               updateCropAvailability(imageUri != null)
               if (imageUri == null) {
                   showToast("Cancelled...")
               }
           }
       }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)


        cameraBtn = findViewById(R.id.cameraBtn)
        galleryBtn = findViewById(R.id.galleryBtn)
        imageIv = findViewById(R.id.imageIv)
        scanBtn = findViewById(R.id.scanBtn)
        scanBtn.isEnabled = false
        cropBtn = findViewById(R.id.cropBtn)
        cropBtn.isEnabled = false
        cropBtn.visibility = View.GONE
        cropOverlay = findViewById(R.id.cropOverlay)
        cropOverlay.isEnabled = false
        cropOverlay.visibility = View.GONE
        resultTv = findViewById(R.id.resultTv)
        resultLabelTv = findViewById(R.id.resultLabelTv)
        compareTv = findViewById(R.id.compareTv)
        productNameTv = findViewById(R.id.productNameTv)
        barcodeTv = findViewById(R.id.barcodeTv)
        healthScoreTv = findViewById(R.id.healthScoreTv)
        healthLevelIndicator = findViewById(R.id.healthLevelIndicator)


        storagePermissions = arrayOf(readImagePermission())

        cameraPermissions =
                if(needsWriteExternal()){
                    arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
                else {
                    arrayOf(Manifest.permission.CAMERA)
                }

        // The following formats are supported:
        //
        //Code 128 (FORMAT_CODE_128)
        //Code 39 (FORMAT_CODE_39)
        //Code 93 (FORMAT_CODE_93)
        //Codabar (FORMAT_CODABAR)
        //EAN-13 (FORMAT_EAN_13)
        //EAN-8 (FORMAT_EAN_8)
        //ITF (FORMAT_ITF)
        //UPC-A (FORMAT_UPC_A)
        //UPC-E (FORMAT_UPC_E)
        //QR Code (FORMAT_QR_CODE)
        //PDF417 (FORMAT_PDF417)
        //Aztec (FORMAT_AZTEC)
        //Data Matrix (FORMAT_DATA_MATRIX) //
        barcodeScannerOptions = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_EAN_8, Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E)
            .build()

        barcodeScanner = BarcodeScanning.getClient(barcodeScannerOptions!!)

        cameraBtn.setOnClickListener {
            startCameraCapture()
        }

        galleryBtn.setOnClickListener {

         if(Build.VERSION.SDK_INT >= 33) {
             pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
         }
         else {
             if(checkStoragePermission()){
                 pickImageGallery()
             }
             else {
                 requestStoragePermission()
             }
         }
        }

        scanBtn.setOnClickListener {
            val uri = imageUri
            if(uri == null){
                showToast("Pick image first")
                return@setOnClickListener
            }
            if(isScanning) {
                return@setOnClickListener
            }

            updateCropAvailability(false)

            isScanning = true
            scanBtn.isEnabled = false
            cameraBtn.isEnabled = false
            galleryBtn.isEnabled = false

            detectResultFromImage(uri, triedDownscaled = false)
        }


        cropBtn.setOnClickListener {
            if (imageUri == null) return@setOnClickListener // without a picture, it makes no sense to crop

            val show = cropOverlay.visibility != View.VISIBLE
            cropOverlay.visibility = if (show) View.VISIBLE else View.GONE
            cropOverlay.isEnabled = show
            if (show) cropOverlay.bringToFront()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onDestroy() {
        try {
            barcodeScanner?.close()
        }
        catch (e: Exception) {
            Log.w(TAG, "Failed to close barcodeScanner", e)
        }
        finally {
            barcodeScanner = null
        }
        super.onDestroy()
    }

    private fun detectResultFromImage(uri: Uri, triedDownscaled: Boolean) {
        Log.d(TAG, "detectResultFromImage: triedDownscaled=$triedDownscaled uri=$uri")

        val scanner = barcodeScanner
        if (scanner == null) {
            showToast("The scanner is not ready")
            finishScanUi()
            return
        }

        try {
            val inputImage = InputImage.fromFilePath(this, uri)

            scanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                Log.d(TAG, "onSuccess: barcodes.size = ${barcodes.size}")
                barcodes.forEachIndexed { i, b  ->
                    Log.d(TAG,
                        "[$i] format=${b.format} rawValue=${b.rawValue} displayValue=${b.displayValue}")
                }
                    val hasSupported1D = barcodes.any { b ->
                        val v = b.rawValue?.trim()
                        val okFormat =
                            b.format == Barcode.FORMAT_EAN_8 ||
                            b.format == Barcode.FORMAT_EAN_13 ||
                            b.format == Barcode.FORMAT_UPC_A ||
                            b.format == Barcode.FORMAT_UPC_E
                        okFormat && !v.isNullOrBlank()
                    }

                    val shouldRetry = !hasSupported1D

                    if(shouldRetry && !triedDownscaled) {
                        Log.d(TAG,"Fallback: retry with downscaled bitmap")
                        detectResultFromImageDownscaled(uri)
                    } else {
                        handleProductBarcodes(barcodes)
                        finishScanUi()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "detectResultFromImage failed", e)
                    showToast("Failed scanning due to ${e.message}")
                    finishScanUi()
                }
        } catch (e: Exception) {
            Log.e(TAG, "detectResultFromImage exception", e)
            showToast("Failed due to ${e.message}")
            finishScanUi()
        }
    }

    private fun detectResultFromImageDownscaled(uri: Uri) {
        val scanner = barcodeScanner
        if(scanner == null) {
            finishScanUi()
            return
        }

        val bmp = loadDownscaledBitmap(uri, FALLBACK_MAX_SIZE)
        if(bmp == null) {

            showToast("Could not decode image")
            finishScanUi()
            return
        }

        try {
            val inputImage = InputImage.fromBitmap(bmp, 0)

            scanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    Log.d(TAG, "downscaled onSuccess: barcodes.size=${barcodes.size}")
                    barcodes.forEachIndexed { i,  b ->
                        Log.d(TAG, "[DS $i] format=${b.format} raw=${b.rawValue} display=${b.displayValue}")
                    }
                    handleProductBarcodes(barcodes)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Downscaled scan failed", e)
                    showToast("Failed scanning due to ${e.message}")
                }
                .addOnCompleteListener {
                    finishScanUi()
                    if(!bmp.isRecycled) bmp.recycle()
                }
        } catch (e: Exception) {
            Log.e(TAG, "Downscaled scan exception", e)
            showToast("Failed scanning due to ${e.message}")
            finishScanUi()
            if(!bmp.isRecycled) bmp.recycle()
        }
    }

    private fun loadDownscaledBitmap(uri: Uri, maxSize: Int): Bitmap? {
        return try {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    val w = info.size.width
                    val h = info.size.height
                    val maxDimension = maxOf(w, h)

                    if (maxDimension > maxSize) {
                        val scale = maxDimension.toFloat() / maxSize.toFloat()
                        val tw = (w / scale).toInt().coerceAtLeast(1)
                        val th = (h / scale).toInt().coerceAtLeast(1)
                        decoder.setTargetSize(tw, th)
                    }
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } else {

                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                contentResolver.openInputStream(uri)?.use {input ->
                    BitmapFactory.decodeStream(input, null, bounds)
                }

                val (w, h) = bounds.outWidth to bounds.outHeight
                if(w <= 0 || h <= 0) return null

                val sample = calculateInSampleSize(w, h, maxSize)

                val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                contentResolver.openInputStream(uri)?.use{ input ->
                    BitmapFactory.decodeStream(input, null, opts)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "loadDownscaledBitmap failed", e)
            null
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxSize: Int): Int {
        var inSampleSize = 1
        while (width / inSampleSize > maxSize || height / inSampleSize > maxSize) {
            inSampleSize = inSampleSize * 2
        }
        return inSampleSize.coerceAtLeast(1)
    }
    private fun handleProductBarcodes(barcodes: List<Barcode>) {
    Log.d(TAG, "handleProductBarcodes() got ${barcodes.size} items")
        if (barcodes.isEmpty()) {
            productNameTv.text = ""
            barcodeTv.text = ""
            healthScoreTv.text = ""
            healthLevelIndicator.visibility = View.GONE
            healthLevelIndicator.setBackgroundColor(Color.TRANSPARENT)
            resultTv.text = getString(R.string.msg_no_codes)
            compareTv.text = ""
            resultLabelTv.visibility = View.GONE
            Log.d(TAG, "handleProductBarcodes(): barcodes is Empty -> show 'no codes' ")
            return
        }

        val productBarcodes = mutableListOf<String>()

        for ((i, barcode) in barcodes.withIndex()) {
            Log.d(TAG, "[$i] format=${barcode.format} raw=${barcode.rawValue} display=${barcode.displayValue}")
            val value = barcode.rawValue?.trim() ?: continue
            val okFormat =
                barcode.format == Barcode.FORMAT_EAN_8 ||
                barcode.format == Barcode.FORMAT_EAN_13 ||
                barcode.format == Barcode.FORMAT_UPC_A ||
                barcode.format == Barcode.FORMAT_UPC_E

            if(okFormat && value.isNotBlank()) {
                productBarcodes += value
            }
        }

        Log.d(TAG, "productBarcodes=${productBarcodes.joinToString()}")

        if (productBarcodes.isEmpty()) {
            productNameTv.text = ""
            barcodeTv.text = ""
            healthScoreTv.text = ""
            healthLevelIndicator.visibility = View.GONE
            healthLevelIndicator.setBackgroundColor(Color.TRANSPARENT)
            resultTv.text = getString(R.string.msg_no_ean_upc)
            compareTv.text = ""
            resultLabelTv.visibility = View.GONE
            return
        }

        val firstCode = productBarcodes.first()

        Log.d("BARCODE", "Scanned barcode: $firstCode") // Log: print the first detected barcode value to Logcat so we know what is being sent further

        if(!hasInternetConnection()) {

            Log.d("BARCODE", "No internet connection, using local repository") // Log: no internet -> skip Firestore and use local product database

            val evaluation = LocalProductRepository.evaluate(barcode = firstCode)

            showProductEvaluation(evaluation)

            return
        }
        OnlineProductRepository.evaluate(
            barcode = firstCode,

            onResult = { onlineEvaluation ->

                Log.d("FIRESTORE", "onResult: onlineEvaluation = $onlineEvaluation") // Log: see what response came back from Firestore (null or a complete ProductEvaluation object)

                val evaluation = onlineEvaluation

                    ?: LocalProductRepository.evaluate(barcode = firstCode)

                showProductEvaluation(evaluation)
            },

            onError = { e ->
                Log.e("BARCODE", "Error loading from Firestore", e) // Log (error): something went wrong while reading data from Firestore; print the exception for debugging

                val evaluation = LocalProductRepository.evaluate(barcode = firstCode)

                showProductEvaluation(evaluation)
            }
        )
    }

    private fun showProductEvaluation(evaluation: ProductEvaluation) {

        resultLabelTv.visibility = View.VISIBLE

        resultLabelTv.text = getString(R.string.label_result)

        val levelTextResId = when (evaluation.level) {
            HealthLevel.HEALTHY -> R.string.health_level_healthy
            HealthLevel.MODERATE -> R.string.health_level_moderate
            HealthLevel.UNHEALTHY -> R.string.health_level_unhealthy
        }

        val levelText = getString(levelTextResId)

        productNameTv.text = getString(

            R.string.product_name_format,

            evaluation.productName

        )

        barcodeTv.text = getString(

            R.string.result_barcode_format,

            evaluation.barcode

        )

        healthScoreTv.text = getString(

            R.string.health_score_format,

            evaluation.healthScore,

            levelText

        )

        val categoryText = buildString {

            appendLine(getString(R.string.result_category_title))

            append(evaluation.explanation)

        }

        resultTv.text = categoryText

        if (!evaluation.compareHint.isNullOrBlank()) {

            val compareText = buildString {

                appendLine(getString(R.string.result_compare_title))

                append(evaluation.compareHint)

            }

            compareTv.text = compareText

        } else {

            compareTv.text = ""

        }

        val color = when (evaluation.level) {
            HealthLevel.HEALTHY -> Color.parseColor("#4CAF50")
            HealthLevel.MODERATE -> Color.parseColor("#FFC107")
            HealthLevel.UNHEALTHY -> Color.parseColor("#F44336")
        }
        healthLevelIndicator.visibility = View.VISIBLE
        healthLevelIndicator.setBackgroundColor(color)
    }

    private fun pickImageGallery() {
        val pm = packageManager
        val galleryPkgs = listOf(
            "com.simplemobiletools.gallery.pro",
            "com.simplemobiletools.gallery"
        )

        var intent: Intent? = null
        for (pkg in galleryPkgs) {
            val i = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
                type = "image/*"
                setPackage(pkg)
            }
            if (i.resolveActivity(pm) != null) { intent = i; break }
        }

        if (intent == null) {
            intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "image/*"
                    putExtra(Intent.EXTRA_LOCAL_ONLY, true)
                    addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                    )
                }
            } else {
                Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "image/*"
                    putExtra(Intent.EXTRA_LOCAL_ONLY, true)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
        }
        galleryActivityResultLauncher.launch(intent)
    }

    private val galleryActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->

        // 1) User a ieșit din galerie / a dat Back / Cancel
        if (result.resultCode != Activity.RESULT_OK) {
            scanBtn.isEnabled = (imageUri != null)
            updateCropAvailability(imageUri != null)
            if (imageUri == null) {
                showToast("Cancelled...")
            }
            return@registerForActivityResult
        }

        // 2) User a selectat ceva (sau ar trebui). Luăm Uri-ul.
        val data = result.data
        val newUri: Uri? = data?.data

        // 3) Dacă a intrat dar nu a ales nimic (sau provider-ul nu a întors uri)
        if (newUri == null) {
            scanBtn.isEnabled = (imageUri != null)
            updateCropAvailability(imageUri != null)
            if (imageUri == null) {
                showToast("Cancelled...")
            }
            return@registerForActivityResult
        }

        // 4) Avem Uri valid => păstrăm imaginea și activăm scan
        imageUri = newUri
        imageIv.setImageURI(newUri)
        scanBtn.isEnabled = true
        updateCropAvailability(true)

        // 5) Încercăm să păstrăm permisiunea (merge în special cu ACTION_OPEN_DOCUMENT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {

            val persistable = (data?.flags ?: 0) and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION

            val modeFlags = (data?.flags ?: 0) and (Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

            val hasPersistableFlag = persistable != 0

            if (hasPersistableFlag && modeFlags != 0) {
                try {
                    contentResolver.takePersistableUriPermission(newUri, modeFlags)
                } catch (e: Exception) {
                    Log.w(TAG, "takePersistableUriPermission failed", e)
                }
            }
        }
    }

    private fun checkStoragePermission(): Boolean {

        return ContextCompat.checkSelfPermission(this, readImagePermission()) ==
                PackageManager.PERMISSION_GRANTED

    }


    private fun requestStoragePermission(){

        ActivityCompat.requestPermissions(this, storagePermissions, STORAGE_REQUEST_CODE)
    }

    private fun checkCameraPermissions(): Boolean {

        val cam = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        val writeOk = if(needsWriteExternal()){
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED
        }
        else {
            true
        }
        return cam && writeOk
    }

    private fun requestCameraPermission(){

        ActivityCompat.requestPermissions(this, cameraPermissions, CAMERA_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when(requestCode){
            CAMERA_REQUEST_CODE -> {

                if(grantResults.isNotEmpty()){

                    val cameraAccepted = grantResults.getOrNull(0) == PackageManager.PERMISSION_GRANTED
                    val writeAccepted = if(needsWriteExternal()){
                        grantResults.getOrNull(1) == PackageManager.PERMISSION_GRANTED
                    }
                    else {
                        true
                    }

                    if(cameraAccepted == true && writeAccepted){

                        startCameraCapture()
                    }
                    else{

                        showToast("Camera & Storage permissions are required")
                    }
                }
            }
            STORAGE_REQUEST_CODE -> {

                if(grantResults.isNotEmpty()){

                    val storageAccepted = grantResults.getOrNull(0) == PackageManager.PERMISSION_GRANTED

                    if(storageAccepted == true){

                        pickImageGallery()
                    }
                    else{

                        showToast("Storage permission is required...")
                    }
                }
            }
        }
    }

    private fun showToast(message: String){
        Toast.makeText(this,message, Toast.LENGTH_SHORT).show()
    }

    private fun finishScanUi() {
        isScanning = false
        val hasImage = (imageUri != null)
        scanBtn.isEnabled = hasImage
        updateCropAvailability(hasImage)
        cameraBtn.isEnabled = true
        galleryBtn.isEnabled = true
    }

    private fun updateCropAvailability(hasImage: Boolean) {
        cropBtn.isEnabled = hasImage
        cropBtn.visibility = if (hasImage) View.VISIBLE else View.GONE

        // The overlay stays hidden by default; shown only when entering Crop mode
            cropOverlay.isEnabled = false
            cropOverlay.visibility = View.GONE

    }

    private fun hasInternetConnection(): Boolean {

        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = connectivityManager.activeNetwork ?: return false

        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||

                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||

                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }
}