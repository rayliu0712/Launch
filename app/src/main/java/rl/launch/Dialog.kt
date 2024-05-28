package rl.launch

import android.app.AlertDialog
import android.content.DialogInterface.OnClickListener

class Dialog private constructor(
    private val title: String,
    private val msg: String,
    private val cancelable: Boolean,
    private val posBtn: String,
    var posLis: OnClickListener? = null,
    var negBtn: String? = null,
    var negLis: OnClickListener? = null) {

    fun show(posLis: OnClickListener? = this.posLis, negLis: OnClickListener? = this.negLis) =
        AlertDialog.Builder(Ez.ma)
            .setTitle(title)
            .setMessage(msg)
            .setCancelable(cancelable)
            .setPositiveButton(posBtn, posLis)
            .setNegativeButton(negBtn, negLis)
            .show()!!

    companion object {
        fun abBuilder(
            title: String, msg: String, posBtn: String, negBtn: String? = null,
            posLis: OnClickListener? = null, negLis: OnClickListener? = null): Dialog =
            Dialog(title, msg, true, posBtn, posLis, negBtn, negLis)

        fun posBuilder(title: String, msg: String, posBtn: String, posLis: OnClickListener? = null): Dialog =
            Dialog(title, msg, false, posBtn, posLis)

        fun yesNo(title: String, msg: String, posLis: OnClickListener? = null) =
            Dialog(title, msg, false, "YES", posLis, "NO").show()

        fun warn(title: String, msg: String) =
            Dialog(title, msg, false, "OK").show()
    }
}