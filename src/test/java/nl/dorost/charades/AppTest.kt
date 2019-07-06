package nl.dorost.charades

import kotlinx.coroutines.delay
import nl.dorost.charades.domain.*
import org.junit.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


class AppTest {

    val repository = MemoryStorage()
    val charadesGame = CharadesGame(repository)

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

    val game = Game(
            id = 1u,
            name = "test-game",
            status = GameStatus.STARTED,
            teams = mutableListOf(
                    Team(
                            name = "Team Men",
                            id = 1u
                    ),
                    Team(
                            name = "Team Women",
                            id = 2u
                    )
            ),
            wordsCountLimit = 2,
            timeLimitSeconds = 3
    )

//    @Test
//    fun `Test one whole flow of the game`() {
//
//        repository.createUser(player1)
//        repository.createUser(player2)
//        charadesGame.createGame(game)
//        charadesGame.save()
//
//        charadesGame.playerJoinToGame(player1.username,game.id, 1u)
//        charadesGame.playerJoinToGame(player2.username,game.id, 2u)
//
//        charadesGame.addWord(player1.username, Word("Some word 1"))
//        charadesGame.addWord(player1.username, Word("Some word 2"))
//        charadesGame.addWord(player2.username, Word("Another word 1"))
//        charadesGame.changeBasedOnGameStage()
//        assertEquals(GameStatus.STARTED, charadesGame.game.status)
//        charadesGame.addWord(player2.username, Word("Another word 2"))
//        Thread.sleep(1000)
//
//        assertEquals(GameStatus.SENTENCE, charadesGame.game.status)
//
//        // Now charading should start
//        assertEquals(CharadingStatus.STARTED, charadesGame.game.charadingStatus)
//        var charader = charadesGame.getCharader()
//        assertNotNull(charader)
//        println("Charader is ${charader.username}")
//
//        charadesGame.charadingStarted(charader.username)
//        charadesGame.charadesWord()
//
//        assertEquals(charader.points, 1)
//        Thread.sleep(4000)
//        assertEquals(CharadingStatus.VOTING, charadesGame.game.charadingStatus)
//        assertEquals(charadesGame.game.teams.map {  it.points }.sum(), 1)
//
//        val update1 = charadesGame.createUpdate(player1.username)
//        val update2 = charadesGame.createUpdate(player2.username)
//        charadesGame.vote(player2.username, update2.charadedWords!!.first().text, Vote.ACCEPTED)
//        charadesGame.vote(player1.username, update1.charadedWords!!.first().text, Vote.ACCEPTED)
//
//        Thread.sleep(1000)
//
//        assertEquals(CharadingStatus.STARTED, charadesGame.game.charadingStatus)
//        assertNotEquals(charader, charadesGame.getCharader())
//
//        Thread.sleep(100)
//        assertEquals(charadesGame.game.timeLimitSeconds, charadesGame.createUpdate(player1.username).timeSecondsRemaining)
//
//        charadesGame.charadingStarted(charadesGame.getCharader()!!.username)
//        val currentWord = charadesGame.createUpdate(charadesGame.getCharader()!!.username).currentWord!!
//        charadesGame.skipWord()
//        val nextWord = charadesGame.createUpdate(charadesGame.getCharader()!!.username).currentWord!!
//        assertNotEquals(currentWord, nextWord)
//        Thread.sleep(4000)
//        assertEquals(CharadingStatus.VOTING, charadesGame.game.charadingStatus)
//        charadesGame.vote(player2.username, update2.charadedWords!!.first().text, Vote.ACCEPTED)
//        charadesGame.vote(player1.username, update1.charadedWords!!.first().text, Vote.ACCEPTED)
//
//        Thread.sleep(1000)
//        assertEquals(GameStatus.ONE_WORD, charadesGame.game.status)
//
//
//    }


}
