package com.wangyiheng.vcamsx.components

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun SettingRow(
    label: String,
    checkedState: MutableState<Boolean>,
    onCheckedChange: (Boolean) -> Unit,
    context: Context
) {
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, modifier = Modifier.weight(1f))
        Switch(
            checked = checkedState.value,
            onCheckedChange = {
                checkedState.value = it
                onCheckedChange(it)
                Toast.makeText(context, if (it) "$label 打开" else "$label 关闭", Toast.LENGTH_SHORT).show()
            }
        )
    }
}