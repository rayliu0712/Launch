package rl.launch

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import java.io.File
import java.security.MessageDigest
import rl.launch.MainActivity.Companion.me

object Ez {
    fun isNotASCII(pattern: String): Boolean =
        !pattern.matches("\\A\\p{ASCII}*\\z".toRegex())

    fun sha256(msg: String): String {
        val bytes = msg.toByteArray()
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    fun relPath(file: File): String =
        "~/" + file.absolutePath.replace("^${Environment.getExternalStorageDirectory().absolutePath}".toRegex(), "").trimStart('/')

    fun specialLen(file: File): Long =
        if (file.isDirectory) -1 else file.length()

    fun totalSize(): String {
        val units = arrayOf("Byte", "KB", "MB", "GB", "TB")
        var size = pending.sumOf { dir ->
            dir.walkTopDown().sumOf { it.length() }
        }.toDouble()

        var i = 0
        while (size >= 1024 && i < 5) {
            size /= 1024
            i++
        }

        return if (i == 0)
            "%.0f ${units[i]}${if (size <= 1.0) "" else "s"}".format(size)
        else
            "%.2f ${units[i]}".format(size)
    }

    fun url(url: String) {
        me.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    fun dialog(cancelable: Boolean = false): AlertDialog.Builder =
        AlertDialog.Builder(me)
            .setCancelable(cancelable)

    fun warnDialog(title: String, msg: String): AlertDialog =
        dialog()
            .setTitle(title)
            .setMessage(msg)
            .setPositiveButton("OK", null)
            .show()!!

    fun permissionDialog(action: MainActivity.() -> Unit): AlertDialog =
        dialog()
            .setTitle("沒有權限 :(")
            .setPositiveButton("GRANT") { _, _ -> me.action() }
            .show()!!

    fun adbDialog(action: MainActivity.() -> Unit): AlertDialog =
        dialog()
            .setTitle("沒有啟用ADB :(")
            .setPositiveButton("SETTINGS") { _, _ -> me.action() }
            .show()!!

    fun log(vararg any: Any) =
        Log.d("666", any.joinToString(" "))
}