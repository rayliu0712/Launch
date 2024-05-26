package rl.launch

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface.OnClickListener
import android.util.Log
import android.widget.Toast
import java.io.File

object Ez {
    data class Dialog(
        val context: Context,
        val cancelable: Boolean = false,
        val title: String? = null,
        val msg: String? = null,
        val positiveBtn: String? = null,
        var negativeBtn: String? = null,
    )

    fun buildDialog(dialog: Dialog,
                    positiveListener: OnClickListener? = null,
                    negativeListener: OnClickListener? = null
    ) {
        AlertDialog.Builder(dialog.context)
            .setCancelable(dialog.cancelable)
            .setTitle(dialog.title)
            .setMessage(dialog.msg)
            .setPositiveButton(dialog.positiveBtn, positiveListener)
            .setNegativeButton(dialog.negativeBtn, negativeListener)
            .show()
    }

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

    fun totalLength(root: File): Long {
        return root.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
    }

    fun size(length: Long): String {
        val units = arrayOf("B", "KiB", "MiB", "GiB", "TiB")
        var size = length.toDouble()
        var i = 0
        while (size >= 1024 && i < 5) {
            size /= 1024
            i++
        }
        return "%.2f ${units[i]}".format(size)
    }

}