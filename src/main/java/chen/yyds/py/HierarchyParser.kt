package chen.yyds.py

import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.w3c.dom.ls.DOMImplementationLS
import org.w3c.dom.ls.LSSerializer
import java.io.File
import javax.swing.tree.DefaultMutableTreeNode
import javax.xml.parsers.DocumentBuilderFactory


object HierarchyParser {
    private val cacheListNodes = HashMap<NodeObject, DefaultMutableTreeNode>()

    fun innerXml(node: Node): String {
        val lsImpl: DOMImplementationLS =
            node.ownerDocument.implementation.getFeature("LS", "3.0") as DOMImplementationLS
        val lsSerializer: LSSerializer = lsImpl.createLSSerializer()
        return lsSerializer.writeToString(node).replace("<?xml version=\"1.0\" encoding=\"UTF-16\"?>", "")
    }

    fun parseNode(root:Node, treeNode:DefaultMutableTreeNode, nodeMap: HashMap<NodeObject, DefaultMutableTreeNode>) {
        val ele = root as Element
        /**
        var parent = ele.parentNode
        var parentCount = 0
        while (parent != null && parent.nodeType == Node.ELEMENT_NODE) {
            parentCount++
            parent = parent.parentNode
        }
        */
        // val childCount = ele.childNodes.length

        val nodeObject = NodeObject(

            ele.getAttribute( "index").toInt(),

            ele.getAttribute("text"),
            ele.getAttribute("resource-id"),
            ele.getAttribute("class"),
            ele.getAttribute("package"),
            ele.getAttribute("content-desc"),

            ele.getAttribute("checkable").toBoolean(),
            ele.getAttribute("checked").toBoolean(),
            ele.getAttribute("clickable").toBoolean(),
            ele.getAttribute("enabled").toBoolean(),
            ele.getAttribute("focusable").toBoolean(),

            ele.getAttribute("focused").toBoolean(),
            ele.getAttribute("scrollable").toBoolean(),
            ele.getAttribute("long-clickable").toBoolean(),
            ele.getAttribute("password").toBoolean(),
            ele.getAttribute("selected").toBoolean(),
            ele.getAttribute("visible-to-user").toBoolean(),
            ele.getAttribute("bounds"),
            try {
                ele.getAttribute( "parent-count").toInt()
            } catch (e:Exception) { 0 },
            try {
                ele.getAttribute( "child--count").toInt()
            } catch (e:Exception) { 0 },
            innerXml(root)
        )

        // System.out.println("->$nodeObject")
        val newNode = DefaultMutableTreeNode(nodeObject)
        treeNode.add(newNode)
        nodeMap[nodeObject] = newNode
        // 解析所有子
        for (child_index in 0 until ele.childNodes.length) {
            val child = ele.childNodes.item(child_index)
            if (child.nodeType == Node.ELEMENT_NODE) {
                parseNode(child, newNode, nodeMap)
            }
        }
    }

    fun parse(path:String, treeNode:DefaultMutableTreeNode):DefaultMutableTreeNode {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder();
        val doc = builder.parse(File(path))
        var rootNode:Node = doc.documentElement.firstChild
        while (rootNode.nodeType != Node.ELEMENT_NODE) {
            rootNode = rootNode.nextSibling
            System.out.println("Next Node...")
        }
        cacheListNodes.clear()
        System.out.println("Nodetype: " + rootNode.nodeType)
        parseNode(rootNode, treeNode, cacheListNodes)
        return treeNode
    }

    fun clearCacheList() {
        cacheListNodes.clear()
    }

    fun allInRectExceptLayout(x:Int, y:Int):Map<NodeObject, DefaultMutableTreeNode> {
        if (cacheListNodes.isEmpty()) return emptyMap<NodeObject, DefaultMutableTreeNode>()

        return cacheListNodes.filter {
            x > it.key.bounds.p1x && x < it.key.bounds.p2x && y > it.key.bounds.p1y && y < it.key.bounds.p2y
        }
    }
}
