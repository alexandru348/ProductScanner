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
import androidx.core.content.getSystemService


class MainActivity : AppCompatActivity() {

private lateinit var cameraBtn: MaterialButton
    private lateinit var galleryBtn: MaterialButton
    private lateinit var imageIv: ImageView
    private lateinit var scanBtn: MaterialButton
    private lateinit var resultTv: TextView
    private lateinit var productNameTv: TextView

    private lateinit var barcodeTv: TextView
    private lateinit var healthScoreTv: TextView
    private lateinit var healthLevelIndicator: View


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

       private val takePhotoLauncher =
           registerForActivityResult(ActivityResultContracts.TakePicture()) {
            success ->
               if(success && imageUri != null) {
                   imageIv.setImageURI(imageUri)
               }
               else {
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



       private var barcodeScannerOptions: BarcodeScannerOptions? = null
       private var barcodeScanner: BarcodeScanner? = null

       private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()){
          uri ->
           if(uri != null) {
               imageUri = uri
               imageIv.setImageURI(uri)
           }
           else {
               showToast("Cancelled...")
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
        resultTv = findViewById(R.id.resultTv)
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

            if(checkCameraPermissions()){
                imageUri = createImageUri()
                if (imageUri == null) {
                    showToast("I can't create a file for the photo")
                    return@setOnClickListener
                }
                takePhotoLauncher.launch(imageUri)
            }
            else{
                requestCameraPermission()
            }
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
            if(imageUri == null){
                showToast("Pick image first")
            }
            else{

                detectResultFromImage()
            }
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

    private fun detectResultFromImage(){
        Log.d(TAG, "detectResultFromImage: ") // Log: entry point for starting barcode detection from the selected image
        try {

            val inputImage = InputImage.fromFilePath(this, imageUri!!)

            val barcodeResult = barcodeScanner!!.process(inputImage)
                .addOnSuccessListener {barcodes ->

                    handleProductBarcodes(barcodes)
                }
                .addOnFailureListener {e ->
                    Log.e(TAG, "detectResultFromImage: ", e)
                    showToast("Failed scanning due to ${e.message}")
                }
        }
        catch (e: Exception){
            Log.e(TAG, "detectResultFromImage: ", e)
            showToast("Failed due to ${e.message}")
        }
    }

    private fun handleProductBarcodes(barcodes: List<Barcode>) {

        if (barcodes.isEmpty()) {
            productNameTv.text = ""
            barcodeTv.text = ""
            healthScoreTv.text = ""
            healthLevelIndicator.setBackgroundColor(Color.TRANSPARENT)
            resultTv.text = getString(R.string.msg_no_codes)
            return
        }

        val productBarcodes = mutableListOf<String>()

        for (barcode in barcodes) {
            if (barcode.valueType == Barcode.TYPE_PRODUCT) {
                val value = barcode.rawValue ?: continue
                productBarcodes += value
            }
        }

        if (productBarcodes.isEmpty()) {
            productNameTv.text = ""
            barcodeTv.text = ""
            healthScoreTv.text = ""
            healthLevelIndicator.setBackgroundColor(Color.TRANSPARENT)
            resultTv.text = getString(R.string.msg_no_ean_upc)
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

        val text = buildString {

            appendLine(getString(R.string.result_category_title))

            appendLine(evaluation.explanation)

            if(!evaluation.compareHint.isNullOrBlank()) {

                appendLine()

                appendLine(getString(R.string.result_compare_title))

                appendLine(evaluation.compareHint)

            }

        }

        resultTv.text = text

        val color = when (evaluation.level) {
            HealthLevel.HEALTHY -> Color.parseColor("#4CAF50")
            HealthLevel.MODERATE -> Color.parseColor("#FFC107")
            HealthLevel.UNHEALTHY -> Color.parseColor("#F44336")
        }
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

        if (result.resultCode == Activity.RESULT_OK) {

            val data = result.data

            imageUri = data?.data
            Log.d(TAG, "galleryActivityResultLauncher: imageUri: $imageUri")

            if(imageUri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                val takeFlags = (data?.flags ?: 0) and
                        (Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                try {
                    contentResolver.takePersistableUriPermission(imageUri!!,
                        takeFlags)
                }
                catch (e: Exception) {
                    Log.w(TAG, "takePersistableUriPermission failed", e)
                }
            }


            imageIv.setImageURI(imageUri)
        }
        else {
            showToast("Cancelled....!")
        }
    }

    private fun pickImageCamera(){

        val contentValues = ContentValues()
        contentValues.put(MediaStore.Images.Media.TITLE, "Sample Image")
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "Sample Image Description")

        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        cameraActivityResultLauncher.launch(intent)
    }

   private val cameraActivityResultLauncher = registerForActivityResult(
       ActivityResultContracts.StartActivityForResult()
   ){result ->

       if(result.resultCode == Activity.RESULT_OK) {

           val data = result.data

           Log.d(TAG, "cameraActivityResultLauncher: imageUri: $imageUri")
           imageIv.setImageURI(imageUri)
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

                        pickImageCamera()
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

    private fun hasInternetConnection(): Boolean {

        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = connectivityManager.activeNetwork ?: return false

        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||

                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||

                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }
}