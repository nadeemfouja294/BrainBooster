package com.example.brain_booster

import android.content.Context
import android.widget.Toast

class Extensions {

    fun Context.showToast(message:String, length:Int= Toast.LENGTH_LONG){
        Toast.makeText(this,message,length).show()
    }
}