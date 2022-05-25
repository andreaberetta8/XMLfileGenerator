import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.*
import javax.swing.*
import javax.swing.border.CompoundBorder


class EntityRepresentation(val ent: XMLElement,val contr:Controller) : JPanel() {

    val component=this

    fun printStatus(){
        var e:Entity=ent as Entity
        while (e.parent!=null)
            e= e.parent!!
        e.printVisitor()
    }

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
        ent.addObserver(object:XMlListener{
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

            override fun drawEntity(el:XMLElement) {
                val cmp=EntityRepresentation(el,contr)
                add(cmp)
                cmp.printStatus()
                revalidate()
            }

            override fun drawTextEntity(el: XMLElement) {
                val cmp=EntityRepresentation(el,contr)
                add(cmp)
                val te=el as TextEntity
                cmp.add(JLabel(te.text))
                revalidate()
            }

            override fun cancelEntity() {
               val toDelete= parent.components[0]
               val toSave=if (components.isNotEmpty()) components else null
                if (toSave!=null) toSave.forEach{ parent.add(it)}
                parent.remove(toDelete)
                revalidate()
               repaint()
            }

        })
        createPopupMenu()
    }

    private fun createPopupMenu() {
        val popupmenu = JPopupMenu("Actions")

        val a = JMenuItem("Add Entity")
        a.addActionListener {
            if ( ent is Entity) {
                val text = JOptionPane.showInputDialog("name of the entity")
                contr.addEntity(ent,text, ent)
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
            if (ent is TextEntity){
                JOptionPane.showMessageDialog(null, "This is an entity with text, you can't add attributes");
            }else {
                val name = JOptionPane.showInputDialog("insert name of the attribute")
                val v = JOptionPane.showInputDialog("insert value of the attribute")
                contr.addNewAttribute(ent,name, v)
            }
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
            if (ent is TextEntity){
                JOptionPane.showMessageDialog(null, "This is an entity with text, it doesn't have attributes");
            }else {
                val key = JOptionPane.showInputDialog("name of the attribute you want to edit")
                val v1 = JOptionPane.showInputDialog("insert new value")
                contr.modifyAttribute(ent,key,v1)
            }
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

       addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (SwingUtilities.isRightMouseButton(e))
                    popupmenu.show(this@EntityRepresentation, e.x, e.y)
            }
        })
    }
}


class View(model:XMLElement, contr:Controller) : JFrame("title"),IObservable<XMlListener> {

    override val observers: MutableList<XMlListener> = mutableListOf()


    init {
        defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        size = Dimension(300, 300)

        add(EntityRepresentation(model,contr))
    }

    fun open() {
        isVisible = true
    }

}

interface Command {
    fun run()
    fun undo()
}

class UndoStack {
    val stack = Stack<Command>()

    fun execute(c: Command) {
        c.run()
        stack.add(c)
    }

    fun undo() {
        if (stack.isNotEmpty())
            stack.pop().undo()
    }
}

class AddEntityCommand(val e:Entity,val name:String,val parent:Entity?):Command{
    var ent:Entity?=null
    override fun run() {
        ent=e.addEntityChild(name,parent)
    }

    override fun undo() {
        ent?.removeEntity()
    }
}

class AddAttributeCommand(val e:Entity,val key:String,val value:String):Command{

    override fun run() {
        e.addAttribute(key,value)
    }

    override fun undo() {
        e.removeAttribute(key)
    }
}



/*running UI*/
fun main() {
    val e = Entity("root",null)
    val contr=Controller()
    val w = View(e,contr)

    w.open()

}

