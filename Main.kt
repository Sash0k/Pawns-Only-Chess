package chess

/**
 * Шахматная доска
 */
class Table {
    private val pattern = Regex("^([a-h][1-8]){2}\$")
    private val passant: MutableMap<Pawn, Cell> = mutableMapOf()
    private val table = mutableListOf(
        MutableList(size = 8, init = { " " }),
        MutableList(size = 8, init = { "W" }),
        MutableList(size = 8, init = { " " }),
        MutableList(size = 8, init = { " " }),
        MutableList(size = 8, init = { " " }),
        MutableList(size = 8, init = { " " }),
        MutableList(size = 8, init = { "B" }),
        MutableList(size = 8, init = { " " }),
    )

    /**
     * Победил один из игроков:
     * 1) пешка дошла до противоположного края доски
     * 2) у одного из игроков не осталось пешек
     */
    fun gameWin() = table[7].contains("W")
            || table[0].contains("B")
            || !table.flatten().contains("W")
            || !table.flatten().contains("B")

    /**
     * Ничья.
     * У оставшихся пешек этого цвета нет ни одного возможного хода
     */
    fun stalemate(pawn: Pawn): Boolean {
        for (x in 0..7) {
            for (y in 0..7) {
                if (table[x][y] == pawn.toString()) {
                    val from = Cell(x, y)
                    for (a in -1..1)
                        for (b in -1..1) {
                            val to = Cell(adjust(x + a), adjust(y + b))
                            if (isValidMove(from, to, pawn) || isValidCapture(from, to, pawn)) return false
                        }
                }
            }
        }
        return true
    }

    /**
     * Выравнивание координаты в пределах доски
     */
    private fun adjust(pos: Int) = minOf(maxOf(pos, 0), 7)

    /**
     * Клетка доски
     */
    private data class Cell(val x: Int, val y: Int)

    /**
     * Перевод команды в координаты доски
     */
    private fun translate(data: String): Array<Cell> {
        val command = data.toCharArray()
        val x1 = Integer.parseInt(command[1].toString()) - 1
        val y1 = command[0].code - 97
        val x2 = Integer.parseInt(command[3].toString()) - 1
        val y2 = command[2].code - 97
        return arrayOf(Cell(x1, y1), Cell(x2, y2))
    }

    /**
     * Определение длины шага между клетками
     */
    private fun step(from: Int, to: Int, pawn: Pawn) = if (pawn == Pawn.WHITE) to - from else from - to

    /**
     * Сохранение ячеек, шаг в которые дает возможность взятия на проходе
     */
    private fun passant(from: Cell, to: Cell, pawn: Pawn) {
        if (step(from.x, to.x, pawn) == 2 && from.x == pawn.initRow) {
            passant[pawn] = Cell(to.x + pawn.passantDiff, to.y)
        } else passant.remove(pawn)
    }

    /**
     * Проверка хода на взятие
     */
    private fun isValidCapture(from: Cell, to: Cell, pawn: Pawn): Boolean {
        if (table[to.x][to.y] == pawn.toString()) return false
        if (kotlin.math.abs(from.y - to.y) != 1) return false

        val dest = table[to.x][to.y]
        return step(from.x, to.x, pawn) == 1 &&
                (dest == pawn.opposite.toString() || (dest == " " && Cell(to.x, to.y) == passant[pawn.opposite]))
    }

    /**
     * Проверка хода на шаг
     */
    private fun isValidMove(from: Cell, to: Cell, pawn: Pawn): Boolean {
        if (from.y != to.y || table[to.x][to.y] != " ") return false

        val step = step(from.x, to.x, pawn)
        return step == 1 || (from.x == pawn.initRow && step in (1..2))
    }

