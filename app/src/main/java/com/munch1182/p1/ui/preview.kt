package com.munch1182.p1.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.munch1182.p1.domain.ThemeRepoImpl
import com.munch1182.p1.domain.ThemeVM
import com.munch1182.p1.ui.theme.Dimens
import com.munch1182.p1.ui.theme.P1Theme
import com.munch1182.p1.ui.theme.paddingPage

/**
 * 提供一个Column的预览
 */
@Composable
fun PreviewContainer(
    modifier: Modifier = Modifier,
    vm: ThemeVM = ThemeVM(ThemeRepoImpl()),
    content: @Composable () -> Unit
) {
    P1Theme(vm) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = modifier
        ) {
            Column(
                modifier = Modifier.paddingPage(),
                verticalArrangement = Arrangement.spacedBy(Dimens.PaddingItem)
            ) {
                content()
            }
        }
    }
}
