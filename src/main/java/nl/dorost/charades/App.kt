package nl.dorost.charades

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.*
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.html.respondHtml
import io.ktor.http.ContentType
import io.ktor.http.cio.websocket.Frame
import io.ktor.jackson.jackson
import io.ktor.request.receive
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import kotlinx.coroutines.channels.Channel
import kotlinx.html.*
import org.apache.commons.io.IOUtils
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import com.mitchellbosecke.pebble.PebbleEngine
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.sessions.Sessions
import io.ktor.sessions.cookie
import kotlinx.coroutines.delay
import java.io.StringWriter


fun main(args: Array<String>) {

    val objectMapper = ObjectMapper().registerKotlinModule().setSerializationInclusion(JsonInclude.Include.NON_NULL)
    val storage = MemoryStorage()
    val engine = PebbleEngine.Builder().build()
    val joinGameTemplate = engine.getTemplate("static/join.html")


    val env = applicationEngineEnvironment {
        connector {
            host = "0.0.0.0"
            port = 9090
        }


        module {
            install(WebSockets)
            install(DefaultHeaders)
            install(CallLogging)
            install(Sessions) {
                cookie<CharadeSession>("CHARADES")
            }
            install(ContentNegotiation) {
                jackson {
                    setSerializationInclusion(JsonInclude.Include.NON_NULL)
                }
            }
            install(Authentication) {
                basic {
                    realm = "ktor"
                    validate { credentials ->
                        val user = storage.readUser(credentials.name)

                        return@validate user?.let { player ->
                            return@let if (player.password == hash(credentials.password)) UserIdPrincipal(credentials.name)
                            else null
                        }
                    }
                }
            }


            routing {
                static("/") {
                    resources("static")
                }

                webSocket("/ws/{user-name}/") {
                    val username = call.parameters["user-name"]

                    while (true) {
                        username?.let {
                            val response = storage.readUpdateForUser(username)
                            val toSend = objectMapper.writeValueAsString(response)
                            outgoing.send(Frame.Text(toSend))
                            delay(1000)
                        }
                    }
                }

                authenticate {

                    get("/startCharade") {
                        val username = call.principal<UserIdPrincipal>()?.name
                        val game = storage.readGameForUser(username!!)
                        val chGame = CharadesGame(game!!)
                        if (chGame.charadingStarted(username))
                            call.respond(ResponseModel(message = "Started charading!"))
                        else
                            call.respond(ResponseModel(message = "Failed to start to charade!"))
                    }

                    get("/word/{word}") {
                        val word = call.parameters["word"]
                        val username = call.principal<UserIdPrincipal>()!!.name
                        val game = storage.readGameForUser(username!!)
                        val chGame = CharadesGame(game!!)
                        if (chGame.addWord(username, Word(text = word!!))) {
                            call.respond(ResponseModel(message = "OK!"))
                            chGame.nextStep()
                        } else {
                            call.respond(ResponseModel(message = "Not possible anymore!"))
                            return@get
                        }

                    }

                    get("/create") {
                        call.respondText(
                                IOUtils.toString(getFile("static/create.html")),
                                ContentType.Text.Html
                        )
                    }

                    get("/users") {
                        call.respond(storage.readAllUsers().map { it.password = null })
                    }
                    get("/user/") {
                        val user = call.principal<UserIdPrincipal>()!!.name
                        val responseModel = ResponseModel(currentPlayer = storage.readUser(user)!!.toUserRespone())
                        call.respond(responseModel)
                    }

                    get("/game/{gameId}/{teamId}") {
                        log.info("Params : ${call.parameters["gameId"]} and ${call.parameters["teamID"]}")
                        val gameId: UInt? = (call.parameters["gameId"] as? String)?.toUInt()
                        val teamId: ULong? = (call.parameters["teamId"] as? String)?.toULong()
                        if (gameId == null || teamId == null) {
                            call.respondText("GameId or TeamId should be specified!")
                            return@get
                        }
                        val game = storage.loadGame(gameId = gameId!!)
                        if (game == null) {
                            call.respondText("Couldn't find the game with id $gameId!")
                            return@get
                        }

                        val player = storage.readUser(call.principal<UserIdPrincipal>()!!.name)
                        val team = if ((game as Game).teamA.id == teamId) game.teamA else game.teamB

                        val charades = CharadesGame(game)
                        if (charades.playerJoin(player!!, team.name))
                            storage.saveGame(charades.game)

                        call.respondText(
                                IOUtils.toString(getFile("static/game.html"), "utf-8"),
                                ContentType.Text.Html
                        )
                    }

                    get("/joinOrCreateGame") {


                        // Here list all games and names of teams for the user
                        // or create a game
                        val writer = StringWriter()
                        joinGameTemplate.evaluate(
                                writer, createGamesList(storage)
                        )
                        call.respondText(
                                writer.toString(),
                                ContentType.Text.Html
                        )
                        writer.close()

                    }

                    post("/create") {
                        val parameters = call.receiveParameters()
                        val newGame = storage.saveGame(
                                Game(
                                        name = parameters["game-name"]!!,
                                        teamA = Team(
                                                name = parameters["team-a-name"]!!
                                        ),
                                        teamB = Team(
                                                name = parameters["team-b-name"]!!
                                        )
                                )
                        )
                        call.respondHtml {
                            body {
                                p {
                                    +"Game with id ${newGame.id} created Successfully!"
                                }
                                p {
                                    +"Please go to "
                                    a("/joinOrCreateGame") {
                                        +"here"
                                    }
                                }
                            }
                        }
                    }

                }

                post("/signUp") {
                    val parameters = call.receiveParameters()
                    val player = Player(
                            name = parameters["firstname-input"]!!,
                            username = parameters["username-input"]!!,
                            password = hash(parameters["password-input"]!!)
                    )
                    val playerId = storage.createUser(player)
                    playerId?.let {

                        call.respondHtml {
                            body {
                                h2 { +"User successfully created with id $it" }
                                form("/joinOrCreateGame") {
                                    button {
                                        +"Let's Play!"
                                    }
                                }
                            }
                        }
                    } ?: kotlin.run { call.respondText("Oops something went wrong!") }
                }

                get("/createUser") {
                    call.respondHtml {
                        signupForm()
                    }
                }


                get("/") {
                    call.respondHtml {
                        body {
                            h1 { +"Login/Sign up" }
                            div {
                                form(action = "/joinOrCreateGame") {
                                    button {
                                        +"I already have an account, Let's play!"
                                    }
                                }
                                br
                                div {
                                    form(action = "/createUser") {
                                        button {
                                            +"Sign up!"
                                        }
                                    }
                                }
                            }
                        }
                    }
                }


                post("/user") {
                    val user = call.receive(Player::class)
                    call.respond(ResponseModel(message = "user created with ${user.name}"))
                }





                get("/signout") {
                    call.respondRedirect {
                        encodedPath = "/"
                        user = "someone"
                        password = "x"
                    }
                }


            }
        }
    }

    val server = embeddedServer(Netty, environment = env)
    server.start(wait = true)
}


