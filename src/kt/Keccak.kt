import java.math.BigInteger

typealias Block = List<UByte>
typealias MutableBlock = MutableList<UByte>
typealias LongLong = BigInteger
var Zero: UByte = 0u

class Keccak private constructor(
    private val outputSize: Int,    // d, in bytes
    private val rate: Int,          // r, in bytes
    private val capacity: Int,      // c, in bytes
    private val delimitedSuffix: Int
) {

    /* Static configuration of the keccak */
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

        /* 5 x 5 x 8 blocks */
        private fun initialState(): MutableList<UByte> {
            val zero: UByte = 0u
            return (0 until 200).map { zero }.toMutableList()
        }
    }

    private val blockSize = rate + capacity /* b = rate + capacity */
    private val state = initialState() /* Empty b */
    private lateinit var inputBytes: MutableBlock
    private lateinit var block: MutableBlock

    fun hash(bytes: Block) {
        initBlock()
        inputBytes = bytes.toMutableList()

        /* pad the input bytes */
        pad(inputBytes, rate);
        val inputBlocks = inputBytes.chunked(rate);

        absorb(inputBlocks)

//        TODO("Add padding")
//        TODO("Implement squeezing phase")

    }

    private fun absorb(inputBlocks: List<Block>){
        /* Do the absorption for every blocks */
        for (inputBlock in inputBlocks) {
            /* xor the rate  */
            for (i in inputBlock.indices) {
                state[i] = state[i] xor inputBlock[i]
            }

            /* do the round function */
            if (inputBlock.size == rate) {
                roundFunction();
            }
        }
    }

    /**
     * @see https://keccak.team/keccak_specs_summary.html
     */
    private fun roundFunction() {
        var stateMatrix = getStateAsMatrix()
        var RC = 1

        /* For each round do the permutation */
        for (round in 0 until 24) {

            /**
             * ## θ step
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
             * ## ι step
             */
            for (i in 0..6){
                RC = ((RC shl 1) xor ((RC shr 7) * 0x71)) % 256
                val offsetShift = (1 shl i) - 1
                if (RC and 2 != 0){
                    stateMatrix[0][0] = stateMatrix[0][0] xor ((1 shl offsetShift).toLong())
                }
            }
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
}

fun pad(input: MutableBlock, many: Int){
    val currentSize = input.size
    val zero: UByte = 0u
    val paddedBlock = (0 until many - currentSize).map { zero }.toMutableList();
    input.addAll(paddedBlock);
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


fun main(){
    val bit = 8;
//    val blockSize = 256;
//    val digest = blockSize / bit;
    val rate = 1088 / bit;
//    val capacity = blockSize / bit;
    val k = Keccak._256()

//    rot(3)
//    k.hash(c.toList());
}
