package rl.launch

import android.app.AlertDialog
import android.util.Log
import java.io.File

object Ez {
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

    fun ynDialog(title: String, action: MainActivity.() -> Unit) =
        AlertDialog.Builder(sec.me)
            .setCancelable(false)
            .setTitle(title)
            .setPositiveButton("YES") { _, _ -> sec.me.action() }
            .setNegativeButton("NO", null)
            .show()!!

    fun warnDialog(title: String, msg: String) =
        AlertDialog.Builder(sec.me)
            .setCancelable(false)
            .setTitle(title)
            .setMessage(msg)
            .setPositiveButton("OK", null)
            .show()!!

    fun permissionDialog(action: MainActivity.() -> Unit) =
        AlertDialog.Builder(sec.me)
            .setCancelable(false)
            .setTitle("沒有權限 :(")
            .setPositiveButton("給你") { _, _ -> sec.me.action() }
            .show()!!

    fun adbDialog(posBtn: String, action: MainActivity.() -> Unit) {
        AlertDialog.Builder(sec.me)
            .setCancelable(false)
            .setTitle("沒有開啟adb :(")
            .setPositiveButton(posBtn) { _, _ -> sec.me.action() }
            .show()!!
    }
}