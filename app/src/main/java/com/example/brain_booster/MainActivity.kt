package com.example.brain_booster

import android.animation.ArgbEvaluator
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.brain_booster.R.layout
import com.example.brain_booster.models.BoardSize
import com.example.brain_booster.models.MemoryGame
import com.example.brain_booster.utilis.CUSTOM_BOARD_SIZE
import com.example.brain_booster.utilis.EXTRA_GAME_NAME
import com.example.brain_booster.utilis.showToast
import com.github.jinatonic.confetti.CommonConfetti
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso


class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
        private const val CREATE_REQUEST_CODE = 294
    }

    private lateinit var adapter: MemoryBoardAdapter
    private lateinit var clRoot: ConstraintLayout
    private lateinit var memoryGame: MemoryGame
    private lateinit var rvBoard: RecyclerView
    private lateinit var tvNumMoves: TextView
    private lateinit var tvNumPairs: TextView
    private lateinit var firestore: FirebaseFirestore
    private var gameName: String? = null
    private var customGameImages: List<String>? = null
    private var boardSize: BoardSize = BoardSize.EASY


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout.activity_main)
        rvBoard = findViewById(R.id.rvBoard)
        tvNumMoves = findViewById(R.id.tvNumMoves)
        tvNumPairs = findViewById(R.id.tvNumPairs)
        clRoot = findViewById(R.id.clRoot)
        firestore = FirebaseFirestore.getInstance()
        setUpBoard()
    }

    //menu inflate
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.mnenu_main, menu)
        return true
    }

    //menu
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.miRfresh -> {
                if (memoryGame.getNumMoves() > 0 && !memoryGame.haveWonGame()) {
                    showAlertDialogue("Quit your current game", null, View.OnClickListener {
                        setUpBoard()
                    })
                } else
                    setUpBoard()
                return true
            }
            R.id.miNewSize -> {
                showNewSizeDialogue()
                return true
            }
            R.id.mi_costum -> {
                showCreationDialogue()
                return true
            }
            R.id.miDownloadCustomGame -> {
                showDownloadDialogue()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    //Get name of the game user wants to play
    private fun showDownloadDialogue() {
        val getGameNameView = LayoutInflater.from(this).inflate(R.layout.get_game_name, null)
        showAlertDialogue("Fetch memory game", getGameNameView, View.OnClickListener {
            val etDownloadGame = getGameNameView.findViewById<EditText>(R.id.etGameName)
            val customGame = etDownloadGame.text.toString()
            downloadGame(customGame)
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == CREATE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val customGameName: String? = data?.getStringExtra(EXTRA_GAME_NAME)
            if (customGameName == null) {
                Log.i(TAG, "Got null custom game name from CreateActivity")
                return
            }
            downloadGame(customGameName)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    //Download requested game images from Firestore
    private fun downloadGame(customGameName: String) {
        firestore.collection("games").document(customGameName).get()
            .addOnSuccessListener { document ->
                val userImageList = document.toObject(UserImageList::class.java)
                if (userImageList?.images == null) {
                    Log.i(TAG, "Invalid custom game image data from Firestore ")
                    showToast("Sorry, we couldn't find anr game with the name $customGameName")
                    return@addOnSuccessListener
                }
                val numCards = userImageList.images.size * 2
                boardSize = BoardSize.getByValue(numCards)
                gameName = customGameName
                customGameImages = userImageList.images
                // download images for better performance
                for (imageUrl in userImageList.images) {
                    Picasso.get().load(imageUrl).fetch()
                }
                setUpBoard()
            }.addOnFailureListener { exception ->
                Log.i(TAG, "Failed to retrieve game from Firestore ", exception)

            }
    }

    //Get size of the game
    private fun showCreationDialogue() {
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialogue_board_size, null)
        val selectedRadioButton = boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)
        when (boardSize) {
            BoardSize.MEDIUM -> selectedRadioButton.check(R.id.rbMedium)
            BoardSize.HARD -> selectedRadioButton.check(R.id.rbHard)
            else -> selectedRadioButton.check(R.id.rbEasy)
        }
        showAlertDialogue("Create your own memory board", boardSizeView, View.OnClickListener {
            val customBoardSize = when (selectedRadioButton.checkedRadioButtonId) {
                R.id.rbMedium -> BoardSize.MEDIUM
                R.id.rbHard -> BoardSize.HARD
                else -> BoardSize.EASY
            }
            //navigate to a new activity
            val intent = Intent(this, CreateActivity::class.java)
            intent.putExtra(CUSTOM_BOARD_SIZE, customBoardSize)
            startActivityForResult(intent, CREATE_REQUEST_CODE)

        })
    }

    private fun showNewSizeDialogue() {
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialogue_board_size, null)
        val selectedRadioButton = boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)
        when (boardSize) {
            BoardSize.MEDIUM -> selectedRadioButton.check(R.id.rbMedium)
            BoardSize.HARD -> selectedRadioButton.check(R.id.rbHard)
            else -> selectedRadioButton.check(R.id.rbEasy)
        }
        showAlertDialogue("Chose new board size", boardSizeView, View.OnClickListener {
            boardSize = when (selectedRadioButton.checkedRadioButtonId) {
                R.id.rbMedium -> BoardSize.MEDIUM
                R.id.rbHard -> BoardSize.HARD
                else -> BoardSize.EASY
            }
            gameName = null
            customGameImages = null
            setUpBoard()

        })
    }

    private fun showAlertDialogue(
        title: String,
        view: View?,
        positiveClickListener: View.OnClickListener
    ) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(view)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Ok") { _, _ ->
                positiveClickListener.onClick(null)
            }.show()

    }

    private fun  setUpBoard() {
        supportActionBar?.title = gameName ?: getString(R.string.app_name)
        when (boardSize) {
            BoardSize.EASY -> {
                tvNumMoves.text = "EASY: 4*2"
                tvNumPairs.text = "0/4"
            }
            BoardSize.MEDIUM -> {
                tvNumMoves.text = "MEDIUM: 6*3"
                tvNumPairs.text = "0/9"
            }
            BoardSize.HARD -> {
                tvNumMoves.text = "HARD: 6*4"
                tvNumPairs.text = "0/12"
            }
        }
        tvNumPairs.setTextColor(ContextCompat.getColor(this, R.color.red))
        memoryGame = MemoryGame(boardSize, customGameImages)
        rvBoard.setHasFixedSize(true)
        rvBoard.layoutManager = GridLayoutManager(this, boardSize.getWidth())

        //Adapter
        adapter = MemoryBoardAdapter(
            this,
            boardSize,
            memoryGame.cards,
            object : MemoryBoardAdapter.CardClickListener {
                override fun onCardClicked(position: Int) {
                    // Log.i(TAG,"Main Act $position")
                    updateGameWithFlip(position)

                }

            })
        rvBoard.adapter = adapter
    }


    private fun updateGameWithFlip(position: Int) {
        if (memoryGame.haveWonGame()) {
            // Alert the user about result
            showToast("You already have won th game ")
            return
        }
        if (memoryGame.isCardFaceUp(position)) {
            // Alert thr user about wrong move
            showToast("Invalid move! ")
            return

        }
        if (memoryGame.flipCard(position)) {
            tvNumPairs.text = "${memoryGame.numPairsFound}/${boardSize.getNumPairs()}"
            val color = ArgbEvaluator().evaluate(
                memoryGame.numPairsFound.toFloat() / boardSize.getNumPairs(),
                ContextCompat.getColor(this, R.color.red),
                ContextCompat.getColor(this, R.color.green)
            ) as Int
            tvNumPairs.setTextColor(color)
            if (memoryGame.haveWonGame()) {
//                Toast.makeText(this, "Congratulations!!,You  win th game ", Toast.LENGTH_LONG)
//                    .show()
                showToast("Congratulations!!,You  win th game ")
                CommonConfetti.rainingConfetti(
                    clRoot,
                    intArrayOf(Color.YELLOW, Color.GREEN, Color.MAGENTA, Color.RED)
                ).oneShot()

            }
            // Alert the user about result

        }
        tvNumMoves.text = "Moves: ${memoryGame.getNumMoves()}"
        adapter.notifyDataSetChanged()
    }


}