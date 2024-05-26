package rl.launch

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.widget.Toast
import android.provider.MediaStore.Images

object Ez {
    fun log(vararg any: Any) {
        Log.d("666", any.joinToString(" "))
    }

    fun toast(att: Activity, vararg any: Any) {
        att.runOnUiThread {
            Toast.makeText(att, any.joinToString(" "), Toast.LENGTH_SHORT).show()
        }
    }

    fun millis(): Long {
        return System.currentTimeMillis()
    }
}