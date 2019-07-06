package nl.dorost.charades

import nl.dorost.charades.domain.Game
import nl.dorost.charades.domain.Player
import kotlin.random.Random
import kotlin.random.nextUInt
import kotlin.random.nextULong

interface Repository {
    fun readUser(userName: String): Player?
    fun createUser(player: Player): UInt?
    fun readAllUsers(): List<Player>
    fun readAllGames(): List<Game>
    fun saveGame(game: Game)
    fun deleteUser(username: String)
    fun loadGame(gameId: UInt): Game?
    fun readGameForUser(username: String): Game?
}


class MemoryStorage : Repository {

    private val users: MutableList<Player> = mutableListOf(
            Player(
                    name = "Amin Dorostanian",
                    password = hash("thepass"),
                    id = Random.nextUInt(),
                    username = "amind"
            )
    )
    private val games: MutableList<Game> = mutableListOf()

    override fun readUser(userName: String): Player? {
        return users.firstOrNull { it.username == userName }
    }

    override fun createUser(player: Player): UInt? {
        if (users.none { it.username == player.username }) {
            val createdPlayer = player.copy(id = Random.nextUInt())
            users.add(createdPlayer)
            return createdPlayer.id
        }
        return null
    }

    override fun saveGame(game: Game){
        val existingGame = games.firstOrNull { it.id == game.id }?.let {
            return
        }
        games.add(game)

    }

    override fun loadGame(gameId: UInt) = games.firstOrNull { it.id == gameId }

    override fun deleteUser(username: String) {
        users.removeIf { it.username == username }
    }

    override fun readAllUsers(): List<Player> = users

    override fun readAllGames(): List<Game> = games

    override fun readGameForUser(username: String) = games.firstOrNull { game ->
        game.teams.flatMap { it.players }.map { it.username }.contains(username)
    }

}


