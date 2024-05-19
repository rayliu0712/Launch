package rl.pusher

import android.app.Activity
import android.util.Log
import android.widget.Toast

object Ez {
    fun log(vararg any: Any) {
        Log.d("666", any.joinToString(" "))
    }

    fun toast(activity: Activity, vararg any: Any) {
        activity.runOnUiThread {
            Toast.makeText(activity, any.joinToString(" "), Toast.LENGTH_SHORT).show()
        }
    }

    fun millis(): Long {
        return System.currentTimeMillis()
    }
}