    /**
     * Проверка возможности хода
     * 1) ввод по формату
     * 2) в начальной клетке есть пешка
     * 3) команда по правилам (ход или взятие)
     */
    fun check(command: String, pawn: Pawn): Boolean {
        if (!command.matches(pattern)) {
            println("Invalid input")
            return false
        }

        val (cFrom, cTo) = translate(command)
        if (table[cFrom.x][cFrom.y] != pawn.toString()) {
            println("No ${pawn.color} pawn at ${command.substring(0, 2)}")
            return false
        }

        if (cFrom == cTo) {
            println("Invalid input")
            return false
        }

        return if (isValidMove(cFrom, cTo, pawn) || isValidCapture(cFrom, cTo, pawn)) true
        else {
            println("Invalid input")
            false
        }
    }

    /**
     * Делает ход по введённой команде
     */
    fun move(command: String, pawn: Pawn) {
        val (cFrom, cTo) = translate(command)
        passant(cFrom, cTo, pawn)
        table[cFrom.x][cFrom.y] = " "
        if (table[cTo.x][cTo.y] == " ") {
            table[cTo.x + pawn.passantDiff][cTo.y] = " "
        }
        table[cTo.x][cTo.y] = pawn.toString()
    }

    /**
     * Рисует текущее состояние доски
     */
    fun draw() {
        drawLine()
        for (i in 8 downTo 1) {
            drawCells(rank = i, row = table[i - 1])
            drawLine()
        }
        drawFiles()
        println()
    }

    private fun drawLine() = println("  +---+---+---+---+---+---+---+---+")
    private fun drawFiles() = println("    a   b   c   d   e   f   g   h")
    private fun drawCells(rank: Int, row: List<String>) {
        print("$rank |")
        for (i in 0 until 8) print(" ${row[i]} |")
        println()
    }
}

/**
 * Пешка
 * @property color - цвет
 * @property opposite - цвет противника
 * @property initRow - номер строки для начальной расстановки
 * @property passantDiff - смещение по оси Х для взятия на проходе
 */
enum class Pawn(val color: String, val initRow: Int) {
    WHITE("white", 1),
    BLACK("black", 6);

    val passantDiff: Int get() = if (this == WHITE) -1 else 1
    val opposite: Pawn get() = if (this == WHITE) BLACK else WHITE

    override fun toString() = color.first().uppercase()
}

/**
 * Игрок
 * @property name - имя игрока
 * @property pawn - пешки игрока
 */
data class Player(val name: String, val pawn: Pawn) {
    fun move(): String {
        println("$name's turn:")
        return readLine()!!
    }
}

/**
 * Ввод данных игрока
 */
fun inputPlayer(pawn: Pawn): Player {
    val order = if (pawn == Pawn.WHITE) "First" else "Second"
    println("$order Player's name:")
    val name = readLine()!!
    return Player(name, pawn)
}

/**
 * Ход игрока (повтор ввода, пока не введена корректная команда)
 */
fun doMove(player: Player, table: Table): String {
    var move: String?
    do {
        move = player.move()
        if (move == "exit") return move
        if (table.check(move, player.pawn)) table.move(move, player.pawn)
        else move = null

    } while (move == null)
    return move
}

/**
 * Проверка условий окончания игры
 */
fun gameFinished(player: Player, table: Table): Boolean {
    if (table.gameWin()) {
        val color = player.pawn.color.replaceFirstChar { it.uppercase() }
        println("$color Wins!")
        return true
    }
    if (table.stalemate(player.pawn.opposite)) {
        println("Stalemate!")
        return true
    }
    return false
}

fun gameOver(move: String) = (move == "exit")

fun main() {
    println("Pawns-Only Chess")
    val playerWhite = inputPlayer(Pawn.WHITE)
    val playerBlack = inputPlayer(Pawn.BLACK)
    val table = Table()
    table.draw()

    while (true) {
        val move1 = doMove(playerWhite, table)
        if (gameOver(move1)) break
        table.draw()
        if (gameFinished(playerWhite, table)) break

        val move2 = doMove(playerBlack, table)
        if (gameOver(move2)) break
        table.draw()
        if (gameFinished(playerBlack, table)) break
    }
    println("Bye!")
}
