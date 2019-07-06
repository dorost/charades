package nl.dorost.charades.domain


data class Team(
        val name: String,
        val id: UInt,
        var points: Int = 0,
        val players: MutableList<Player> = mutableListOf()
)
