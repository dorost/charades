package nl.dorost.charades

import kotlin.random.Random
import kotlin.random.nextULong

data class Player(
        val name: String,
        val id: ULong? = null,
        val username: String,
        var password: String? = null,
        val words: MutableList<Word> = mutableListOf(),
        val charadesWords: MutableList<Word> = mutableListOf(),
        var points: Int = 0,
        var playingsCount: Int = 0
) {
    fun toUserRespone(): PlayerResponse? {
        return PlayerResponse(
                name = this.name,
                id = this.id!!,
                points = this.points,
                username = this.username,
                wordsCount = this.words.size,
                playingsCount = this.playingsCount
        )
    }
}


data class Team(
        val name: String,
        val id: ULong? = null,
        var points: Int = 0,
        val players: MutableList<Player> = mutableListOf()
)

data class Word(
        val text: String,
        val id: ULong? = Random.nextULong()
)

data class Game(
        val id: UInt? = null,
        val name: String,
        val teamA: Team,
        val teamB: Team,
        var timeRemainingSeconds: Int = 0,
        var charadeDone: Boolean = false,
        var turn: Team? = if (Random.nextDouble()<0.5) teamA else teamB,
        val timeLimitSeconds: Int = 5,
        var wordsToGuessStack: MutableList<Word> = mutableListOf(),
        var charader: Player? = null,
        var playersToPlay: MutableList<Player> = mutableListOf(),
        val wordsCountLimit: Int = 1,
        var charadingStarted: Boolean = false,
        var status: GameStatus = GameStatus.STARTED,
        val lastPlayedTurns: MutableList<Team>? = null
) {
    fun toGameResponse(): GameResponse? {
        return GameResponse(
                name = this.name,
                id = this.id!!,
                charader = this.charader?.toUserRespone(),
                charadingStarted = this.charadingStarted,

                teamA = TeamResponse(
                        id= this.teamA.id!!,
                        name = this.teamA.name,
                        points = this.teamA.points
                ),
                teamB = TeamResponse(
                        id= this.teamB.id!!,
                        name = this.teamB.name,
                        points = this.teamB.points
                ),
                allPlayers = (this.teamA.players + this.teamB.players).map {
                    it.toUserRespone()!!.copy(teamName = getTeamForUser(it.username).name)
                },
                status = this.status.s,
                gameStatus = this.status.name,
                wordCountLimit = this.wordsCountLimit,
                timeSecondsRemaining = this.timeRemainingSeconds
        )
    }

    fun getTeamForUser(username: String): Team {
        return if (this.teamA.players.map { it.username }.contains(username))
            this.teamA
        else
            this.teamB
    }
}


enum class GameStatus(val s: String) {
    STARTED("Waiting for players to enter the words!"),
    SENTENCE("Players ready! Starting to charade! With only one sentence!"),
    ONE_WORD("Only one word to charade!"),
    PANTHOMIME("Just play it! Pantomime!"),
    FINISHED("Aaaand that's it!")

}

data class ResponseModel(
        val message: String? = null,
        val currentGame: GameResponse? = null,
        val games: List<GameResponse>? = null,
        val currentPlayer: PlayerResponse? = null,
        val currentWord: Word? = null
)
data class GameResponse(
        val name: String,
        val id: UInt,
        val teamA: TeamResponse,
        val teamB: TeamResponse,
        val charadingStarted: Boolean,
        val charader: PlayerResponse?,
        val allPlayers: List<PlayerResponse>,
        val status: String,
        val gameStatus: String,
        val wordCountLimit: Int,
        val timeSecondsRemaining: Int
)

data class TeamResponse(
        val name: String,
        val id: ULong,
        val points: Int
)
data class PlayerResponse(
        val name: String,
        val username: String,
        val id: ULong,
        val points: Int,
        val wordsCount: Int,
        val teamName: String? = null,
        val playingsCount: Int
)