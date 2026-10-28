// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.powerstudy

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.browser.IdsFile
import com.ichi2.anki.common.utils.android.showThemedToast
import com.ichi2.anki.databinding.FragmentPowerStudyBinding
import com.ichi2.anki.previewer.CardViewerActivity
import com.ichi2.anki.previewer.CardViewerFragment
import com.ichi2.anki.utils.ext.require
import com.ichi2.anki.workarounds.SafeWebViewLayout
import dev.androidbroadcast.vbpd.viewBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException

class PowerStudyFragment : CardViewerFragment(R.layout.fragment_power_study) {
    override val viewModel: PowerStudyViewModel by viewModels {
        viewModelFactory {
            initializer {
                val handle = createSavedStateHandle()
                val ids =
                    try {
                        handle.require<IdsFile>(CARD_IDS_FILE_ARG).getIds()
                    } catch (e: IOException) {
                        throw UnavailableSelectionException(e)
                    }
                if (ids.isEmpty()) throw UnavailableSelectionException()
                PowerStudyViewModel(handle, ids)
            }
        }
    }

    private val binding by viewBinding(FragmentPowerStudyBinding::bind)

    override val webViewLayout: SafeWebViewLayout get() = binding.webViewLayout

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        try {
            viewModel
        } catch (e: UnavailableSelectionException) {
            Timber.w(e, "Failed to read power study IDs")
            showThemedToast(requireContext(), CommonString.something_wrong, false)
            view.isVisible = false
            requireActivity().finish()
            return
        }

        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.showAnswerButton.setOnClickListener { viewModel.showCurrentAnswer() }
        binding.wrongButton.setOnClickListener { viewModel.answerWrong() }
        binding.correctButton.setOnClickListener { viewModel.answerCorrect() }

        lifecycleScope.launch {
            viewModel.showingAnswer
                .flowWithLifecycle(lifecycle)
                .collectLatest { showingAnswer ->
                    binding.showAnswerButton.isVisible = !showingAnswer
                    binding.wrongButton.isVisible = showingAnswer
                    binding.correctButton.isVisible = showingAnswer
                }
        }

        lifecycleScope.launch {
            viewModel.session
                .flowWithLifecycle(lifecycle)
                .collectLatest { session ->
                    val counts = (0..POWER_STUDY_BOX_COUNT).map { box -> session.cards.count { it.box == box } }
                    binding.progressIndicator.text =
                        getString(
                            R.string.power_study_progress,
                            counts[0],
                            counts[1],
                            counts[2],
                            counts[3],
                            counts[4],
                            counts[5],
                            counts[6],
                        )
                }
        }

        lifecycleScope.launch {
            viewModel.isComplete
                .flowWithLifecycle(lifecycle)
                .collectLatest { complete ->
                    if (!complete) return@collectLatest
                    showThemedToast(requireContext(), R.string.power_study_complete, true)
                    requireActivity().finish()
                }
        }
    }

    private class UnavailableSelectionException(
        cause: IOException? = null,
    ) : Exception("Unable to load power study selection", cause)

    companion object {
        const val CARD_IDS_FILE_ARG = "powerStudyCardIdsFile"

        fun getIntent(
            context: Context,
            idsFile: IdsFile,
        ): Intent {
            val arguments =
                Bundle().apply {
                    putParcelable(CARD_IDS_FILE_ARG, idsFile)
                }
            return CardViewerActivity.getIntent(context, PowerStudyFragment::class, arguments)
        }
    }
}
