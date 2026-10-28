// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.powerstudy

import androidx.lifecycle.SavedStateHandle
import com.ichi2.anki.CollectionManager.withCol
import com.ichi2.anki.asyncIO
import com.ichi2.anki.libanki.Card
import com.ichi2.anki.pages.AnkiServer
import com.ichi2.anki.previewer.CardViewerViewModel
import com.ichi2.anki.previewer.TypeAnswer
import com.ichi2.anki.reviewer.CardSide
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.MutableStateFlow

class PowerStudyViewModel(
    savedStateHandle: SavedStateHandle,
    cardIds: List<Long>,
) : CardViewerViewModel(savedStateHandle) {
    private val engine = PowerStudyEngine()
    val session =
        MutableStateFlow(
            engine.createSession(
                id = "temporary",
                name = "Power study",
                cardIds = cardIds,
                boxWeights = DEFAULT_POWER_STUDY_BOX_WEIGHTS,
            ),
        )
    val isComplete = MutableStateFlow(false)

    private fun nextCardId(): Long =
        checkNotNull(engine.nextCard(session.value)) {
            "Power study session has no active card"
        }

    override var currentCard: Deferred<Card> =
        asyncIO {
            withCol { getCard(nextCardId()) }
        }

    override val server = AnkiServer(this).also { it.start() }

    override fun onPageFinished(isAfterRecreation: Boolean) {
        launchCatchingIO {
            if (showingAnswer.value) {
                showAnswer()
                loadAndPlayMedia(CardSide.ANSWER)
            } else {
                showQuestion()
                loadAndPlayMedia(CardSide.QUESTION)
            }
        }
    }

    fun showCurrentAnswer() {
        launchCatchingIO {
            if (isComplete.value) return@launchCatchingIO
            showAnswer()
            loadAndPlayMedia(CardSide.ANSWER)
        }
    }

    fun answerCorrect() = answer(correct = true)

    fun answerWrong() = answer(correct = false)

    private fun answer(correct: Boolean) {
        launchCatchingIO {
            if (isComplete.value) return@launchCatchingIO

            val cardId = currentCard.await().id
            session.value =
                if (correct) {
                    engine.answerCorrect(session.value, cardId)
                } else {
                    engine.answerWrong(session.value, cardId)
                }

            val nextCardId = engine.nextCard(session.value)
            if (nextCardId == null) {
                isComplete.emit(true)
                return@launchCatchingIO
            }

            currentCard =
                asyncIO {
                    withCol { getCard(nextCardId) }
                }
            showQuestion()
            loadAndPlayMedia(CardSide.QUESTION)
        }
    }

    private suspend fun loadAndPlayMedia(side: CardSide) {
        cardMediaPlayer.loadCardAvTags(currentCard.await())
        cardMediaPlayer.autoplayAllForSide(side)
    }

    override suspend fun typeAnsFilter(text: String): String =
        if (showingAnswer.value) {
            val typeAnswer = TypeAnswer.getInstance(currentCard.await(), text)
            typeAnswer?.answerFilter() ?: text
        } else {
            TypeAnswer.removeTags(text)
        }
}
