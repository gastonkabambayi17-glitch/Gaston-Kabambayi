package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.example.R

@Composable
fun getDrawablePainter(resName: String?): Painter {
    return when (resName) {
        "img_profile_sophie" -> painterResource(id = R.drawable.img_profile_sophie)
        "img_profile_lucas" -> painterResource(id = R.drawable.img_profile_lucas)
        "img_landing_couple" -> painterResource(id = R.drawable.img_landing_couple)
        "img_gaston_love_icon" -> painterResource(id = R.drawable.img_gaston_love_icon)
        else -> painterResource(id = R.drawable.img_profile_sophie)
    }
}
