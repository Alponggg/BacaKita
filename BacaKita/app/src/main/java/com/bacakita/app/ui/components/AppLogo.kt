package com.bacakita.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bacakita.app.R

/** Logo aplikasi. Ganti file res/drawable/app_logo.* untuk memasang logo sendiri. */
@Composable
fun AppLogo(modifier: Modifier = Modifier, size: Dp = 96.dp) {
    Image(
        painter = painterResource(R.drawable.app_logo),
        contentDescription = "Logo BacaKita",
        modifier = modifier.size(size),
    )
}