fun createGamesList(storage: Storage): Map<String, Any> =
        mapOf(
                "games" to storage.readAllGames().map { game ->
                    mapOf(
                            "name" to game.name,
                            "id" to game.id,
                            "teamA" to mapOf("name" to game.teamA.name, "id" to game.teamA.id),
                            "teamB" to mapOf("name" to game.teamB.name, "id" to game.teamB.id)
                    )
                }
        )


private fun HTML.signupForm() {
    body {
        h2 {
            +"Create a user:"
        }
        form("/signUp", encType = FormEncType.applicationXWwwFormUrlEncoded, method = FormMethod.post) {
            p {
                label { +"First Name: " }
                textInput {
                    name = "firstname-input"
                    required = true
                }
            }
            p {
                label { +"User Name: " }
                textInput {
                    name = "username-input"
                    required = true
                }
            }
            p {
                label {
                    +"Password: "
                }
                passwordInput {
                    name = "password-input"
                    required = true
                }
            }
            p {
                submitInput {
                    value = "send"
                }
            }
        }
    }
}

fun hash(password: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hash = digest.digest(
            password.toByteArray(StandardCharsets.UTF_8))
    return String(hash)
}

class CharadeSession(val userId: String)

private fun getFile(fileName: String) = Thread.currentThread().contextClassLoader.getResourceAsStream(fileName)
