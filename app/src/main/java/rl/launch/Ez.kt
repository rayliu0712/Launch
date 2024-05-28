package rl.launch

import android.util.Log
import android.widget.Toast
import java.io.File

object Ez {
    lateinit var ma: MainActivity

    fun ui(action: MainActivity.() -> Unit) {
        ma.runOnUiThread { ma.action() }
    }

    fun toast(vararg any: Any) {
        ui {
            Toast.makeText(this, any.joinToString(" "), Toast.LENGTH_SHORT).show()
        }
    }

    fun totalLength(root: File): Long {
        return root.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
    }

    fun sizeWithUnit(length: Long): String {
        val units = arrayOf("B", "KiB", "MiB", "GiB", "TiB")
        var size = length.toDouble()
        var i = 0
        while (size >= 1024 && i < 5) {
            size /= 1024
            i++
        }
        return "%.2f ${units[i]}".format(size)
    }

    fun isNotASCII(pattern: String): Boolean {
        return !pattern.matches("\\A\\p{ASCII}*\\z".toRegex())
    }

    fun log(vararg any: Any) {
        Log.d("666", any.joinToString(" "))
    }
}