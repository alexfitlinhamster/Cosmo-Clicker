package com.example.myapplication.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.myapplication.R
import com.example.myapplication.Rarity

@Composable
fun rarityLabel(rarity: Rarity): String = stringResource(
    when (rarity) {
        Rarity.COMMON -> R.string.rarity_common
        Rarity.UNCOMMON -> R.string.rarity_uncommon
        Rarity.RARE -> R.string.rarity_rare
        Rarity.EPIC -> R.string.rarity_epic
        Rarity.LEGENDARY -> R.string.rarity_legendary
    }
)
