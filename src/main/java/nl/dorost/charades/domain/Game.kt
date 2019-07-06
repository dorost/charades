package nl.dorost.charades.domain

import nl.dorost.charades.*
import kotlin.random.Random


data class Game(
        val id: UInt,
        val name: String,
        val teams: MutableList<Team> = mutableListOf(),
        var status: GameStatus = GameStatus.STARTED,
        var charadingStatus: CharadingStatus = CharadingStatus.NOT_STARTED,
        var teamIdTurn: UInt? = null,
        val timeLimitSeconds: Int,
        val wordsCountLimit: Int
)


enum class GameStatus(val s: String) {
    STARTED("Waiting for players to enter the words!"),
    SENTENCE("Players ready! Starting to charade! With only one sentence!"),
    ONE_WORD("Only one word to charade!"),
    PANTHOMIME("Just play it! Pantomime!"),
    FINISHED("Aaaand that's it!")
}

enum class CharadingStatus {
    NOT_STARTED,
    STARTED,
    VOTING,
    FINISHED
}