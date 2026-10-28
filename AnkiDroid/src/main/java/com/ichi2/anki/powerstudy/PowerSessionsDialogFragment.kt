// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.powerstudy

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.utils.message
import com.ichi2.utils.negativeButton
import com.ichi2.utils.title

class PowerSessionsDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val sessions =
            PowerSessionStore(requireContext())
                .sessions()
                .filterNot { it.isComplete }

        val builder =
            AlertDialog
                .Builder(requireContext())
                .title(R.string.power_study_sessions)
                .negativeButton(CommonString.dialog_cancel)

        if (sessions.isEmpty()) {
            return builder
                .message(R.string.power_study_no_sessions)
                .create()
        }

        val labels =
            sessions
                .map { session ->
                    getString(R.string.power_study_session_list_item, session.name, session.cards.size)
                }.toTypedArray()

        return builder
            .setItems(labels) { _, index ->
                setFragmentResult(
                    REQUEST_OPEN_SESSION,
                    Bundle().apply {
                        putString(ARG_SESSION_ID, sessions[index].id)
                    },
                )
                dismiss()
            }.create()
    }

    companion object {
        const val TAG = "powerSessions"
        const val REQUEST_OPEN_SESSION = "requestOpenPowerSession"
        const val ARG_SESSION_ID = "powerStudySessionId"
    }
}
