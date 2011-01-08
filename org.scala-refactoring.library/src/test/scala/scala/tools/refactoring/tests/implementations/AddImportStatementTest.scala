/*
 * Copyright 2005-2010 LAMP/EPFL
 */

package scala.tools.refactoring
package tests.implementations

import implementations.AddImportStatement
import tests.util.TestRefactoring
import tests.util.TestHelper
import common.Change
import org.junit.Assert._

class AddImportStatementTest extends TestHelper {
  outer =>
  
  def addImport(imp: (String, String), src: String, expected: String) = {
        
    val refactoring = new AddImportStatement with SilentTracing {
      val global = outer.global
      
      val selection = {
        val start = commentSelectionStart(src)
        val end = commentSelectionEnd(src)
        val file = addToCompiler(randomFileName(), src)
        new FileSelection(file, start, end)
      }
      val change = addImport(selection, imp._1 + "." + imp._2)
    }
    
    assertEquals(expected, Change.applyChanges(refactoring.change, src))
  }

  @Test
  def importInEmpty = {
    addImport(("collection.mutable", "ListBuffer"), """
      object Main {val lb = ListBuffer(1)}
    """,
    """
      import collection.mutable.ListBuffer
      object Main {val lb = ListBuffer(1)}
    """)
  }

  @Test
  def importAlreadyExisting = {
    addImport(("collection.mutable", "ListBuffer"), """
      import collection.mutable.ListBuffer
      object Main {}
    """,
    """
      import collection.mutable.ListBuffer
      import collection.mutable.ListBuffer
      object Main {}
    """)
  }

  @Test
  def importIsInsertedAtEnd = {
    addImport(("collection.mutable", "ListBuffer"), """
      import collection.mutable.HashMap

      import collection.mutable.HashMap

      object Main {}
    """,
    """
      import collection.mutable.HashMap
      import collection.mutable.HashMap
      import collection.mutable.ListBuffer

      object Main {}
    """)
  }

  @Test
  def importWithNestedPackages = {
    addImport(("collection.mutable", "ListBuffer"), """
      package nstd
      package pckg

      import collection.mutable.HashMap

      import collection.mutable.HashMap

      object Main {}
    """,
    """
      package nstd
      package pckg

      import collection.mutable.HashMap
      import collection.mutable.HashMap
      import collection.mutable.ListBuffer

      object Main {}
    """)
  }

  @Test
  def importExistsBetweenPackages = {
    addImport(("collection.mutable", "ListBuffer"), """
      package nstd

      import collection.mutable.HashMap

      package pckg

      import collection.mutable.HashMap
      import collection.mutable.HashMap

      object Main {}
    """,
    """
      package nstd

      import collection.mutable.HashMap
      import collection.mutable.ListBuffer

      package pckg

      import collection.mutable.HashMap
      import collection.mutable.HashMap

      object Main {}
    """)
  }
}
