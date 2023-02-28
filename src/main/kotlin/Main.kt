fun Item.tokenize(): List<Item> {
    val tokens = mutableListOf<Item>()

    var start = 0

    val str = this.string()

    str.forEachIndexed { index, c ->
        if (c.isWhitespace()) {
            if (index > start) {
                tokens.add(Item.Slice(IntRange(start, index-1), this))
            }
            start = index+1
        }
    }

    if (start < str.length) {
        tokens.add(Item.Slice(IntRange(start, str.length-1), this))
    }

    return tokens
}

fun Item.normalize(): Item {
    return Item.Modification(this.string().lowercase(), this)
}

fun Item.simplify(): Item {
    val res = this.string().mapNotNull { c ->
        if (c.isDigit() || c.isLetter()) {
            c
        }
        else if (c.isWhitespace()) {
            " "
        } else {
            null
        }
    }
    return Item.Modification(res.joinToString(""), this)
}

fun Item.lower(): Item {
    val str = this.string()

    return Item.Modification(str.lowercase(), this)
}

fun Item.similar(value: String): Int {
    var sim = 0

    val str = this.string()
    val min = minOf(str.length, value.length)

    for (i in 0 until min) {
        if (str[i] == value[i]) {
            sim++
        }
        else {
            break
        }
    }

    return sim
}

sealed class Item {
    class Root(val value: String): Item() {
        override fun string(): String {
            return value
        }

        override val length: Int
            get() = value.length
    }

    class Modification(val value: String, val source: Item): Item() {
        override fun string(): String {
            return value
        }

        override val length: Int
            get() = value.length
    }

    class Slice(val range: IntRange, val source: Item): Item() {
        override fun string(): String {
            return source.string().slice(range)
        }

        override val length: Int
            get() = range.last+1-range.first

        override fun slice(range: IntRange): Item {
            val newRange = IntRange(range.first+this.range.first, this.range.first+range.last)
            return source.slice(newRange)
        }
    }

    abstract fun string(): String

    abstract val length: Int

    open fun slice(range: IntRange): Item {
        return Slice(range, this)
    }

    val root: String
        get() {
            return when (this) {
                is Root -> this.value
                is Modification -> this.source.root
                is Slice -> this.source.root
            }
        }

    val rootItem: Root
        get() {
            return when (this) {
                is Root -> this
                is Modification -> this.source.rootItem
                is Slice -> this.source.rootItem
            }
        }

    override fun hashCode(): Int {
        return string().hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return this.hashCode() == other.hashCode()
    }

    override fun toString(): String {
        return this.string()
    }
}

interface INode {
    val children: MutableList<Node>

    fun query(q: Item): List<Node> {
        val fullMatch = mutableListOf<Node>()
        val partialMatch = mutableListOf<Node>()

        for (c in children) {
            val sim = c.value.similar(q.string())

            if (sim == c.value.length) {
                if (sim == q.length) {
                    fullMatch.add(c)
                }

                val e = c.query(q.slice(sim until q.length))
                fullMatch.addAll(e)
            }
            else if (sim == q.length) {
                fullMatch.add(c)
            }
        }

        return fullMatch.ifEmpty { partialMatch }
    }

    fun insert(value: Item) {
        //             index, amount
        var maxSim: Pair<Int, Int>? = null

        children.forEachIndexed { index, node ->
            val sim = node.value.similar(value.string())
            if (sim > (maxSim?.second ?: 0)) {
                maxSim = Pair(index, sim)
            }
        }

        if (maxSim == null) {
            // println("a $value")
            this.children.add(Node(value))
        }
        else {
            // println("b $value")
            val node = children[maxSim!!.first]

            node.slice(maxSim!!.second)

            node.insert(value.slice(maxSim!!.second until  value.length))
        }
    }
}

data class RootNode(override val children: MutableList<Node> = mutableListOf()): INode

data class Node(var value: Item, override var children: MutableList<Node> = mutableListOf()): INode {
    fun slice(amount: Int) {
        if (children.size == 0) {
            val old = value
            val new = value.slice(0 until amount)
            value = new
            children.add(Node(old.slice(amount until old.length)))
        }
        else if (amount-1 != value.length) {
            val newNode = Node(Item.Slice(amount until value.length, value), children)
            value = Item.Slice(0 until amount, value)
            children = mutableListOf(newNode)
        }
    }
}

fun prepareStr(str: String): Set<Item> {
    val result = mutableSetOf<Item>()
    val root = Item.Root(str)
    result.add(root)

    val normalized = root.normalize()
    result.add(normalized)

    val simplified = root.simplify()
    result.add(simplified)

    val simplifiedNormalized = normalized.simplify()
    result.add(simplifiedNormalized)

    result.addAll(root.tokenize())
    result.addAll(normalized.tokenize())
    result.addAll(simplified.tokenize())
    result.addAll(simplifiedNormalized.tokenize())

    return result
}

fun countItems(items: List<Item>): List<Item> {
    val map = mutableMapOf<Item, Int>()

    for (item in items) {
        map.compute(item.rootItem) { it, value ->
            (value ?: 0) + item.length
        }
    }

    return map.map { it }.sortedByDescending { it.value }.map { it.key }
}

fun main(args: Array<String>) {
    val root = RootNode()

    getNames("/usr/share/applications").forEach {
        val res = prepareStr(it)
        res.forEach { root.insert(it) }
    }

    while (true) {
        val l = readln()
        val res = prepareStr(l).flatMap {
            root.query(it)
        }
        countItems(res.map { it.value }).forEach {
            println(it.rootItem.string())
        }
    }
}