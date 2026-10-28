// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.powerstudy

import androidx.lifecycle.SavedStateHandle
import com.ichi2.anki.CollectionManager.withCol
import com.ichi2.anki.asyncIO
import com.ichi2.anki.launchCatchingIO
import com.ichi2.anki.libanki.Card
import com.ichi2.anki.pages.AnkiServer
import com.ichi2.anki.previewer.CardViewerViewModel
import com.ichi2.anki.previewer.TypeAnswer
import com.ichi2.anki.reviewer.CardSide
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PowerStudyViewModel(
    savedStateHandle: SavedStateHandle,
    initialSession: PowerSession,
    private val store: PowerSessionStore,
) : CardViewerViewModel(savedStateHandle) {
    private val engine = PowerStudyEngine()
    private val answerMutex = Mutex()

    private val resumedSession =
        if (initialSession.currentCardId == null) {
            initialSession
                .copy(currentCardId = checkNotNull(engine.nextCard(initialSession)))
                .also(store::save)
        } else {
            initialSession
        }

    val session = MutableStateFlow(resumedSession)
    val isComplete = MutableStateFlow(false)

    override var currentCard: Deferred<Card> =
        asyncIO {
            withCol { getCard(checkNotNull(session.value.currentCardId)) }
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
            answerMutex.withLock {
                if (isComplete.value) return@withLock

                val cardId = currentCard.await().id
                val answeredSession =
                    if (correct) {
                        engine.answerCorrect(session.value, cardId)
                    } else {
                        engine.answerWrong(session.value, cardId)
                    }

                val nextCardId = engine.nextCard(answeredSession)
                val updatedSession = answeredSession.copy(currentCardId = nextCardId)
                store.save(updatedSession)
                session.value = updatedSession

                if (nextCardId == null) {
                    isComplete.value = true
                    return@withLock
                }

                currentCard =
                    asyncIO {
                        withCol { getCard(nextCardId) }
                    }
                showQuestion()
                loadAndPlayMedia(CardSide.QUESTION)
            }
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
