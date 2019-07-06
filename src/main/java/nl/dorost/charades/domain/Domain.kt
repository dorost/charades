package nl.dorost.charades.domain


data class ResponseModel(
        val message: String? = null,
        val gameName: String? = null,
        val players: List<PlayerResponse>? = null,
        val currentPlayer: PlayerResponse? = null,
        val gameStatus: String? = null,
        val status: String? = null,
        val charadingStatus: String? = null,
        val currentWord: Word? = null,
        val charadedWords: List<Word>?=null,
        val teams: List<TeamResponse>?=null,
        val timeSecondsRemaining: Int? = null,
        val charader: PlayerResponse? = null,
        val wordsCountLimit: Int? = null,
        val charadingStarted: Boolean? = null,
        val winnerTeam: String? = null

)

data class PlayerResponse(
        val name: String,
        val username: String,
        val points: Int,
        val playingsCount: Int,
        val wordsCount: Int,
        val teamName: String?
)

data class TeamResponse(
        val id: UInt,
        val teamName: String,
        val points: Int
)


enum class Vote {
    ACCEPTED,
    REJECTED
}

