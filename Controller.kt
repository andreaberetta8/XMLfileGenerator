import java.io.File
import java.util.*


var flagAddE=false
var flagAddTextE=false
var elSpecialUndo:XMLElement?=null
/**
 * this class represent the controller used in the mvc design pattern,it specifically controls this model (XMLElement) and
 */
class Controller() {

    /**
     * list of executed commands
     */
    val listOfCommand=UndoStack()


    fun addEntity(e:XMLElement,name:String){
        val ent= e as Entity
        listOfCommand.execute(AddEntityCommand(ent,name))
    }

    fun renameEntity(e:XMLElement,newName:String){
        listOfCommand.execute(RenameCommand(e,newName))
    }

    fun deleteEntity(e:XMLElement){
        val remCommand=RemoveEntityCommand(e)
        listOfCommand.execute(remCommand)
        elSpecialUndo=remCommand.deleted
    }

    fun addNewAttribute(e:XMLElement,key:String?,value:String?){
        if (key!=null)
            listOfCommand.execute(AddAttributeCommand(e,key,value!!))
    }

    fun modifyAttribute(e:XMLElement,key:String?,newValue:String?){
        if (key!=null)
            listOfCommand.execute(EditAttributeCommand(e,key,newValue!!))
    }

    fun deleteAttribute(e:XMLElement,key:String?){
        if (key!=null)
            listOfCommand.execute(RemoveAttributeCommand(e,key))
    }

    fun addTextEntity(e:XMLElement,name:String,text:String){
        if (flagAddTextE){
            elSpecialUndo?.removeEntity()
            flagAddTextE=false
            return
        }
            val ent = e as Entity
            listOfCommand.execute(AddTextEntityCommand(ent, name, text))
    }




    fun goBack(){
        val commands=listOfCommand.stack
        if (!commands.empty())
           listOfCommand.undo()
        }


    fun generateXMlFile(ent:XMLElement?,name:String){
        var e:Entity=ent as Entity
        while (e.parent!=null)
            e= e.parent!!

        var file = File(name)


        file.writeText(e.serialize())
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

class AddEntityCommand(val e:Entity,val name:String):Command{
    var ent:Entity?=null
    override fun run() {
        ent=e.addEntityChild(name)
    }

    override fun undo() {
        ent?.removeEntity()
    }
}

class AddTextEntityCommand(val e:Entity,val name:String,val text:String):Command{
    var tEnt:TextEntity?=null

    override fun run() {
        tEnt=e.addTextEntityChild(name,text)
    }

    override fun undo() {
        if (elSpecialUndo!=null)//serve a gestire il caso specialedel doppio undo, in cui ultimo comando dello stack è rimuovi x e il penultimo è aggiungi x
            if (flagAddTextE && e.name== elSpecialUndo!!.name) {
                elSpecialUndo!!.removeEntity()
                return
            }
        tEnt?.removeEntity()
    }
}

class RemoveEntityCommand(val e:XMLElement):Command{

    var deleted:XMLElement?=null
    override fun run() {
        deleted=e.removeEntity()
    }

    override fun undo() {
        if (deleted!=null) {
            if (deleted is Entity)
                e.parent?.addEntityChild(deleted as Entity)
            else deleted!!.parent?.addTextEntityChild(deleted as TextEntity)
        }
    }
}



class AddAttributeCommand(val e:XMLElement,val key:String,val value:String):Command{

    override fun run() {
        e.addAttribute(key,value)
    }

    override fun undo() {
        e.removeAttribute(key)
    }
}

class RenameCommand(val e:XMLElement,val newName:String):Command{
    var originalName=""
    override fun run() {
        originalName=e.name
        e.name=newName
    }

    override fun undo() {
        e.name=originalName
    }
}

class EditAttributeCommand(val e:XMLElement,val key:String,val newName:String):Command{
    var originalName=""
    override fun run() {
        originalName=e.editAttribute(key,newName)
    }

    override fun undo() {
        e.editAttribute(key,originalName)
    }
}

class RemoveAttributeCommand(val e:XMLElement,val key:String):Command{
    var deletedAtt:Attribute?=null
    override fun run() {
        deletedAtt=e.removeAttribute(key)
    }

    override fun undo() {
        deletedAtt?.let { e.addAttribute(key, it.value) }
    }
}
