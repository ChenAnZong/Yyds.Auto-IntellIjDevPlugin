package chen.yyds.py

import java.awt.Point

data class Bounds(val boundsString: String) {
    private val s = boundsString.split("[", "]", ",").filter { it.isNotEmpty() }
    val p1x = s[0].toInt() // 左上角x
    val p1y = s[1].toInt() // 左上角y
    val p2x = s[2].toInt()
    val p2y = s[3].toInt()

    val width by lazy { p2x - p1x }
    val height by lazy { p2y - p1y }

    val centerPos: Point by lazy {
        val rx = p1x + ((width*0.3).toInt()..(width*0.7).toInt()).random()
        val ry = p1y + ((height*0.3).toInt()..(height*0.7).toInt()).random()
        Point(rx, ry)
    }

    public fun isWidthHeightEqual(w:Int, h:Int):Boolean = w == width && h == height

    override fun toString(): String {
        return "$p1x,$p1y $p2x,$p2y $width,$height"
    }
}

data class NodeObject(
    val index: Int,

    val text: String,
    val id: String,
    val cls: String,
    val pkg: String,
    val desc: String,

    val isCheckable: Boolean,
    val isChecked: Boolean,
    val isClickAble: Boolean,
    val isEnable: Boolean,
    val isFocusable: Boolean,

    val isFocused: Boolean,
    val isScrollable: Boolean,
    val isLongClickable: Boolean,
    val isPassword: Boolean,
    val isSelected: Boolean,

    val isVisible: Boolean,
    val boundsString: String,
    val parentCount: Int, // xml depth
    val childCount: Int,
    val innerXml: String
) {

    override fun toString(): String {
        val idText = if (id.substringAfterLast("/").isNotEmpty()) {
            "id=" + id.substringAfterLast("/")
        } else {
            ""
        }

        val descText = if (desc.isNotEmpty() && text.isNotEmpty()) {
            "desc=$desc"
        } else {
            ""
        }

        val textText = if (text.isNotEmpty()) {
            "text=$text"
        } else {
            ""
        }

        val clickText = if (isClickAble) {
            "可点击"
        } else {
            ""
        }

        val indexText = if (parentCount > 0) {
            "${index}_"
        } else {
            ""
        }

        return "$indexText${bounds.width}x${bounds.height} $clickText${cls.substringAfterLast(".")} $idText $descText $textText"
    }

    val bounds by lazy { Bounds(boundsString) }
}
