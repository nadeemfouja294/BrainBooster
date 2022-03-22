package com.example.brain_booster.models

import com.example.brain_booster.utilis.DEFAULT_ICONS

private const val TAG = "MemoryGame"

class MemoryGame(private val boardSize: BoardSize, private val customGameImages: List<String>?) {

    lateinit var cards: List<MemoryCard>
    private var indexOfSingleSelectedCard: Int? = null
     var numPairsFound = 0
     var numOfMoves=0

    init {
        if(customGameImages==null){
        var chosenImages = DEFAULT_ICONS.shuffled().take(boardSize.getNumPairs())
        chosenImages += chosenImages.shuffled()
        cards = chosenImages.map { MemoryCard(it) }
        }else{
            val randomizeImage=(customGameImages+customGameImages).shuffled()
            cards=randomizeImage.map{MemoryCard(it.hashCode(),it)}
        }
    }

    fun flipCard(position: Int): Boolean {
        numOfMoves++
        val card = cards[position]
        var foundMatch = false
        if (indexOfSingleSelectedCard == null) {
            restoreCards(position)
            indexOfSingleSelectedCard = position
        } else {
            foundMatch = checkForMatch(indexOfSingleSelectedCard!!, position)
            indexOfSingleSelectedCard = null
        }


        card.isFaceUp = !card.isFaceUp
        return foundMatch
    }

    private fun checkForMatch(position1: Int, position2: Int): Boolean {
        if (cards[position1].identifier == cards[position2].identifier) {
            cards[position1].isMatched = true
            cards[position2].isMatched = true
            numPairsFound++
            return true
        }
        return false
    }

//    private fun checkForMatch(position1: Int, position2: Int): Boolean {
//        if (cards[position1].chosenImage == cards[position2].chosenImage) {
//            cards[position1].isMatched = true
//            cards[position2].isMatched = true
//            numPairsFound++
//            return true
//        }
//        return false
//
//    }

    private fun restoreCards(position: Int) {
        for (card in cards) {
            if (!card.isMatched)
                card.isFaceUp = false
        }
        //  cards[position].isFaceUp=true
    }

    fun haveWonGame(): Boolean {
        return numPairsFound == boardSize.getNumPairs()
    }

    fun isCardFaceUp(position: Int): Boolean {
        return cards[position].isFaceUp

    }

    fun getNumMoves(): Int {
        return numOfMoves/2

    }
}