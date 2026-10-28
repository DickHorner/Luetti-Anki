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
import com.ichi2.anki.R
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

class PowerStudyFragment : CardViewerFragment(R.layout.fragment_power_study) {
    override val viewModel: PowerStudyViewModel by viewModels {
        viewModelFactory {
            initializer {
                val handle = createSavedStateHandle()
                val sessionId = handle.require<String>(SESSION_ID_ARG)
                val store = PowerSessionStore(requireContext())
                val session = store.get(sessionId) ?: throw UnavailableSessionException()
                if (session.isComplete) throw UnavailableSessionException()
                PowerStudyViewModel(handle, session, store)
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
        } catch (e: UnavailableSessionException) {
            Timber.w(e, "Failed to load power study session")
            showThemedToast(requireContext(), R.string.power_study_session_unavailable, false)
            view.isVisible = false
            requireActivity().finish()
            return
        }

        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.title = viewModel.session.value.name
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

    private class UnavailableSessionException : Exception("Unable to load power study session")

    companion object {
        const val SESSION_ID_ARG = "powerStudySessionId"

        fun getIntent(
            context: Context,
            sessionId: String,
        ): Intent {
            val arguments =
                Bundle().apply {
                    putString(SESSION_ID_ARG, sessionId)
                }
            return CardViewerActivity.getIntent(context, PowerStudyFragment::class, arguments)
        }
    }
}
