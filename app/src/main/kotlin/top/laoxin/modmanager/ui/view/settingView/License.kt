package top.laoxin.modmanager.ui.view.settingView

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.mikepenz.aboutlibraries.ui.compose.android.rememberLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import top.laoxin.modmanager.R
import top.laoxin.modmanager.ui.viewmodel.SettingViewModel

@Composable
fun License(modifier: Modifier, viewModel: SettingViewModel) {
    val context = LocalContext.current

    val libraries = rememberLibraries(
        resId = R.raw.aboutlibraries
    )

    LibrariesContainer(
        modifier = modifier,
        libraries = libraries.value,
        onLibraryClick = { library ->
            library.website?.let { website ->
                viewModel.openUrl(context, website)
            }
        },
    )

    BackHandler {
        viewModel.setAboutPage(false)
    }
}
