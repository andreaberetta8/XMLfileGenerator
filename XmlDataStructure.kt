/**
 * an interface that provide methods to implement the observer pattern
 */
interface IObservable <O> {
    val observers: MutableList<O>

    fun addObserver(observer: O) {
        observers.add(observer)
    }

    fun removeObserver(observer: O) {
        observers.remove(observer)
    }

    fun notifyObservers(handler: (O) -> Unit) {
        observers.toList().forEach { handler(it) }
    }
}


/**
 * an interface for views that provide methods to interact with the data structure
 */
interface XMlListener{
    fun rename(newName:String)
    fun setAttribute(key:String,value:String)
    fun editAttribute(key:String,newValue:String)
    fun cancelAttribute(key:String)
    fun drawEntityAndChildren(el:XMLElement)
    fun cancelEntity(el:XMLElement)
}

/**
 * this class represent a general entity in an XML file.
 * a general XMLElement has a name and a parent, and it can have attributes
 * the class is Observable through the XMlListener interface
 */
abstract class XMLElement(name: String, var parent: Entity? = null) :IObservable<XMlListener> {
    /**
     * the list of the views that observe this class
     */
    override val observers: MutableList<XMlListener> = mutableListOf()

    /**
     * the list of the entity's attributes
     */
    var atb = mutableListOf<Attribute>()

    init {
        parent?.children?.add(this)
    }

    /**
     * name of the entity
     */
    var name = name
    set(value){
        field=value
        notifyObservers {
            it.rename(value)
        }
    }

    /**
     *  effect: deletes this entity and returns it
     * modifies: notifies all the observers about the update
     */
     open fun removeEntity():XMLElement?{
         val l=this.parent?.children
             l?.remove(this)
             notifyObservers {
                 it.cancelEntity(this)
             }

         return this
     }

    /**
     * @param key : the name of the attribute
     * @param value : the value of the attribute
     * effect: creates a new Attribute with the key and value passed as arguments, and adds it to the
     * modifies: notifies all the observers about the update
     */
    fun addAttribute(key:String,value:String){
        atb?.add(Attribute(key,value))
        notifyObservers {
            it.setAttribute(key,value)
        }
    }

    /**
     * @param key : the name of the attribute
     * @param newValue : the  new value of the attribute
     * effect: if found, it replaces the value of the attributed named key with newValue. it returns the old value
     * modifies: if the attribute is found, notifies all the observers about the update
     */
    fun editAttribute(key:String,newValue:String):String{
        var att=atb.find{e -> e.key==key }

        var oldName=""
        if (att !=null) {
            oldName=att.value
            att.value = newValue
            notifyObservers {
                it.editAttribute(att!!.key, newValue)
            }
        }
        return oldName
    }

    /**
     * @param key : the name of the attribute to delete
     * effect: if found, delete the attribute whose name is key
     * modifies: if the attribute is found, notifies all the observers about the update
     */

    fun removeAttribute(key:String):Attribute?{
        var att=atb.find{e -> e.key==key }

        if (att !=null) {
            atb.remove(att)
            notifyObservers {
                it.cancelAttribute(att.key)
            }
        }
        return att
    }

    /**
     *  returns true if this has at least one attribute, if not it returns false
     */
    fun hasAttribute():Boolean=atb.isNotEmpty()

    /**
     * @param v: the visitor object
     * effect: it calls the visit method of v
     */
    abstract fun accept(v: XMLVisitor)

}

/**
 * this class represents an entity with text
 */
class TextEntity(name: String,val text:String, parent: Entity? = null) : XMLElement(name, parent) {

    /**
     * returns a string representation of the object
     */
    override fun toString():String{
        return "<${this.name}>"+ if (atb.isNotEmpty()) atb.joinToString(" ") else {""} + " ${this.text} </${this.name}>"
    }

    override fun accept(v: XMLVisitor) {
        v.visit(this)
    }
}

/**
 * this class represent an entity which can have nested XMLElement objects
 */
class Entity(name: String, parent: Entity? = null) : XMLElement(name, parent) {

    constructor(name:String,parent: Entity? ,attributes:List<Attribute>) : this(name,parent) {
        atb=attributes as MutableList<Attribute>
    }

    /**
     * the list of nested XMLElements objects
     */
    var children = mutableListOf<XMLElement>()

    /**
     * @param name: the name of the new Entity
     * effect: add a new entity named as name to 'this' list of nested entities, and it returns it
     */
    fun addEntityChild(name:String):Entity{
        val e=Entity(name,this)
        notifyObservers {
            it.drawEntityAndChildren(e)
        }
        return e
    }

    /**
     * @param e: the child XMLElement to add
     * effect: add e to 'this' list of nested entities, and it returns it
     */
    fun addEntityChild(e:XMLElement):XMLElement{
        this.children.add(e)
        notifyObservers {
            it.drawEntityAndChildren(e)
        }
        return e
    }

    /**
     * @param name: the name of the new entity
     * @param text : the text of the new entity
     * effect: add a new entity with text to 'this' list of nested entities, and it returns it
     */
    fun addTextEntityChild(name:String,text:String):TextEntity?{
        val te=TextEntity(name,text,this)
        notifyObservers {
            it.drawEntityAndChildren(te)
        }
        return te
    }

    /**
     * @param name: the name of the new entity
     * @param text : the text of the new entity
     * effect: add a new entity with text to 'this' list of nested entities, and it returns it
     */
    fun addTextEntityChild(te:TextEntity):TextEntity?{
        this.children.add(te)
        notifyObservers {
            it.drawEntityAndChildren(te)
        }
        return te
    }


