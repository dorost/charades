package nl.dorost.charades.domain

import kotlin.random.Random
import kotlin.random.nextULong


data class Word(
        val text: String,
        val id: ULong? = Random.nextULong(),
        var positiveVote:Int = 0,
        var negativeVote:Int = 0
)
