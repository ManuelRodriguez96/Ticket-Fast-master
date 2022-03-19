package mx.odelant.printorders.activity.main

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.PermissionChecker
import androidx.core.content.PermissionChecker.checkSelfPermission
import mx.odelant.printorders.R
import java.io.File
import java.io.IOException
import java.net.URI

class MainImageUtility(private val activity: Activity) {
    companion object {
        private const val IMAGE_PICK_CODE = 1000
        private const val PERMISSION_CODE = 1001
    }

    fun setPickerListener(imagePickButton: View) {
        imagePickButton.setOnClickListener {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    PermissionChecker.PERMISSION_DENIED
                ) {
                    val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                    requestPermissions(activity, permissions, PERMISSION_CODE)
                } else {
                    //permission already granted
                    pickImageFromGallery()
                }
            } else {
                //system OS is < Marshmallow
                pickImageFromGallery()
            }
        }
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(activity, intent, IMAGE_PICK_CODE, null)
    }

    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    //permission from popup granted
                    pickImageFromGallery()
                } else {
                    //permission from popup denied
                    Toast.makeText(activity, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?, imageView: ImageView) {
        if (resultCode == Activity.RESULT_OK && requestCode == IMAGE_PICK_CODE) {
            imageView.setImageURI(data?.data)

            activity.openFileOutput(activity.getString(R.string.profile_picture), Context.MODE_PRIVATE).use {
                val checkedData = data?.data
                if (checkedData != null) {
                    it.write(readBytes(activity, checkedData))
                }
            }
        }
    }

    fun getProfilePictureFile(): File {
        return activity.getFileStreamPath(activity.getString(R.string.profile_picture))
    }

    @Throws(IOException::class)
    private fun readBytes(context: Context, uri: Uri): ByteArray? =
        context.contentResolver.openInputStream(uri)?.buffered()?.use { it.readBytes() }
}