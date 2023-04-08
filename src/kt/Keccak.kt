class Keccak private constructor(
    private val outputSize: Int,    // d, in bytes
    private val rate: Int,          // r, in bytes
    private val capacity: Int,      // c, in bytes
    private val delimitedSuffix: Int
) {

    companion object {
        fun _224(): Keccak {
            return Keccak(
                224 / 8,
                1152 / 8,
                448 / 8,
                0x06
            )
        }

        fun _256(): Keccak {
            return Keccak(
                256 / 8,
                1088 / 8,
                512 / 8,
                0x06
            )
        }

        fun _384(): Keccak {
            return Keccak(
                384 / 8,
                832 / 8,
                768 / 8,
                0x06
            )
        }

        fun _512(): Keccak {
            return Keccak(
                512 / 8,
                576 / 8,
                1024 / 8,
                0x06
            )
        }

        private fun initialState(): MutableList<UByte> {
            val zero: UByte = 0u
//            5x5 64 bit number
            return (0 until 200).map { zero }.toMutableList()
        }
    }

    private val blockSize = rate + capacity
    private val state = initialState()
    private lateinit var inputBytes: MutableBlock
    private lateinit var block: MutableBlock

    fun hash(bytes: Block) {
        initBlock()
        inputBytes = bytes.toMutableList()

        val inputBlocks = inputBytes.chunked(rate);

//        Absorption
        for (inputBlock in inputBlocks) {
            for (i in inputBlock.indices) {
                state[i] = state[i] xor inputBlock[i]
            }

            if (inputBlock.size == rate) {
                TODO("Permute state")
            }
        }

        TODO("Add padding")
        TODO("Implement squeezing phase")

    }


    private fun permute() {
        var stateMatrix = getStateAsMatrix()

        var R = 1
        for (round in 0 until 24) {

            /**
             * ## θ
            C = [lanes[x][0] ^ lanes[x][1] ^ lanes[x][2] ^ lanes[x][3] ^ lanes[x][4] for x in range(5)]
            D = [C[(x+4)%5] ^ ROL64(C[(x+1)%5], 1) for x in range(5)]
            lanes = [[lanes[x][y]^D[x] for y in range(5)] for x in range(5)]
             */
            val C = stateMatrix.map { row ->
                row[0] xor row[1] xor row[2] xor row[3] xor row[4]
            }
            val D = (0 until 5).map {
                C[(it + 4) % 5] xor rot(C[(it + 1) % 5], 1)
            }

            for (x in 0 until 5) {
                for (y in 0 until 5) {
                    stateMatrix[x][y] = stateMatrix[x][y] xor D[x]
                }
            }

            /**
             *  ## ρ and π
            (x, y) = (1, 0)
            current = lanes[x][y]
            for t in range(24):
            (x, y) = (y, (2*x+3*y)%5)
            (current, lanes[x][y]) = (lanes[x][y], ROL64(current, (t+1)*(t+2)//2))
             */

            var x = 1
            var y = 0
            var current = stateMatrix[x][y]
            for (t in 0 until 24) {
                val k = x
                x = y
                y = (2 * k + 3 * y) % 5

                val next = stateMatrix[x][y]
                stateMatrix[x][y] = rot(current, (t + 1) * (t + 2) / 2)
                current = next
            }

            /**
             * ## χ step
             */
            for (y in 0 until 5) {
                val T = (0 until 5).map { stateMatrix[it][y] }
                for (x in 0 until 5) {
                    stateMatrix[x][y] = T[x] xor (T[(x + 1) % 5].inv() and T[(x + 2) % 5])
                }
            }

            /**
            ## ι
            for j in range(7):
            R = ((R << 1) ^ ((R >> 7)*0x71)) % 256
            if (R & 2):
            lanes[0][0] = lanes[0][0] ^ (1 << ((1<<j)-1))
            return lanes
             */
            TODO("Implement ι step")
            /**
             * @see https://github.com/XKCP/XKCP/blob/master/Standalone/CompactFIPS202/Python/CompactFIPS202.py
             * @see https://keccak.team/keccak_specs_summary.html
             */
        }
    }

    private fun getStateAsMatrix(): MutableList<MutableList<Long>> {
        var cell = 0L
        val flatStateMatrix = mutableListOf<Long>()

        for (i in state.indices) {
            val byte = state[i]
            cell = cell shl 8
            cell += byte.toInt()

            if (i % 8 == 7) {
                flatStateMatrix.add(cell)
                cell = 0L
            }
        }

        return flatStateMatrix.chunked(5) { list ->
            list.toMutableList()
        }.toMutableList()
    }

    private fun initBlock() {
        val zero: UByte = 0u
        block = (0 until blockSize).map { zero }.toMutableList()
    }

    private fun absorb(inputBlock: Block) {
        for (i in 0 until rate) {
            block[i] = block[i] xor inputBlock[i]
        }
    }
}

fun rot(a: Long, n: Int): Long {
//    For example, shifting 20 bits would result in
//    these partitioning
//    [44][20]
    val shift = n % 64
//    The [44] bits part is then shifted right 20 bits
//    yielding `right`
    val right = a ushr shift
//    The [20] bits part is then shifted left 44 bits
//    yielding `left`
    val left = a shl (64 - shift)
    return left or right
}
typealias Block = List<UByte>
typealias MutableBlock = MutableList<UByte>