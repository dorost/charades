package nl.dorost.charades

import mu.KotlinLogging
import nl.dorost.charades.domain.*
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate

class CharadesGame(
        val repository: Repository
) {

    val log = KotlinLogging.logger {}
    lateinit var game: Game
    private var scheduler: TimerTask? = null
    private var timeRemainingSeconds: Int = 0
    private var wordsToGuessStack: MutableList<Word> = mutableListOf()
    private var playersToPlay: MutableList<Player> = mutableListOf()
    private var charadedWords: MutableList<Word> = mutableListOf()
    private var startCommandReceived: Boolean = false
    private var charader: Player? = null
    private var charadingStarted: Boolean = false
    fun getCharader() = this.charader


    init {
        Timer().scheduleAtFixedRate(0, 200) {
            if (this@CharadesGame::game.isInitialized)
                changeBasedOnGameStage()
        }
    }

    fun createGame(game: Game) {
        this.game = game
    }

    fun getGameFromId(gameId: UInt) {
        this.game = this.repository.loadGame(gameId) ?: throw RuntimeException("GameId $gameId not found!")
    }

    fun getGameFromUserName(userName: String) {
        this.game = repository.readGameForUser(userName) ?: throw RuntimeException("Game not found for user $userName!")
    }

    fun changeBasedOnGameStage() {
        when (game.status) {
            GameStatus.STARTED -> {
                log.info { "Game started waiting for players to get ready!" }
                if (checkIfReadyToStart()) {
                    game.status = GameStatus.SENTENCE
                }
            }
            GameStatus.SENTENCE -> handleCharading()
            GameStatus.ONE_WORD -> handleCharading()
            GameStatus.PANTHOMIME -> handleCharading()
            GameStatus.FINISHED -> {
                game.charadingStatus = CharadingStatus.NOT_STARTED
                charader = null

            }
        }

    }

    private fun handleCharading() {
        when (game.charadingStatus) {
            CharadingStatus.NOT_STARTED -> {
                game.charadingStatus = CharadingStatus.STARTED
                makeGameReadyForCharading()
            }
            CharadingStatus.STARTED -> {
                log.info { "Charading..." }
                if (this.playersToPlay.isEmpty() && this.timeRemainingSeconds==0) {
                    game.charadingStatus = CharadingStatus.FINISHED
                    log.info { "Everyone charaded!" }
                }
            }
            CharadingStatus.VOTING -> {
                log.info { "Voting..." }
                if (this.charadedWords.isEmpty() && !playersToPlay.isEmpty()){
                    changeTurn()
                    pickCharader()
                }
                if (everyoneVoted()) {

                    // apply points changes

                    this.charadedWords.forEach { word ->
                        if (word.positiveVote>word.negativeVote){
                            log.info { "Accepted word ${word.text}" }
                            // do nothing
                        }else{
                            log.info { "Rejected word ${word.text}" }
                            // revert
                            this.game.teams.first { it.name == getTeamName(this.charader!!.username) }.players.first { it.username==this.charader!!.username }.points--
                        }

                    }
                    this.charader = null
                    this.charadedWords.clear()

                    if (this.playersToPlay.isEmpty()){
                        game.charadingStatus = CharadingStatus.FINISHED
                        log.info { "Everyone charaded!" }
                        return
                    }
                    this.game.charadingStatus = CharadingStatus.STARTED
                    changeTurn()
                    pickCharader()
                }

            }
            CharadingStatus.FINISHED -> {
                log.info { "This stage is finished!" }
                goToNextStage()
            }
        }
    }

    private fun goToNextStage() {
        log.info { "Chaning the game stage from ${game.status.toString()}" }
        resetVotes()
        when (game.status) {
            GameStatus.STARTED -> this.game.status = GameStatus.SENTENCE
            GameStatus.SENTENCE -> this.game.status = GameStatus.ONE_WORD
            GameStatus.ONE_WORD -> this.game.status = GameStatus.PANTHOMIME
            GameStatus.PANTHOMIME -> this.game.status = GameStatus.FINISHED
            GameStatus.FINISHED -> {
            }
        }
        this.game.charadingStatus = CharadingStatus.NOT_STARTED
        log.info { "New status ${game.status}" }

    }

    private fun makeGameReadyForCharading() {
        fillPlayersToPlay()
        makeWordsPot()
        changeTurn()
        pickCharader()
    }

    private fun checkIfReadyToStart() = allPlayersReady()

    fun fillPlayersToPlay() {
        this.playersToPlay = game.teams.flatMap { it.players }.toMutableList()
    }

    fun changeTurn() {
        if (game.teamIdTurn == null)
            game.teamIdTurn = game.teams.map { it.id }.shuffled().first()
        else
            game.teamIdTurn = game.teams.filter { it.id != game.teamIdTurn }.minBy { it.players.map { it.playingsCount }.sum() }!!.id
        log.info { "Turn changed to ${game.teamIdTurn}" }
    }

    fun allPlayersReady() = game.teams.flatMap { it.players }
            .all { it.words.size == game.wordsCountLimit } && game.teams.map { it.players.size }.toSet().size == 1

    fun pickCharader() {
        resetVotes()

        this.charader = playersToPlay.filter { user ->
            game.teams.first { it.id == this.game.teamIdTurn }.players.map { it.username }.contains(user.username)
        }.shuffled().first().apply {
            this.isCharader = true
            this.playingsCount++
        }

        this.playersToPlay.removeIf { it.id == this.charader!!.id }
        this.timeRemainingSeconds = game.timeLimitSeconds
    }

    private fun resetVotes() {
        game.teams.flatMap { it.players }.forEach {
            it.votes.clear()
        }
    }

    fun makeWordsPot() {
        this.wordsToGuessStack.clear()
        this.wordsToGuessStack.addAll(game.teams.flatMap { it.players }.flatMap { it.words }.shuffled())
    }

    fun addWord(userName: String, word: Word): Boolean {
        val player = game.teams.flatMap { it.players }.first { it.username == userName }
        return if (player.words.size == game.wordsCountLimit)
            false
        else {
            player.words.add(word)
            true
        }
    }

    fun playerJoinToGame(username: String, gameId: UInt, teamId: UInt): Boolean {
        this.game = repository.loadGame(gameId)!!
        if (game.status != GameStatus.STARTED)
            return false
        if (game.teams.flatMap { it.players }.map { it.username }.contains(username))
            return false
        game.teams.first { it.id == teamId }.players.add(repository.readUser(username)!!)

        return true

    }

    fun charadesWord() {
        val word = this.wordsToGuessStack.last()
        this.charader!!.charadesWords.add(word.copy(positiveVote = 0, negativeVote = 0))
        this.wordsToGuessStack.remove(word)

        //update points
        this.charader!!.points++
        this.game.teams.first{ it.name==getTeamName(this.charader!!.username) }.points++

        if (this.wordsToGuessStack.size == 0)
            makeWordsPot()

    }


    fun charadingStarted(userName: String) {
        if (userName != this.charader!!.username)
            return
        log.info { "Started the timer..." }
        charadingStarted = true
        scheduler = Timer().scheduleAtFixedRate(0, 1000) {
            this@CharadesGame.timeRemainingSeconds--
            if (this@CharadesGame.timeRemainingSeconds == 0) {
                this.cancel()
                this@CharadesGame.charadedWords = this@CharadesGame.charader!!.charadesWords
                this@CharadesGame.game.charadingStatus = CharadingStatus.VOTING
                charadingStarted = false
                changeBasedOnGameStage()
            }
        }
    }

    fun skipWord() {
        val word = this.wordsToGuessStack.last()
        this.wordsToGuessStack = (mutableListOf(word)
                + this.wordsToGuessStack.filter { it != word }).toMutableList()
    }

    fun vote(userName: String, word: String, vote: Vote) {
        val player = game.teams.flatMap { it.players }.first { it.username == userName }
        if ( this.charadedWords.isEmpty() || player.votes.size==this.charadedWords.size) {
            return
        }


        if (player.votes.contains(word))
            return
        if (vote == Vote.ACCEPTED) {
            this.charadedWords.first { it.text == word }.positiveVote++
        } else
            this.charadedWords.first { it.text == word }.negativeVote++

        player.votes.add(word)
    }

    private fun everyoneVoted(): Boolean = this.charadedWords.map { it.positiveVote + it.negativeVote }.all { it==this.game.teams.flatMap { it.players }.count() }

    fun createUpdate(userName: String) = ResponseModel(
            gameName = this.game.name,
            gameStatus = this.game.status.toString(),
            charadingStatus = this.game.charadingStatus.toString(),
            currentPlayer = this.repository.readUser(userName)?.toPlayerResponse(getTeamName(userName)),
            currentWord = if (this.charader?.username == userName) this.wordsToGuessStack.lastOrNull() else null,
            charadedWords = this.charadedWords,
            players = this.game.teams.flatMap { it.players }.map { user ->
                user.toPlayerResponse(getTeamName(user.username))
            },
            teams = this.game.teams.map { TeamResponse(id = it.id, teamName = it.name, points = it.players.map { it.points }.sum()) },
            charader = this.charader?.let { it.toPlayerResponse(getTeamName(userName)) },
            timeSecondsRemaining = this.timeRemainingSeconds,
            wordsCountLimit = this.game.wordsCountLimit,
            status = this.game.status.s,
            charadingStarted = charadingStarted,
            winnerTeam = this.game.teams.maxBy { it.points }?.name


    )

    private fun getTeamName(userName: String) =
            this.game.teams.first { it.players.map { it.username }.contains(userName) }.name

    fun save() {
        repository.saveGame(this.game)
    }

}

