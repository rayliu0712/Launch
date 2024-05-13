package rl.puller

import android.content.Context
import android.util.Log
import android.widget.Toast

object Ez {
    fun log(any: Any) {
        Log.d("666", any.toString())
    }

    fun toast(context: Context, any: Any) {
        Toast.makeText(context, any.toString(), Toast.LENGTH_SHORT).show()
    }
}