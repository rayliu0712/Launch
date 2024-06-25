package rl.launch

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import java.io.File

object Ez {
    fun relPath(file: File): String =
        "./" + file.absolutePath.replace(
            "^${Environment.getExternalStorageDirectory().absolutePath}".toRegex(), ""
        ).replace("/", "")

    fun specialLen(file: File): Long =
        if (file.isDirectory) -1 else file.length()

    fun length(dir: File): Long =
        dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }

    fun sizeTv(): String {
        val units = arrayOf("Byte", "KB", "MB", "GB", "TB")
        var size = pending.sumOf { length(it) }.toDouble()

        var i = 0
        while (size >= 1024 && i < 5) {
            size /= 1024
            i++
        }

        return if (i == 0)
            "%.0f ${units[i]}${if (size <= 1.0) "" else "s"}".format(size)
        else if (size == size.toInt().toDouble())
            "%.0f ${units[i]}".format(size)
        else
            "%.2f ${units[i]}".format(size)
    }

    fun url(me: MainActivity, url: String) =
        me.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))

    fun dialog(me: MainActivity, cancelable: Boolean = false): AlertDialog.Builder =
        AlertDialog.Builder(me)
            .setCancelable(cancelable)

    fun warnDialog(me: MainActivity, title: String, msg: String): AlertDialog =
        dialog(me)
            .setTitle(title)
            .setMessage(msg)
            .setPositiveButton("OK", null)
            .show()!!

    fun permissionDialog(me: MainActivity, action: MainActivity.() -> Unit): AlertDialog =
        dialog(me)
            .setTitle("沒有權限 :(")
            .setPositiveButton("GRANT") { _, _ -> me.action() }
            .show()!!

    fun adbDialog(me: MainActivity, action: MainActivity.() -> Unit): AlertDialog =
        dialog(me)
            .setTitle("沒有啟用ADB :(")
            .setPositiveButton("SETTINGS") { _, _ -> me.action() }
            .show()!!

    fun toast(me: MainActivity, vararg msg: Any) =
        Toast.makeText(me, msg.joinToString { "$it" }, Toast.LENGTH_SHORT).show()

    fun log(vararg msg: Any) =
        Log.d("666", msg.joinToString { "$it" })
}