    /**
     * returns a string representation of the object
     */
    override fun toString():String{
        return "<${this.name}>"+ if (atb.isNotEmpty()) atb.joinToString(" ") else ""
    }


    override fun accept(v: XMLVisitor) {
        if(v.visit(this)) {
            children.forEach {
                it.accept(v)
            }
        }
        v.endVisit(this)
    }
}

/**
 * this interface represents a specific visitor object for this xml data structure, it lists methods to achieve visiting XMlElement objects
 */
interface XMLVisitor {
    /**
     * @param e: the visiting entity with text
     * effect: it defines the actions to execute while visiting e
     */
    fun visit(e: TextEntity) {}
    /**
     * @param e: the visiting entity with text
     * effect: it defines the actions to perform while visiting e, if the visit has to stop, it returns false, otherwise true
     */
    fun visit(e: Entity) = true

    /**
     * @param e: the visiting entity
     * effect: it defines the actions to execute before ending the visit of e
     */
    fun endVisit(e: Entity) {}
}


/**
 * @param name: the name of the entity to find
 * effect: check the XMLElement named as 'name' is inside the structure ,if not it returns null, otherwise it returns the found object
 */
fun Entity.findVisitor(name: String): XMLElement? {
    var el: XMLElement? = null
    val v = object : XMLVisitor {
        override fun visit(e: TextEntity) {
            if(e.name == name)
                el = e
        }

        override fun visit(e: Entity): Boolean {
            if (el!=null )return false //if el!= null it means the entity has already been found
            if(e.name == name) {
                el = e
                return false //if i found the entity i can stop the visit
            }
            return true
        }
    }
    accept(v)
    return el
}

/**
 * @param lmbd : the predicate used to filter the data structure
 * effect: it filters the data structure based on lmbd, and produce the resulted data structure. it returns the head of the data structure
 */
fun XMLElement.filter(lmbd: (XMLElement) -> Boolean ):Entity? {
    var firstElement:Entity?=null
    var tmpParent:Entity?=null   //represent the Entity that will be the parent of the next Entity
    var lastVisited:XMLElement?=null
        val v = object : XMLVisitor {

            override fun visit(e: TextEntity){
                if (lmbd(e)) {
                    TextEntity(e.name,e.text,tmpParent)
                }
            }
            override fun visit(e: Entity):Boolean{
                if (lmbd(e)) {
                    if (tmpParent == null) { //if i m visiting the first entity, its parent is null
                        if (e.hasAttribute())
                            tmpParent= Entity(e.name, null,e.atb)
                        else
                            tmpParent=Entity(e.name,null)

                        firstElement = tmpParent //the tmpParent became the first entity
                        lastVisited=tmpParent
                        return true
                    }
                    val ent = Entity(e.name, tmpParent) //create a copy of the current entity with parent=tmpParent
                    if (e.children.isNotEmpty())
                        tmpParent = ent //if ent has children i have to add those children to ent
                    lastVisited=ent
                }
                return true
            }

            override fun endVisit(e: Entity) {
                tmpParent= lastVisited!!.parent
            }
        }
        accept(v)
        return firstElement
    }

/**
 * effect: print a serialization of this entity and its nested entities
 */
fun Entity.serialize() :String{
    var toReturn=""
    accept(object : XMLVisitor {
        var depth = 0
        override fun visit(e: TextEntity) {
            toReturn+="\t".repeat(depth) + "$e\n"

        }

        override fun visit(e: Entity): Boolean {
            toReturn+="\t".repeat(depth) +"$e\n"
            depth++
            return true
        }

        override fun endVisit(e: Entity) {
            depth--
            toReturn+=("\t".repeat(depth) + "</${e.name}>\n")
        }

    })
    return toReturn
}

/**
 * represent a xml attribute which is composed by a key and a value
 */
class Attribute(val key:String, var value:String){

      override fun toString():String= "$key='$value' "

}

/**
 * @param name: name of the document
 * @param head: root entity of the document
 * an instance of this class represent an xml file
 */
class XMLDocument(val name:String, val head:Entity){
    private val header="<?xml version='1.0' encoding='UTF-8'?>"

    /**
     * @param f: the predicate used to filter this
     * effect: it returns an XMLDocument object from this ,after a filtering action based on f
     */
     fun XMlfilter(f :(XMLElement)->Boolean) :XMLDocument{
         return XMLDocument("filteredDoc", head.filter(f) as Entity)
     }

    /**
     * @param name : the name of the XMLElement object to find
     * effect: returns null if there is no XMLElement object named as name in the document
     */
     fun findEntity(name:String):XMLElement?{
        return head.findVisitor(name)
    }

    /**
     * effect: print a string representation of the document
     */
     fun printDocument(){
        println(header)
        println(head.serialize())
    }
}
fun main() {

    val e1=Entity("filter1",null)
    e1.addAttribute("genre ","action")
    e1.addAttribute("year ","2008")
    val t1=TextEntity("1film","mission impossible",e1)
    val t2=TextEntity("2film","joker",e1)
    val e2=e1.addEntityChild("filter2")
    e2.addAttribute("year ","2008")
    t1.addAttribute("bello","bellissimo")
    val t3=TextEntity("1film","mission impossible",e2)
    val e3=Entity("select",e2)
    val t4=TextEntity("press button","film selected",e3)
    val e4=Entity("random child",e1)



    val d=XMLDocument("filmSelection",e1)
    //d.printDocument()
    //print(d.findEntity("1film")?.name)
    //d.XMlfilter { e-> e.name!="select" }.printDocument()
    println(t1.toString())


}
