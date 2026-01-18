package top.laoxin.modmanager.ui.view.components.setting

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import top.laoxin.modmanager.R
import top.laoxin.modmanager.ui.view.components.common.openUrl
import top.laoxin.modmanager.ui.viewmodel.SettingViewModel

@Composable
fun License(modifier: Modifier, viewModel: SettingViewModel) {
    val context = LocalContext.current

    val libraries by produceLibraries(R.raw.aboutlibraries)

    LibrariesContainer(
        modifier = modifier,
        libraries = libraries,
        onLibraryClick = { library ->
            library.website?.let { website ->
                context.openUrl(website)
            }
        },
    )

    BackHandler {
        viewModel.setAboutPage(false)
    }
}
