package com.example.brain_booster.utilis

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.brain_booster.models.BoardSize


class Utility(val boardSize: BoardSize)

fun Context.showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

fun requestPermission(activity: Activity?, permission: String, requestCode: Int) {
    ActivityCompat.requestPermissions(
        activity!!,
        arrayOf(permission),
        requestCode
    )
}

fun isPermissionGranted(context: Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        permission
    ) == PackageManager.PERMISSION_GRANTED
}

fun Context.showPositiveAlertDialogue(title: String? = null, message: String? = null) {
    AlertDialog.Builder(this)
        .setTitle(title)
        .setMessage(message)
        .setPositiveButton("Ok", null)
        .show()
}