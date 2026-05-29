package com.android.stepsync.utils

import android.app.Activity
import android.content.Context
import android.widget.EditText
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder

fun Activity.toast(msg: String) {
    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}

fun EditText.isNotValid(): Boolean {
    return text?.toString().isNullOrBlank()
}

fun Context.showAlertDialog(
    title: String,
    message: String,
    positiveText: String,
    negativeText: String,
    neutralText: String? = null,
    onPositiveClick: (() -> Unit)? = null,
    onNegativeClick: (() -> Unit)? = null,
) {
    val builder = MaterialAlertDialogBuilder(this)
        .setTitle(title)
        .setMessage(message)
        .setPositiveButton(positiveText) { dialog, _ ->
            onPositiveClick?.invoke()
            dialog.dismiss()
        }
        .setNegativeButton(negativeText) { dialog, _ ->
            onNegativeClick?.invoke()
            dialog.dismiss()
        }

    neutralText?.let {
        builder.setNeutralButton(it) { dialog, _ ->
            dialog.dismiss()
        }
    }

    builder.show()
}

fun Context.showAlertDialog(
    title: String,
    message: String,
    positiveText: String,
    neutralText: String? = null,
    onPositiveClick: (() -> Unit)? = null,
) {
    val builder = MaterialAlertDialogBuilder(this)
        .setTitle(title)
        .setMessage(message)
        .setPositiveButton(positiveText) { dialog, _ ->
            onPositiveClick?.invoke()
            dialog.dismiss()
        }

    neutralText?.let {
        builder.setNeutralButton(it) { dialog, _ ->
            dialog.dismiss()
        }
    }

    builder.show()
}
