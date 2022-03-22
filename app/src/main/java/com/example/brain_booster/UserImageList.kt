package com.example.brain_booster

import com.google.firebase.firestore.PropertyName

data class UserImageList(
   @PropertyName("images") val images:List<String>?=null
)