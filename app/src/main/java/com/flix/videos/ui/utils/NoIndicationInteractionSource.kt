package com.flix.videos.ui.utils

import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import kotlinx.coroutines.flow.MutableSharedFlow

class NoIndicationInteractionSource : MutableInteractionSource {
    override val interactions = MutableSharedFlow<Interaction>()

    override suspend fun emit(interaction: Interaction) {

    }
    override fun tryEmit(interaction: Interaction): Boolean {
        return false
    }
}
