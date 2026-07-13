package com.munch1182.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.munch1182.core.ui.dialog.DialogFactory
import com.munch1182.core.ui.theme.Dimens
import com.munch1182.core.ui.theme.paddingItem


fun showSelectDialog(options: List<String>, onSelect: (Int) -> Unit) {
    DialogFactory.newBottom(Unit) { control ->
        Column(Modifier.padding(vertical = Dimens.PaddingPage)) {
            options.forEachIndexed { idx, str ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable {
                            onSelect(idx)
                            control.dismiss()
                        }
                        .paddingItem(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(str, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }.show()
}