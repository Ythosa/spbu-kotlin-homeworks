package homeworks.homework3.avl

import kotlin.math.pow

fun Int.pow(n: Int) = this.toFloat().pow(n).toInt()

fun <K : Comparable<K>, V> avlTreeOf(vararg pairs: Pair<K, V>): MutableMap<K, V> =
    AVLTree<K, V>().apply { putAll(pairs) }

class AVLTree<K : Comparable<K>, V> : MutableMap<K, V> {
    private var head: AVLNode<K, V>? = null

    override var size: Int = 0
        private set
    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = mutableSetOf<MutableMap.MutableEntry<K, V>>().apply {
            head?.traverse { entire -> this.add(entire) }
        }
    override val keys: MutableSet<K>
        get() = mutableSetOf<K>().apply {
            head?.traverse { entire -> this.add(entire.key) }
        }
    override val values: MutableCollection<V>
        get() = mutableListOf<V>().apply {
            head?.traverse { entire -> this.add(entire.value) }
        }

    override fun containsKey(key: K) = get(key) != null

    override fun containsValue(value: V): Boolean = value in values

    override fun get(key: K): V? {
        var currentNode = head

        while (currentNode != null) {
            currentNode = when {
                key < currentNode.key -> currentNode.leftChild
                key > currentNode.key -> currentNode.rightChild
                else -> return currentNode.value
            }
        }

        return null
    }

    override fun isEmpty(): Boolean = size == 0

    override fun clear() {
        head = null
        size = 0
    }

    override fun put(key: K, value: V): V? {
        head = SubtreeWorker.nodePut(head, key, value)
        size++

        return value
    }

    override fun putAll(from: Map<out K, V>) = from.entries.forEach { put(it.key, it.value) }

    override fun remove(key: K): V? = SubtreeWorker.nodeRemove(head, key).let {
        head = it.first
        size--

        it.second?.value
    }

    override fun toString(): String = Serializer.toString(this)

    private object SubtreeWorker {
        private const val LEFT_SUBTREE_EXCESS = 2
        private const val RIGHT_SUBTREE_EXCESS = -2

        fun <K : Comparable<K>, V> nodePut(node: AVLNode<K, V>?, key: K, value: V): AVLNode<K, V> = node?.let {
            when {
                key < node.key -> balanced(node.apply { leftChild = nodePut(leftChild, key, value) }).updateHeight()
                key > node.key -> balanced(node.apply { rightChild = nodePut(rightChild, key, value) }).updateHeight()
                else -> node.apply { this.value = value }
            }
        } ?: AVLNode(key, value)

        fun <K : Comparable<K>, V> nodeRemove(
            node: AVLNode<K, V>?,
            key: K
        ): Pair<AVLNode<K, V>?, AVLNode<K, V>?> = node?.let {
            when {
                key < node.key -> {
                    val (newRoot, removedNode) = nodeRemove(node.leftChild, key)
                    node.leftChild = newRoot
                    Pair(balanced(node).updateHeight(), removedNode)
                }
                key > node.key -> {
                    val (newRoot, removedNode) = nodeRemove(node.rightChild, key)
                    node.rightChild = newRoot
                    Pair(balanced(node).updateHeight(), removedNode)
                }
                else -> when {
                    node.leftChild == null && node.rightChild == null -> Pair(null, node)
                    node.leftChild == null -> Pair(node.rightChild, node)
                    node.rightChild == null -> Pair(node.leftChild, node)
                    else -> {
                        val minRightChild = node.rightChild!!.minChild
                        node.key = minRightChild.key
                        node.value = minRightChild.value

                        val (newRoot, removedNode) = nodeRemove(node.rightChild, node.key)
                        node.rightChild = newRoot

                        Pair(balanced(node).updateHeight(), removedNode)
                    }
                }
            }
        } ?: Pair(null, null)

        fun <K : Comparable<K>, V> balanced(node: AVLNode<K, V>): AVLNode<K, V> = when (node.balanceFactor) {
            LEFT_SUBTREE_EXCESS -> if (node.leftChild?.balanceFactor == -1) leftRightRotate(node) else rightRotate(node)
            RIGHT_SUBTREE_EXCESS -> if (node.rightChild?.balanceFactor == 1) rightLeftRotate(node) else leftRotate(node)
            else -> node
        }

        fun <K : Comparable<K>, V> leftRotate(node: AVLNode<K, V>): AVLNode<K, V> {
            val pivot = node.rightChild ?: return node

            node.rightChild = pivot.leftChild
            pivot.leftChild = node

            node.updateHeight()
            pivot.updateHeight()

            return pivot
        }

        fun <K : Comparable<K>, V> rightRotate(node: AVLNode<K, V>): AVLNode<K, V> {
            val pivot = node.leftChild ?: return node

            node.leftChild = pivot.rightChild
            pivot.rightChild = node

            node.updateHeight()
            pivot.updateHeight()

            return pivot
        }

        fun <K : Comparable<K>, V> rightLeftRotate(node: AVLNode<K, V>): AVLNode<K, V> {
            val rightChild = node.rightChild ?: return node

            node.rightChild = rightRotate(rightChild)

            return leftRotate(node)
        }

        fun <K : Comparable<K>, V> leftRightRotate(node: AVLNode<K, V>): AVLNode<K, V> {
            val leftChild = node.leftChild ?: return node

            node.leftChild = leftRotate(leftChild)

            return rightRotate(node)
        }
    }

    private object Serializer {
        fun <K : Comparable<K>, V> toString(tree: AVLTree<K, V>): String {
            tree.head ?: return "empty tree"

            val sb = StringBuilder()
            val heap = toHeap(tree)
            val height = tree.head!!.height

            val baseMultiplier = tree.head.toString().length
            var leftSpacesCount = 2.pow(height) - 1
            var betweenSpacesCount = 0

            for (level in 0..height) {
                sb.append(" ".repeat(baseMultiplier * leftSpacesCount))
                for (i in 2.pow(level) - 1 until 2.pow(level + 1) - 1) {
                    if (heap[i] != null)
                        sb.append(heap[i].toString())
                    else
                        sb.append(" ".repeat(baseMultiplier))
                    sb.append(" ".repeat(baseMultiplier * betweenSpacesCount))
                }
                sb.append("\n")

                betweenSpacesCount = leftSpacesCount
                leftSpacesCount -= 2.pow(height - level - 1)
            }

            return sb.toString()
        }

        private fun <K : Comparable<K>, V> toHeap(tree: AVLTree<K, V>): Array<AVLNode<K, V>?> {
            tree.head ?: return emptyArray()

            val heap = arrayOfNulls<AVLNode<K, V>?>(2.pow(tree.head!!.height + 1))
            val queue = ArrayDeque<Pair<AVLNode<K, V>, Int>>().apply { add(Pair(tree.head!!, 0)) }

            while (queue.isNotEmpty()) {
                val (node, index) = queue.removeFirst()
                heap[index] = node

                if (node.leftChild != null)
                    queue.add(Pair(node.leftChild!!, 2 * index + 1))

                if (node.rightChild != null)
                    queue.add(Pair(node.rightChild!!, 2 * index + 2))
            }

            return heap
        }
    }
}
