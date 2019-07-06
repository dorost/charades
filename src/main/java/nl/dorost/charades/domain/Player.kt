package nl.dorost.charades.domain

data class Player(
        val name: String,
        val id: UInt? = null,
        val username: String,
        var password: String? = null,
        val words: MutableList<Word> = mutableListOf(),
        var charadesWords: MutableList<Word> = mutableListOf(),
        var points: Int = 0,
        var playingsCount: Int = 0,
        var votes: MutableList<String> = mutableListOf(),
        var isCharader: Boolean = false
) {
    fun toPlayerResponse(teamName: String? = null) = PlayerResponse(
            name = this.name,
            points = this.points,
            wordsCount = this.words.size,
            playingsCount = this.playingsCount,
            username = this.username,
            teamName = teamName
    )
}