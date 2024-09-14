package top.laoxin.modmanager.ui.view.modview

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.sharp.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ModPartialBottomSheet() {
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false,
    )
    var searchText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(
            onClick = { showBottomSheet = true }
        ) {
            Text("Display partial bottom sheet")
        }

        if (showBottomSheet) {
            ModalBottomSheet(
                modifier = Modifier.fillMaxHeight(),
                sheetState = sheetState,
                onDismissRequest = { showBottomSheet = false }
            ) {
                Row {
                    // 图标按钮
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(
                            imageVector = Icons.Sharp.Add,
                            contentDescription = "Localized description",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(text = "添加")

                    }

                }


                CustomEdit(
                    text = searchText,
                    onValueChange = {
                        searchText = it
                    },
                    hint = "搜索应用",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 10.dp)
                        .height(50.dp)
                        .background(Color(0xBCE9E9E9), shape = MaterialTheme.shapes.medium)
                        .padding(horizontal = 16.dp),
                    textStyle = typography.bodyMedium,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    FilterChipExample()
                    FilterChipExample()
                    FilterChipExample()
                    FilterChipExample()
                    FilterChipExample()
                }
            }
        }
    }
}

@Composable
fun FilterChipExample() {
    var selected by remember { mutableStateOf(false) }

    FilterChip(
        onClick = { selected = !selected },
        label = {
            Text("Filter chip")
        },
        selected = selected,
        leadingIcon = if (selected) {
            {
                Icon(
                    imageVector = Icons.Filled.Done,
                    contentDescription = "Done icon",
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
            }
        } else {
            null
        },
        trailingIcon = {
            Icon(
                imageVector = Icons.Filled.Done,
                contentDescription = "Done icon",
                modifier = Modifier.size(FilterChipDefaults.IconSize)
            )
        }
    )
}

/**
 * @param hint: 空字符时的提示
 * @param startIcon: 左侧图标;  -1 则不显示
 * @param iconSpacing: 左侧图标与文字的距离; 相当于: drawablePadding
 */
@Composable
fun CustomEdit(
    text: String = "",
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    hint: String = "请输入",
    @DrawableRes startIcon: Int = -1,
    iconSpacing: Dp = 6.dp,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = TextStyle.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    cursorBrush: Brush = SolidColor(MaterialTheme.colorScheme.primary)
) {
    // 焦点, 用于控制是否显示 右侧叉号
    var hasFocus by remember { mutableStateOf(false) }

    BasicTextField(
        value = text,
        onValueChange = onValueChange,
        modifier = modifier.onFocusChanged { hasFocus = it.isFocused },
        singleLine = true,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        cursorBrush = cursorBrush,
        decorationBox = @Composable { innerTextField ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // -1 不显示 左侧Icon
                if (startIcon != -1) {
                    Image(painter = painterResource(id = startIcon), contentDescription = null)
                    Spacer(modifier = Modifier.width(iconSpacing))
                }

                Box(modifier = Modifier.weight(1f)) {
                    // 当空字符时, 显示hint
                    if (text.isEmpty())
                        Text(text = hint, color = Color.Gray, style = textStyle)

                    // 原本输入框的内容
                    innerTextField()
                }

                // 存在焦点 且 有输入内容时. 显示叉号
                if (hasFocus && text.isNotEmpty()) {
                    Icon(imageVector = Icons.Filled.Clear, // 清除图标
                        contentDescription = null,
                        // 点击就清空text
                        modifier = Modifier.clickable { onValueChange.invoke("") })
                }
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = 10.dp)
                )
            }
        }
    )
}

// 预览
@Preview("标签筛选")
@Preview(showBackground = true)
@Composable
fun PreviewDisableModPage() {
    ModPartialBottomSheet()
}