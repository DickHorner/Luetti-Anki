// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.powerstudy

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.os.BundleCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.browser.IdsFile
import com.ichi2.utils.input
import com.ichi2.utils.negativeButton
import com.ichi2.utils.positiveButton
import com.ichi2.utils.show
import com.ichi2.utils.title

class CreatePowerSessionDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val idsFile =
            requireNotNull(
                BundleCompat.getParcelable(requireArguments(), ARG_CARD_IDS_FILE, IdsFile::class.java),
            )
        return AlertDialog
            .Builder(requireContext())
            .show {
                title(R.string.power_study_new_session)
                positiveButton(CommonString.dialog_ok)
                negativeButton(CommonString.dialog_cancel)
                setView(R.layout.dialog_generic_text_input)
            }.input(
                hint = getString(R.string.power_study_session_name),
                allowEmpty = false,
                displayKeyboard = true,
                waitForPositiveButton = true,
            ) { dialog, name ->
                setFragmentResult(
                    REQUEST_CREATE_SESSION,
                    Bundle().apply {
                        putParcelable(ARG_CARD_IDS_FILE, idsFile)
                        putString(ARG_SESSION_NAME, name.toString())
                    },
                )
                dialog.dismiss()
            }
    }

    companion object {
        const val TAG = "createPowerSession"
        const val REQUEST_CREATE_SESSION = "requestCreatePowerSession"
        const val ARG_CARD_IDS_FILE = "powerStudyCardIdsFile"
        const val ARG_SESSION_NAME = "powerStudySessionName"

        fun newInstance(idsFile: IdsFile): CreatePowerSessionDialogFragment =
            CreatePowerSessionDialogFragment().apply {
                arguments =
                    Bundle().apply {
                        putParcelable(ARG_CARD_IDS_FILE, idsFile)
                    }
            }
    }
}
