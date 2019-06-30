package nl.dorost.charades

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import kotlin.random.Random
import kotlin.random.nextUInt
import kotlin.random.nextULong

interface Storage {
    fun readUser(userName: String): Player?
    fun createUser(player: Player): ULong?
    fun readAllUsers(): List<Player>
    fun readAllGames(): List<Game>
    fun saveGame(game: Game): Game
    fun deleteUser(username: String)
    fun loadGame(gameId: UInt): Game?
    fun readGameForUser(username: String): Game?
    fun readUpdateForUser(username: String): ResponseModel
}


class MemoryStorage : Storage {

    private val users: MutableList<Player> = mutableListOf(
            Player(
                    name = "Amin Dorostanian",
                    password = hash("thepass"),
                    id = Random.nextULong(),
                    username = "amind"
            )
    )
    private val games: MutableList<Game> = mutableListOf()

    override fun readUser(userName: String): Player? {
        return users.firstOrNull { it.username == userName }
    }

    override fun createUser(player: Player): ULong? {
        if (users.none { it.username == player.username }) {
            val createdPlayer = player.copy(id = Random.nextULong())
            users.add(createdPlayer)
            return createdPlayer.id
        }
        return null
    }

    override fun saveGame(game: Game): Game {
        val existingGame = games.firstOrNull { it.id == game.id }
        existingGame?.let {
            games.remove(existingGame)
            games.add(game)
            return game
        } ?: run {
            val newGame = game.copy(
                    id = Random.nextUInt(),
                    teamA = game.teamA.copy(id = Random.nextULong()),
                    teamB = game.teamB.copy(id = Random.nextULong())
            )
            games.add(
                    newGame
            )
            return newGame
        }

    }

    override fun loadGame(gameId: UInt) = games.firstOrNull { it.id == gameId }

    override fun deleteUser(username: String) {
        users.removeIf { it.username == username }
    }

    override fun readAllUsers(): List<Player> = users

    override fun readAllGames(): List<Game> = games

    override fun readGameForUser(username: String) = games.firstOrNull { game ->
        (game.teamA.players + game.teamB.players).map { it.username }.contains(username)
    }

    override fun readUpdateForUser(username: String): ResponseModel {
        val game = readGameForUser(username)!!
        val user = readUser(username)
        val team = getTeamForUser(username)
        return ResponseModel(
                currentGame = game.toGameResponse(),
                currentPlayer = user!!.toUserRespone()?.copy(teamName = team.name),
                games = this.games.map { it.toGameResponse()!! },
                currentWord = if (game.charader?.username == username) game.wordsToGuessStack.lastOrNull() else null
        )

    }

    fun getTeamForUser(username: String): Team {
        val game = this.readGameForUser(username)!!
        return if (game.teamA.players.map { it.username }.contains(username))
            game.teamA
        else
            game.teamB
    }
}

val player1 = Player(
        id = 1UL,
        name = "Amin Dorostanian",
        password = "thepass",
        username = "amind"
)

fun main() {
    val objectMapper = ObjectMapper().registerKotlinModule()
    println(objectMapper.writeValueAsString(player1))
}

