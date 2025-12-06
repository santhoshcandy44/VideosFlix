package com.flix.videos.ui.utils

import android.content.Context
import android.widget.Toast

fun shortToast(context: Context, message:String){
    Toast.makeText(context, message, Toast.LENGTH_SHORT)
        .show()
}