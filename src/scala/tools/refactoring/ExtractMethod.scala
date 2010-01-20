package scala.tools.refactoring

import scala.tools.nsc.io.AbstractFile
import scala.tools.nsc.interactive.Global

class ExtractMethod(override val global: Global) extends Refactoring(global) {
  
  import global._
  
  abstract class PreparationResult {
    def selection: Selection
    def file: AbstractFile
    def selectedMethod: Tree
  }
  
  abstract class RefactoringParameters {
    def methodName: String
  }
  
  def prepare(f: AbstractFile, from: Int, to: Int) = {
    val s = new Selection(f, from, to)
    s.enclosingDefDef match {
      case Some(defdef) =>
        Right(new PreparationResult {
          val selection = s
          val file = f
          val selectedMethod = defdef
        })
      case None => Left(new PreparationError("no enclosing defdef found"))
    }
  }
    
  def perform(prepared: PreparationResult, params: RefactoringParameters): Either[RefactoringError, ChangeSet] = {
    
    import prepared._
    import params._
    
    indexFile(file)

    val parameters = inboundLocalDependencies(selection, selectedMethod.symbol)
    
    val returns = outboundLocalDependencies(selection, selectedMethod.symbol)
     
    val newDef = mkDefDef(NoMods, methodName, parameters :: Nil, selection.trees ::: (if(returns.isEmpty) Nil else mkReturn(returns) :: Nil))
          
    val call = mkCallDefDef(NoMods, methodName, parameters :: Nil, returns)
    
    var newTree = transform(file) {
      case d: DefDef if d == selectedMethod /*ensure that we don't replace from the new method :) */ => {
        if(selection.trees.size > 1) {
          transform(d) {
            case block: Block => {
              mkBlock(replace(block, selection.trees, call :: Nil))
            }
          }
        } else {
          transform(d) {
            case t: Tree if t == selection.trees.head => call
          }
        }
      }
      case tpl @ Template(_, _, body) if body exists (_ == selectedMethod) => {
        tpl.copy(body = replace(body, selectedMethod :: Nil, selectedMethod :: newDef :: Nil))
      }
    }
    
    Right(refactor(file, newTree))
  }
}
