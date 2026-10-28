// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.powerstudy

import com.github.ivanshafran.sharedpreferencesmock.SPMockBuilder
import org.junit.Test
import kotlin.test.assertEquals

class PowerSessionStoreTest {
    @Test
    fun `session survives store recreation`() {
        val sharedPrefs = SPMockBuilder().createSharedPreferences()
        val original =
            PowerSession(
                id = "english",
                name = "English vocabulary",
                cards =
                    listOf(
                        PowerCardState(1L, 2),
                        PowerCardState(2L, 4),
                        PowerCardState(3L, POWER_STUDY_INACTIVE_BOX),
                    ),
                activationLimit = 2,
                boxWeights = DEFAULT_POWER_STUDY_BOX_WEIGHTS,
                currentCardId = 2L,
            )

        PowerSessionStore(sharedPrefs).save(original)

        val restored = PowerSessionStore(sharedPrefs).get(original.id)

        assertEquals(original, restored)
    }

    @Test
    fun `multiple sessions keep independent progress`() {
        val sharedPrefs = SPMockBuilder().createSharedPreferences()
        val store = PowerSessionStore(sharedPrefs)
        val english =
            PowerSession(
                id = "english",
                name = "English",
                cards = listOf(PowerCardState(42L, 2)),
                activationLimit = 30,
                boxWeights = DEFAULT_POWER_STUDY_BOX_WEIGHTS,
                currentCardId = 42L,
            )
        val french =
            PowerSession(
                id = "french",
                name = "French",
                cards = listOf(PowerCardState(42L, 5)),
                activationLimit = 30,
                boxWeights = DEFAULT_POWER_STUDY_BOX_WEIGHTS,
                currentCardId = 42L,
            )

        store.save(english)
        store.save(french)

        assertEquals(listOf(english, french), PowerSessionStore(sharedPrefs).sessions())
    }
}
