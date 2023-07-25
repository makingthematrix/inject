package io.github.makingthematrix.inject

import io.github.makingthematrix.inject.*
import io.github.makingthematrix.inject.Inject.typeName
import munit.Location

import java.net.URI
import java.util.TreeMap

class InjectTest extends munit.FunSuite:
  implicit val location: Location = Location.empty

  test("Bind and inject") {
    case class Foo(str: String)

    val module = new Module {
      bind[Foo] to Foo("foo")
    }

    class Bar extends Injectable(module) {
      def str: String = {
        val foo = inject[Foo]
        foo.str
      }
    }

    assertEquals("foo", new Bar().str)
  }

  test("Bind with the default module") {
    case class Foo(str: String)

    Inject.set(new Module {
      bind[Foo] to Foo("foo")
    })

    class Bar extends Injectable {
      def str: String = {
        val foo = inject[Foo]
        foo.str
      }
    }

    assertEquals("foo", new Bar().str)
  }

  test("Bind and inject more than one") {
    case class Foo(str: String)
    case class Baz(n: Int)

    val module: Module = new Module {
      bind[Foo] to Foo("foo")
      bind[Baz] to Baz(1)
    }

    class Bar extends Injectable(module) {
      def str: String = {
        val foo = inject[Foo]
        foo.str
      }

      def n: Int = {
        val baz = inject[Baz]
        baz.n
      }
    }

    assertEquals("foo", new Bar().str)
    assertEquals(1, new Bar().n)
  }


  test("Bind to provider") {
    case class Foo(n: Int)

    val module: Module = new Module {
      private var n: Int = 0
      bind[Foo] toProvider  { n += 1; Foo(n) }
    }

    class Bar extends Injectable(module) {
      def n: Int = {
        val foo = inject[Foo]
        foo.n
      }
    }

    val bar = Bar()
    assertEquals(1, bar.n)
    assertEquals(2, bar.n)
  }

  test("ground type") {
    assertEquals(typeName[String], "java.lang.String")
  }

  test("parameterized type") {
    assertEquals(typeName[List[Int]], "scala.collection.immutable.List[scala.Int]")
  }

  test("array (historically problematic)") {
    assertEquals(typeName[Array[Int]], "scala.Array[scala.Int]")
  }

  test("abstracted") {
    def foo[A: TypeName]: String = s"The name is ${typeName[A]}"
    assertEquals(foo[Array[Double]], "The name is scala.Array[scala.Double]")
  }

  test("fully-qualified") {
    assertEquals(typeName[TreeMap[ClassLoader, URI]], "java.util.TreeMap[java.lang.ClassLoader, java.net.URI]")
  }

  test("wildcard (shorthand)") {
    assertEquals(typeName[List[_]], "scala.collection.immutable.List[_ >: scala.Nothing <: scala.Any]")
  }
