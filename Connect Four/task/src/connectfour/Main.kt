package connectfour

fun main() {
    val connectFour = ConnectFour.getGame()
    connectFour.play()
}

enum class Direction(private val row: Int, private val column: Int) {
    UP(-1, 0),
    DOWN(1, 0),
    LEFT(0, -1),
    RIGHT(0, 1),
    UP_LEFT(-1, -1),
    UP_RIGHT(-1, 1),
    DOWN_LEFT(1, -1),
    DOWN_RIGHT(1, 1);

    fun shift(row: Int, column: Int): Pair<Int, Int> {
        return Pair(row + this.row, column + this.column)
    }
}

class ConnectFour private constructor(
    private val player1: String,
    private val player2: String,
    private val gameBoard: GameBoard,
    private val totalGames: Int
) {

    private val isMultiple = totalGames > 1
    private var score = Pair(0, 0)
    private var isPlayer1 = true
    private val currentPlayer get() = if (isPlayer1) player1 else player2
    private var command = ""

    init {
        println("$player1 VS $player2")
        gameBoard.printColumnRows()
        println(if (isMultiple) "Total $totalGames games" else "Single game")
    }

    fun play() {
        playGame()
        println("Game over!")
    }

    private fun playGame() {
        repeat(totalGames) { gameCount ->
            var drawOrWin = "It is a draw"

            if (gameCount > 0) isPlayer1 = (gameCount) % 2 == 0
            if (isMultiple) println("Game #${gameCount + 1}")
            gameBoard.print()
            while (!gameBoard.gameOver() && isContinue()) {
                if (gameBoard.insertDisc(if (isPlayer1) 'o' else '*', command)) {
                    gameBoard.print()
                    if (gameBoard.gameWon) {
                        drawOrWin = "Player $currentPlayer won"
                    } else isPlayer1 = !isPlayer1
                }
            }
            if (!gameBoard.gameOver()) return
            println(drawOrWin)
            if (isMultiple) multipleGame()
        }
    }

    private fun isContinue(): Boolean {
        command = getString("$currentPlayer's turn:")
        return command != "end"
    }

    private fun multipleGame() {
        score = if (gameBoard.gameWon) {
            if (isPlayer1) Pair(score.first + 2, score.second) else Pair(score.first, score.second + 2)
        } else Pair(score.first + 1, score.second + 1)
        println("Score\n$player1: ${score.first} $player2: ${score.second}")
        gameBoard.resetGame()
    }

    companion object {
        fun getGame(): ConnectFour {
            val playerName = "player's name:"
            val player1 = getString("Connect Four\nFirst $playerName")
            val player2 = getString("Second $playerName")

            return ConnectFour(player1, player2, GameBoard.getGameBoard(), getGameTotal())
        }

        private fun getGameTotal(): Int {
            val message = """
                Do you want to play single or multiple games?
                For a single game, input 1 or press Enter
                Input a number of games:
                """.trimIndent()
            val totalString = getString(message)
            val total = if (totalString.isEmpty()) 1 else totalString.toIntOrNull()

            if (total != null && total > 0) return total
            println("Invalid input")
            return getGameTotal()
        }
    }
}

class GameBoard private constructor(private val rows: Int = 6, private val columns: Int = 7) {
    private var board = getBoard()
    private val columnRange = 1..columns
    private val rowRange = 1..rows
    private var boardMap = getBoardMap()
    var gameWon = false
        private set

    fun print() {
        val (pipe, leftPipe, middlePipe, rightPipe) = listOf("║", "╚", "═╩", "═╝")

        println(" " + columnRange.joinToString(" "))
        board.forEach { println(it.joinToString(pipe, pipe, pipe)) }
        println(leftPipe + middlePipe.repeat(columns - 1) + rightPipe)
    }

    fun printColumnRows() = println("$rows X $columns board")

    fun insertDisc(disc: Char, columnString: String): Boolean {
        val column = columnString.toIntOrNull()
        var error =
            if (column == null) "Incorrect column number" else "The column number is out of range (1 - $columns)"

        if (column != null) {
            boardMap[column]?.let { row: Int ->
                if (row != 0) {
                    updateBoard(disc, row, column)
                    gameWon = isWin(disc, row, column)
                    return true
                } else error = "Column $column is full"
            }
        }
        println(error)
        return false
    }

    private fun updateBoard(disc: Char, row: Int, column: Int) {
        board[row - 1][column - 1] = disc
        boardMap[column] = row - 1
    }

    private fun isWin(disc: Char, row: Int, column: Int) = checkWin(disc, row, column, Direction.LEFT, Direction.RIGHT)
            || checkWin(disc, row, column, Direction.UP, Direction.DOWN)
            || checkWin(disc, row, column, Direction.UP_LEFT, Direction.DOWN_RIGHT)
            || checkWin(disc, row, column, Direction.DOWN_LEFT, Direction.UP_RIGHT)

    private fun checkWin(disc: Char, row: Int, column: Int, path1: Direction, path2: Direction) =
        checkPath(disc, row, column, path1) + checkPath(disc, row, column, path2) >= 3

    private fun checkPath(disc: Char, row: Int, column: Int, path: Direction): Int {
        var count = 0
        var shift = path.shift(row, column)

        while (isValid(shift) && isDisc(shift, disc)) {
            count++
            shift = path.shift(shift.first, shift.second)
        }
        return count
    }

    private fun isValid(shift: Pair<Int, Int>) = rowRange.contains(shift.first) && columnRange.contains(shift.second)

    private fun isDisc(shift: Pair<Int, Int>, disc: Char) = board[shift.first - 1][shift.second - 1] == disc

    fun gameOver() = gameWon || boardMap.all { it.value == 0 }

    fun resetGame() {
        if (gameWon) gameWon = false
        board = getBoard()
        boardMap = getBoardMap()
    }

    private fun getBoard() = Array(rows) { Array(columns) { ' ' } }

    private fun getBoardMap() = columnRange.associateWith { rows }.toMutableMap()

    companion object {
        fun getGameBoard(): GameBoard {
            val message = "Set the board dimensions (Rows x Columns)\nPress Enter for default (6 x 7)"
            val dimensions = getString(message).filterNot { it.isWhitespace() }
            val regex = Regex("\\d+[xX]\\d+")
            var error = "Invalid input"

            if (dimensions.isEmpty()) return GameBoard() else if (dimensions.matches(regex)) {
                val (num1, num2) = dimensions.split("x", "X").map { it.toInt() }
                val wrongDimension = if (notRange(num1)) "rows" else if (notRange(num2)) "columns" else ""
                if (wrongDimension.isEmpty()) return GameBoard(num1, num2) else {
                    error = "Board $wrongDimension should be from 5 to 9"
                }
            }
            println(error)
            return getGameBoard()
        }

        private fun notRange(num: Int) = !(5..9).contains(num)
    }
}

fun getString(message: String): String {
    println(message)
    return readln()
}