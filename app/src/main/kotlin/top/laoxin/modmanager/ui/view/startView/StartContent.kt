package top.laoxin.modmanager.ui.view.startView

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import top.laoxin.modmanager.R
import kotlin.random.Random

@SuppressLint("NewApi")
@Composable
fun StartContent() {
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    val imageSize = if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
        screenWidth * 0.8f
    } else {
        screenHeight * 0.6f
    }

    val imageResIds = getDrawableResourcesByPattern(context, "start_")

    val randomImageResId = if (imageResIds.isNotEmpty()) {
        imageResIds[Random.nextInt(imageResIds.size)]
    } else {
        R.drawable.start_2 //
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))
        // Add splash screen background
        Image(
            painter = painterResource(id = randomImageResId),
            contentDescription = null,
            modifier = Modifier
                .size(imageSize)
                .align(Alignment.CenterHorizontally),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.weight(1f))
        // Bottom app icon
        Image(
            painter = painterResource(id = R.drawable.app_icon),
            contentDescription = null,
            modifier = Modifier
                .size(50.dp)
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.height(screenHeight * 0.1f))
    }
}

@SuppressLint("DiscouragedApi")
fun getDrawableResourcesByPattern(context: Context, pattern: String): List<Int> {
    val resources = context.resources
    val packageName = context.packageName
    val drawableResIds = mutableListOf<Int>()

    val fields = R.drawable::class.java.fields
    for (field in fields) {
        if (field.name.startsWith(pattern)) {
            val resId = resources.getIdentifier(field.name, "drawable", packageName)
            if (resId != 0) {
                val drawable = ResourcesCompat.getDrawable(resources, resId, context.theme)
                if (drawable != null) {
                    drawableResIds.add(resId)
                }
            }
        }
    }
    return drawableResIds
}

@Composable
@Preview(showBackground = true)
fun PreviewStartContent() {
    StartContent()
}