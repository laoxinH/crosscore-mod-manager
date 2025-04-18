package top.laoxin.modmanager.ui.view.settingView

import android.content.res.Configuration
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import top.laoxin.modmanager.R
import top.laoxin.modmanager.ui.viewmodel.SettingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingTopBar(
    viewModel: SettingViewModel,
    modifier: Modifier = Modifier,
    configuration: Int
) {
    val context = LocalContext.current
    val showAbout = viewModel.getAboutPage()
    TopAppBar(
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor =
                if (configuration == Configuration.ORIENTATION_LANDSCAPE) MaterialTheme.colorScheme.surface
                else MaterialTheme.colorScheme.surfaceContainer,
        ),
        navigationIcon = {
            if (showAbout) {
                IconButton(
                    onClick = {
                        viewModel.setAboutPage(false)
                    },
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        Modifier.size(28.dp)
                    )
                }
            } else null
        },
        title = {
            if (configuration != Configuration.ORIENTATION_LANDSCAPE) {
                Text(
                    stringResource(id = R.string.settings),
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        actions = {
            IconButton(
                onClick = {
                    viewModel.setAboutPage(!viewModel.uiState.value.showAbout)
                },
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    Modifier.size(28.dp)
                )
            }
            IconButton(
                onClick = {
                    viewModel.openUrl(context, context.getString(R.string.github_url))
                },
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.github_icon),
                    contentDescription = null,
                    Modifier.size(28.dp)
                )
            }
            IconButton(
                onClick = {
                    viewModel.checkUpdate()
                },
            ) {
                Icon(
                    imageVector = Icons.Filled.Update,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    )
}