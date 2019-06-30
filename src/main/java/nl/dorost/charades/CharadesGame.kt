package nl.dorost.charades

import mu.KotlinLogging
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate

class CharadesGame(val game: Game) {

    val log = KotlinLogging.logger {}

    var scheduler: TimerTask? = null
    fun nextStep() {
        log.info { "Entered ${game.status} state! CharadeDone: ${game.charadeDone}" }
        when (game.status) {
            GameStatus.STARTED -> {
                log.info { "Game started waiting for players to get ready!" }
                if (allPlayersReady() && game.teamA.players.size == game.teamB.players.size) {
                    log.info { "Players are ready now! Starting stage one!" }
                    game.status = GameStatus.SENTENCE
                    fillPlayersToPlay()
                    makeWordsPot()
                    pickCharader()
                }
            }
            GameStatus.SENTENCE -> {
                if (game.playersToPlay.isEmpty()) {
                    game.status = GameStatus.ONE_WORD
                    fillPlayersToPlay()
                    makeWordsPot()
                    pickCharader()
                } else if (game.charadeDone) {
                    log.info { "Changing turns!" }
                    changeTurn()
                    pickCharader()
                }
            }
            GameStatus.ONE_WORD -> {
                if (game.playersToPlay.isEmpty()) {
                    game.status = GameStatus.PANTHOMIME
                    fillPlayersToPlay()
                    makeWordsPot()
                    pickCharader()

                } else if (game.charadeDone) {
                    log.info { "Changing turns!" }
                    changeTurn()
                    pickCharader()
                }
            }
            GameStatus.PANTHOMIME -> {
                if (game.playersToPlay.isEmpty()) {
                    game.status = GameStatus.FINISHED
                    game.charader = null
                } else if (game.charadeDone) {
                    log.info { "Changing turns!" }
                    changeTurn()
                    pickCharader()
                }
            }
            GameStatus.FINISHED -> {

            }
        }
    }

    private fun fillPlayersToPlay() {
        game.playersToPlay = (game.teamA.players + game.teamB.players).toMutableList()
        log.info { "Players refilled, count: ${game.playersToPlay.size}" }
    }

    fun changeTurn() {
        if (game.turn == null)
            game.turn = if (Math.random() < 0.5) game.teamA else game.teamB
        else
            game.turn = if (game.turn == game.teamA) game.teamB else game.teamA
        log.info { "Turn changed to ${game.turn?.name}" }
    }

    fun allPlayersReady(): Boolean {
        return (game.teamA.players + game.teamB.players)
                .all { it.words.size >= game.wordsCountLimit }
    }

    fun pickCharader() {
        if (game.turn!!.name == game.teamA.name && game.playersToPlay.any { game.teamA.players.contains(it) }) {
            game.charader = game.playersToPlay.filter { game.teamA.players.contains(it) }.shuffled().first()
        } else {
            game.charader = game.playersToPlay.filter { game.teamB.players.contains(it) }.shuffled().first()
        }
        if(game.playersToPlay.remove(game.charader!!))
            log.info { "Removed charader from remaining list!" }
        else
            log.info { "Ooops!" }
        game.timeRemainingSeconds = game.timeLimitSeconds
    }

    fun makeWordsPot() {
        game.wordsToGuessStack.clear()
        game.wordsToGuessStack.addAll(
                (game.teamA.players.flatMap { it.words } +
                        game.teamB.players.flatMap { it.words }
                        ).shuffled()
        )
    }

    fun addWord(userName: String, word: Word): Boolean {
        val user = getPlayerByUserName(userName)
        if (user.words.map { it.text }.contains(word.text) || user.words.size == game.wordsCountLimit)
            return false
        user.words.add(word)
        return true
    }

    fun playerJoin(player: Player, teamName: String): Boolean {
        if (game.status != GameStatus.STARTED)
            return false
        if (game.teamA.name == teamName) {
            if (game.teamA.players.none { it.username == player.username })
                game.teamA.players.add(player)
        } else {
            if (game.teamB.players.none { it.username == player.username })
                game.teamB.players.add(player)
        }
        return true

    }

    fun charadesWord() {
        val word = game.wordsToGuessStack.last()
        game.charader!!.charadesWords.add(word)
        game.wordsToGuessStack.remove(word)
        game.charader!!.points++
        if (game.teamA.players.map { it.username }.contains(game.charader!!.username)) {
            game.teamA.points++
        } else {
            game.teamB.points++
        }

    }

    fun charadingStarted(userName: String): Boolean {
        if (game.charader!!.username != userName)
            return false
        log.info { "Charading started for $userName" }
        if (game.getTeamForUser(userName).name == game.teamA.name)
            game.teamA.players.first { it.username == userName }.playingsCount++
        else
            game.teamB.players.first { it.username == userName }.playingsCount++


        game.charadingStarted = true
        game.timeRemainingSeconds = game.timeLimitSeconds

        scheduler = Timer().scheduleAtFixedRate(0, 1000) {
            game.timeRemainingSeconds--
            if (game.timeRemainingSeconds == 0) {
                this.cancel()
                game.charadeDone = true
                game.charadingStarted = false
                nextStep()
            }
        }
        return true
    }

    fun skipWord() {
        val word = game.wordsToGuessStack.last()
        game.wordsToGuessStack = (mutableListOf(word)
                + game.wordsToGuessStack.filter { it != word }).toMutableList()
    }

    private fun getPlayerByUserName(userName: String) = (game.teamA.players
            + game.teamB.players)
            .firstOrNull { it.username == userName }
            ?: throw RuntimeException("User $userName not found!")


}

fun main() {
    var x = 0
    val schedule = Timer().scheduleAtFixedRate(0, 1000) {
        x++
        println("Hey")
        if (x > 5)
            this.cancel()
    }
    println("DONE")
}