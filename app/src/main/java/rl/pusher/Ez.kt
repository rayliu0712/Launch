package rl.pusher

import android.content.Context
import android.util.Log
import android.widget.Toast

object Ez {
    fun log(vararg any: Any) {
        Log.d("666", any.joinToString(" "))
    }

    fun toast(context: Context, vararg any: Any) {
        Toast.makeText(context, any.joinToString(" "), Toast.LENGTH_SHORT).show()
    }

    fun isASCII(s: String): Boolean {
        return s.matches("\\A\\p{ASCII}*\\z".toRegex())
    }


}