package rl.launch

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface

class Dialog(
    private val context: Context,
    private val cancelable: Boolean = false,
    private val title: String? = null,
    private val msg: String? = null,
    private val positiveBtn: String? = null,
    var negativeBtn: String? = null,
) {
    fun build(
        positiveListener: DialogInterface.OnClickListener? = null,
        negativeListener: DialogInterface.OnClickListener? = null
    ) {
        AlertDialog.Builder(context)
            .setCancelable(cancelable)
            .setTitle(title)
            .setMessage(msg)
            .setPositiveButton(positiveBtn, positiveListener)
            .setNegativeButton(negativeBtn, negativeListener)
            .show()
    }

    companion object {
        fun warning(context: Context, title: String, msg: String) =
            Dialog(context, false, title, msg, "OK").build()
    }
}