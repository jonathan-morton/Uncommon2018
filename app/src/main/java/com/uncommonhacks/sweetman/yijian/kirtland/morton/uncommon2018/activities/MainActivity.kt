package com.uncommonhacks.sweetman.yijian.kirtland.morton.uncommon2018.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.design.widget.Snackbar
import android.support.media.ExifInterface
import android.support.v4.app.ActivityCompat
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.gson.Gson
import com.uncommonhacks.sweetman.yijian.kirtland.morton.uncommon2018.BuildConfig
import com.uncommonhacks.sweetman.yijian.kirtland.morton.uncommon2018.R
import com.uncommonhacks.sweetman.yijian.kirtland.morton.uncommon2018.models.*
import com.uncommonhacks.sweetman.yijian.kirtland.morton.uncommon2018.networking.GoogleVisionApi
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import net.trippedout.cloudvisionlib.ImageUtil
import org.jetbrains.anko.doAsync
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    var mCurrentPhotoPath: String? = null
    val resizeWidth = 1080
    val resizeHeight = 1920

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /*// Example of a call to a native method
        sample_text.text = stringFromJNI()*/

        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        val rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        if (rc != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission()
        }

        pictureButton.setOnClickListener {
            dispatchTakePictureIntent()
        }
    }

    /**
     * Handles the requesting of the camera permission.  This includes
     * showing a "Snackbar" message of why the permission is needed then
     * sending the request.
     */
    private fun requestCameraPermission() {
        Timber.w("Camera permission is not granted. Requesting permission")

        val permissions = arrayOf(Manifest.permission.CAMERA)

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM)
            return
        }

        val thisActivity = this

        val listener = View.OnClickListener {
            ActivityCompat.requestPermissions(thisActivity, permissions,
                    RC_HANDLE_CAMERA_PERM)
        }

        Snackbar.make(rootConstraintLayout, R.string.permission_camera_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction("OK", listener)
                .show()
    }

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            // Create the File where the photo should go
            var photoFile: File? = null
            try {
                photoFile = createImageFile()
            } catch (ex: IOException) {
            }// Error occurred while creating the File

            // Continue only if the File was successfully created
            if (photoFile != null) {
                val photoURI = FileProvider.getUriForFile(this,
                        BuildConfig.APPLICATION_ID + ".provider",
                        createImageFile());
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            // Show the thumbnail on ImageView
            val imageUri = Uri.parse(mCurrentPhotoPath)
            val file = File(imageUri.getPath())
            val imageFileInputStream = FileInputStream(file)
            val apiService = GoogleVisionApi.RestClient.create(this)
            doAsync {
                try {
                    runOnUiThread {
                        Snackbar.make(backgroundImageView, "Making request please wait", Snackbar.LENGTH_SHORT)
                        backgroundImageView.setColorFilter(this@MainActivity.getResources().getColor(R.color.colorAccent));
                        pictureButton.setText("Please Wait")
                    }

                    val originalImage = BitmapFactory.decodeFile(imageUri.getPath())
                    val rotatedImage = getRotatedImage(imageFileInputStream, originalImage)
                    originalImage.recycle()

                    val resizedImage = getResizedBitmap(rotatedImage!!, resizeWidth, resizeHeight)
                    val base64Image = ImageUtil.getEncodedImageData(resizedImage)
                    resizedImage

                    val requestLogos = RequestLogos(listOf(
                            RequestsItem(Image(base64Image.toString()),
                                    listOf(FeaturesItem()))))
                    val rawJson = Gson().toJson(requestLogos)
                    Timber.d(rawJson)

                    val key = BuildConfig.VISION_API_KEY
                    apiService.locateLogos(apiKey = key, body = requestLogos)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({ result ->
                                val logos = Gson().fromJson(result.string(), LogoResponse::class.java)
                                //val logos = result
                                Log.d("", "Got a result")
                                runOnUiThread {
                                    val censored = censorBitmap(logos, resizedImage)
                                    resizedImage.recycle()
                                    backgroundImageView.setColorFilter(this@MainActivity.getResources().getColor(R.color.transparent))
                                    backgroundImageView.setImageBitmap(censored)
                                    pictureButton.setText("Take Picture")
                                    Log.d("", "Got a result from UI")
                                    Toast.makeText(this@MainActivity, "Got a logo", Toast.LENGTH_SHORT)


                                }
                            }, {
                                Timber.e(it)
                                Log.d("", "Got garbage")
                                runOnUiThread {
                                    Snackbar.make(rootConstraintLayout, "No logos found", Snackbar.LENGTH_SHORT)
                                }

                            })

                    Timber.d(base64Image.toString())
                } catch (e: FileNotFoundException) {
                    Timber.d(e.localizedMessage)
                }
            }

            // ScanFile so it will be appeared on Gallery
            MediaScannerConnection.scanFile(this@MainActivity,
                    arrayOf(imageUri.getPath()), null,
                    object : MediaScannerConnection.OnScanCompletedListener {
                        override fun onScanCompleted(path: String, uri: Uri) {}
                    })
        }


    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(
                imageFileName, /* prefix */
                ".jpg", /* suffix */
                storageDir      /* directory */
        )

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.absolutePath
        return image
    }

    fun getResizedBitmap(bitmap: Bitmap, newWidth: Int, newHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val scaleWidth = newWidth.toFloat() / width
        val scaleHeight = newHeight.toFloat() / height
        // CREATE A MATRIX FOR THE MANIPULATION
        val matrix = Matrix()
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight)

        // "RECREATE" THE NEW BITMAP
        val resizedBitmap = Bitmap.createBitmap(
                bitmap, 0, 0, width, height, matrix, true)
        bitmap.recycle()
        return resizedBitmap
    }

    fun getRotatedImage(imageFis: FileInputStream, bitmap: Bitmap): Bitmap? {
        try {
            val exifInterface = ExifInterface(imageFis)
            val rotation: Int
            val orientation = exifInterface.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            rotation = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90;
                ExifInterface.ORIENTATION_ROTATE_180 -> 180;
                ExifInterface.ORIENTATION_ROTATE_270 -> 270;
                else -> 0
            }

            val w = bitmap.width
            val h = bitmap.height

            val mtx = Matrix()
            mtx.postRotate(rotation.toFloat())

            return Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true)
        } catch (e: IOException) {
            // Handle any errors
        } finally {
            if (imageFis != null) {
                try {
                    imageFis.close()
                } catch (ignored: IOException) {
                }

            }
        }
        return null
    }

    fun censorBitmap(response: LogoResponse, bitmap: Bitmap): Bitmap {
        val space = 35
        val x = response.responses!![0]!!.logoAnnotations!![0]!!.boundingPoly!!.vertices!![3]!!.X!!
        var y = response.responses!![0]!!.logoAnnotations!![0]!!.boundingPoly!!.vertices!![3]!!.Y!!
        val oldW = response.responses!![0]!!.logoAnnotations!![0]!!.boundingPoly!!.vertices!![1]!!.X!! - response.responses!![0]!!.logoAnnotations!![0]!!.boundingPoly!!.vertices!![0]!!.X!!
        val oldH = response.responses!![0]!!.logoAnnotations!![0]!!.boundingPoly!!.vertices!![2]!!.Y!! - response.responses!![0]!!.logoAnnotations!![0]!!.boundingPoly!!.vertices!![0]!!.Y!!
        val w = oldW + space - oldW % space
        val h = oldH + space - oldH % space
        y -= h

        val bitmap2 = bitmap.copy(bitmap.config, true)
        for (i in 0 until w / space) {
            for (j in 0 until h / space) {
                val n = space * space
                var r = 0
                var g = 0
                var b = 0
                val pixels = IntArray(space * space)
                bitmap.getPixels(pixels, 0, space, x + i * space, y + j * space, space, space)
                for (pixel in pixels) {
                    r += Color.red(pixel)
                    g += Color.green(pixel)
                    b += Color.blue(pixel)
                }
                Arrays.fill(pixels, Color.rgb(r / n, g / n, b / n))
                bitmap2.setPixels(pixels, 0, space, x + i * space, y + j * space, space, space)
            }
        }
        return bitmap2
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {

        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("native-lib")
        }

        private val REQUEST_IMAGE_CAPTURE = 1
        private val RC_HANDLE_GMS = 9001
        // permission request codes need to be < 256
        private val RC_HANDLE_CAMERA_PERM = 2
    }
}
