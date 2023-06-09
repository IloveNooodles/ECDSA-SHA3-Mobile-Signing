package ext.keccak

import java.math.BigInteger
import kotlin.math.min

typealias Block = List<UByte>
typealias MutableBlock = MutableList<UByte>

fun Block.Hexdigest(): String = joinToString(separator = "") {
        eachByte -> "%02x".format(eachByte.toByte())
}

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

    fun hash(bytes: ByteArray): String {
        initBlock()
        inputBytes = bytes.asUByteArray().toMutableList();

        val inputBlocks = inputBytes.chunked(rate);

        val blockSize = absorbPhase(inputBlocks);

        /* pad the input bytes to get M % r = 0*/
        paddingPhase(blockSize);

        /* do the squeezing phase */
        val digestBlock = squeezingPhase(blockSize);

        /* Convert into hexdigest */
        val hexDigest = digestBlock.Hexdigest()

        return hexDigest
    }

    private fun absorbPhase(inputBlocks: List<Block>): Int{
        /* Do the absorption for every blocks */
        var blockSize = 0

        for (inputBlock in inputBlocks) {
            blockSize = min(inputBlock.size, rate);
            /* xor the rate  */
            for (i in inputBlock.indices) {
                state[i] = state[i] xor inputBlock[i]
            }

            /* do the round function */
            if (inputBlock.size == rate) {
                roundFunction();
                blockSize = 0;
            }
        }

        return blockSize
    }

    private fun paddingPhase(blockSize: Int){
        val xorSize: UByte = 0x80u

        state[blockSize] = state[blockSize] xor delimitedSuffix.toUByte();
        if(((delimitedSuffix and xorSize.toInt()) != 0) and (blockSize == rate - 1)){
            roundFunction()
        }

        state[rate - 1] = state[rate - 1] xor xorSize
        roundFunction()
    }

    private fun squeezingPhase(blockSize: Int): MutableBlock{
        val result = mutableListOf<UByte>();
        var mutableBlockSize = blockSize;
        var outputLength = outputSize
        while(outputLength > 0){
            mutableBlockSize = min(outputLength, rate);
            result.addAll(state.subList(0, mutableBlockSize))

            outputLength -= mutableBlockSize;
            if(outputLength > 0){
                roundFunction();
            }
        }

        return result;
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
                    var bigIntMatrixElement = BigInteger.valueOf(stateMatrix[0][0].toLong())
                    bigIntMatrixElement = bigIntMatrixElement.xor(BigInteger.ONE shl offsetShift)
                    stateMatrix[0][0] = bigIntMatrixElement.toLong().toULong()
                }
            }
        }

        for (x in 0..4){
            for(y in 0..4){
                val data = convertLittleEndian(stateMatrix[x][y])
                var count = 0
                for(i in (8 * (x + 5 * y)) until (8 * (x + 5 * y) + 8)){
                    state[i] = data[count];
                    count++;
                }
            }
        }
    }

    private fun convertLittleEndian(a: ULong ): MutableBlock {
        return (0..7).map { ((a shr (8 * it)) and 0xffu).toUByte() }.toMutableList()
    }

    private fun getStateAsMatrix(): MutableList<MutableList<ULong>> {
        var cell = 0uL
        val flatStateMatrix = mutableListOf<ULong>()

        for (i in state.indices) {
            val byte = state[i]
            cell += byte.toULong() shl (8 * (i % 8))

            if (i % 8 == 7) {
                flatStateMatrix.add(cell)
                cell = 0uL
            }
        }


        return flatStateMatrix.chunked(5) { list ->
            list.toMutableList()
        }.toMutableList().apply {
            transpose()
        }
    }

    private fun initBlock() {
        val zero: UByte = 0u
        block = (0 until blockSize).map { zero }.toMutableList()
    }
}

fun rot(a: ULong, n: Int): ULong {
    val n = 64 - n
    val shift = n % 64
    val right = a shr shift
    val left = a shl (64 - shift)
    return left or right
}

fun MutableList<MutableList<ULong>>.transpose() {
    for (i in indices) {
        for (j in 0 until i) {
            val k = this[i][j]
            this[i][j] = this[j][i]
            this[j][i] = k
        }
    }
}
