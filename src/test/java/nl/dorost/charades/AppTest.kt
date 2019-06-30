package nl.dorost.charades

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


class AppTest {

    val player1 = Player(
            id = 1u,
            name = "player-2",
            password = "pass",
            points = 0,
            username = "player1",
            words = mutableListOf()
    )

    val player2 = Player(
            id = 2u,
            name = "player-2",
            password = "pass",
            points = 0,
            username = "player2",
            words = mutableListOf()
    )

    @Test
    fun `First stage of the game should start after players are ready`() {

        val game = Game(
                id = 1u,
                name = "test-game",
                status = GameStatus.STARTED,
                teamA = Team(
                        name = "Team Men",
                        id = 1u,
                        players = mutableListOf()
                ),
                teamB = Team(
                        name = "Team Women",
                        id = 1u,
                        players = mutableListOf()
                ),
                wordsCountLimit = 2
        )


        val charadesGame = CharadesGame(game)

        charadesGame.playerJoin(player1, "Team Men")
        charadesGame.playerJoin(player2, "Team Women")

        charadesGame.addWord(player1.username, Word("Some word 1"))
        charadesGame.addWord(player1.username, Word("Some word 2"))

        charadesGame.addWord(player2.username, Word("Another word 1"))

        charadesGame.nextStep()
        assertEquals(GameStatus.STARTED, charadesGame.game.status)
        charadesGame.addWord(player2.username, Word("Another word 2"))
        charadesGame.nextStep()
        assertEquals(GameStatus.SENTENCE, charadesGame.game.status)
    }


    @Test
    fun `Second stage of the game, showing the words waiting for the response from charader`() {
        val game = Game(
                id = 1u,
                name = "test-game",
                status = GameStatus.STARTED,
                teamA = Team(
                        name = "Team Men",
                        id = 1u,
                        players = mutableListOf()
                ),
                teamB = Team(
                        name = "Team Women",
                        id = 1u,
                        players = mutableListOf()
                ),
                wordsCountLimit = 2
        )
        val charadesGame = CharadesGame(game)
        charadesGame.playerJoin(player1, "Team Men")
        charadesGame.playerJoin(player2, "Team Women")
        charadesGame.addWord(player1.username, Word("Some word 1"))
        charadesGame.addWord(player1.username, Word("Some word 2"))
        charadesGame.addWord(player2.username, Word("Another word 1"))
        charadesGame.nextStep()
        charadesGame.addWord(player2.username, Word("Another word 2"))
        charadesGame.nextStep()
        assertNotNull(game.charader)
        println("Charader is ${game.charader?.username}")
        assertNotNull(game.wordsToGuessStack.last())
    }

    @Test
    fun `Player charades!`() {
        val game = Game(
                id = 1u,
                name = "test-game",
                status = GameStatus.STARTED,
                teamA = Team(
                        name = "Team Men",
                        id = 1u,
                        players = mutableListOf()
                ),
                teamB = Team(
                        name = "Team Women",
                        id = 1u,
                        players = mutableListOf()
                ),
                wordsCountLimit = 2
        )
        val charadesGame = CharadesGame(game)
        charadesGame.playerJoin(player1, "Team Men")
        charadesGame.playerJoin(player2, "Team Women")
        charadesGame.addWord(player1.username, Word("Some word 1"))
        charadesGame.addWord(player1.username, Word("Some word 2"))
        charadesGame.addWord(player1.username, Word("Some word 3"))
        charadesGame.addWord(player2.username, Word("Another word 1"))
        charadesGame.addWord(player2.username, Word("Another word 2"))
        charadesGame.addWord(player2.username, Word("Another word 3"))
        charadesGame.nextStep()
        assertEquals(GameStatus.SENTENCE, game.status)
        var wordToCharade = game.wordsToGuessStack.last()

        charadesGame.skipWord()
        assertEquals(0, charadesGame.game.charader!!.points)
        assertNotEquals(game.wordsToGuessStack.last(), wordToCharade)

        wordToCharade = game.wordsToGuessStack.last()
        charadesGame.charadesWord()
        assertEquals(1, charadesGame.game.charader!!.points)
        assertNotEquals(game.wordsToGuessStack.last(), wordToCharade)
        assertTrue(game.charader !in game.playersToPlay)

        val previousCharader = charadesGame.game.charader
        charadesGame.nextStep()
        assertNotEquals(charadesGame.game.charader, previousCharader)
    }
}
