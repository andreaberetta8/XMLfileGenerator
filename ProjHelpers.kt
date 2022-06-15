import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.border.CompoundBorder


class EntityRepresentation(val ent: XMLElement,val contr:Controller) : JPanel() {

    val component=this
    var obs:XMlListener?=null

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        g.font = Font("Arial", Font.BOLD, 16)
        g.drawString(ent.name, 10, 20)
    }


    fun getAttributeValue(key:String):JTextField?{
        var flag=false
        for (c in components)
            if (c is JPanel)
                for (l in c.components) {
                    if (l is JLabel && l.text==key)
                        flag=true
                    if (l is JTextField && flag)
                        return l
                }
        return null
    }
    init {

        layout = GridLayout(0, 1)
        border = CompoundBorder(
            BorderFactory.createEmptyBorder(30, 10, 10, 10),
            BorderFactory.createLineBorder(Color.BLACK, 2, true)
        )
        val observerObject=object:XMlListener{

            override fun rename(newName: String) {
                repaint()
            }

            override fun setAttribute(key:String,value:String) {
                val panel = JPanel()
                val chiave = JLabel(key)
                val valore = JTextField(value)
                add(panel)
                panel.add(chiave)
                panel.add(valore)
                revalidate()
                return

            }

            override fun editAttribute(key:String,newValue:String) {
                val valueField=getAttributeValue(key)
                if (valueField!=null) {
                    valueField.text = newValue
                    revalidate()
                }
            }

            override fun cancelAttribute(key: String) {
                for (c in component.components)
                    if (c is JPanel)
                        for (l in c.components)
                            if (l is JLabel && l.text == key) {
                                component.remove(c)
                                revalidate()
                                repaint()
                            }
            }

            override fun drawEntityAndChildren(el:XMLElement) {

                val cmp=EntityRepresentation(el,contr)
                add(cmp)
                if (el.atb.isNotEmpty())
                    el.atb.forEach{
                        cmp.obs?.setAttribute(it.key,it.value)
                    }
                if (el is Entity){
                    if (el.children.isNotEmpty())
                        el.children.forEach {
                            cmp.obs?.drawEntityAndChildren(it)

                        }

                }else{
                    val te=el as TextEntity
                    cmp.add(JLabel(te.text))
                }
                revalidate()
            }

            override fun cancelEntity(el:XMLElement) {

                parent.remove(component)
                revalidate()
                repaint()
                el.removeObserver(this)
            }

        }
        ent.addObserver(observerObject)
        obs=observerObject
        createPopupMenu()
    }

    private fun createPopupMenu() {
        val popupmenu = JPopupMenu("Actions")

        val a = JMenuItem("Add Entity")
        a.addActionListener {
            if ( ent is Entity) {
                val text = JOptionPane.showInputDialog("name of the entity")
                contr.addEntity(ent,text)
            }else
                JOptionPane.showMessageDialog(null, "This is an entity with text,it can't have a child");
        }
        popupmenu.add(a)

        val b = JMenuItem("Add Entity with text")
        b.addActionListener {
            if (ent is Entity){
                val name = JOptionPane.showInputDialog("name of the entity")
                val text = JOptionPane.showInputDialog("text of the entity")
                contr.addTextEntity(ent,name,text)
            }else{
                JOptionPane.showMessageDialog(null, "This is an entity with text,it can't have a child");
            }
        }
        popupmenu.add(b)

        val c = JMenuItem("Add attribute")

        c.addActionListener {
            val name = JOptionPane.showInputDialog("insert name of the attribute")
            val v = JOptionPane.showInputDialog("insert value of the attribute")
            contr.addNewAttribute(ent,name, v)
        }
        popupmenu.add(c)

        val c1 = JMenuItem("remove attribute")
        c1.addActionListener {
            val text = JOptionPane.showInputDialog("name of the attribute you want to delete")
            contr.deleteAttribute(ent,text)
        }
        popupmenu.add(c1)

        val d = JMenuItem("rename entity")
        d.addActionListener {
            val text = JOptionPane.showInputDialog("text")
            contr.renameEntity(ent,text)
        }
        popupmenu.add(d)

        val f = JMenuItem("edit attribute")
        f.addActionListener {
            val key = JOptionPane.showInputDialog("name of the attribute you want to edit")
            val v1 = JOptionPane.showInputDialog("insert new value")
            contr.modifyAttribute(ent,key,v1)
        }
        popupmenu.add(f)

        val g = JMenuItem("delete entity")
        g.addActionListener {
            contr.deleteEntity(ent)
        }
        popupmenu.add(g)

        val h = JMenuItem("undo")
        h.addActionListener {
            contr.goBack()
        }
        popupmenu.add(h)

        val h1 = JMenuItem("redo")
        h.addActionListener {
            contr.goAhead()
        }
        popupmenu.add(h1)

        val o = JMenuItem("generate XMl file")
        o.addActionListener {
            val text = JOptionPane.showInputDialog("insert the name of the file")
            contr.generateXMlFile(ent, "$text.txt")

        }
        popupmenu.add(o)
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (SwingUtilities.isRightMouseButton(e))
                    popupmenu.show(this@EntityRepresentation, e.x, e.y)
            }
        })
    }
}


class View(model:XMLElement, contr:Controller) : JFrame("title") {

    init {
        defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        size = Dimension(300, 300)

        this.add(EntityRepresentation(model,contr))
    }

    fun open() {
        isVisible = true
    }

}

/*running UI*/
fun main() {
    val e = Entity("root",null)
    val contr=Controller()
    val w = View(e,contr)

    w.open()
}

