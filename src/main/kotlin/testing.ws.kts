
println(getNames("/usr/share/applications"))

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
val tokens = Item.Root("gidiogdog    fdgudugjd uoijgduoijsdguj      dsogjodsgjds dsogjodsgjds").tokenize()

println(tokens.map { it.similar("giddddddddddddddddd") })

tokens[1].slice(2..5).string()