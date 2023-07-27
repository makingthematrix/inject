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

    class Bar extends Injectable(module):
      def str: String =
        val foo = inject[Foo]
        foo.str

    assertEquals("foo", new Bar().str)
  }

  test("Inject in the constructor") {
    Inject.set(new Module {
      bind[String] to "foo"
    })

    import Inject.inject
    class Foo(val str: String = inject[String])

    assertEquals("foo", new Foo().str)
  }

  test("Bind with the default module") {
    case class Foo(str: String)

    Inject.set(new Module {
      bind[Foo] to Foo("foo")
    })

    class Bar extends Injectable:
      def str: String =
        val foo = inject[Foo]
        foo.str

    assertEquals("foo", new Bar().str)
  }

  test("throw a no binding exception") {
    case class Foo(str: String)

    Inject.set(new Module {
      bind[Foo] to Foo("foo")
    })

    class Bar extends Injectable:
      def n: Int =
        val n = inject[Int]
        n

    intercept[NoBindingException](new Bar().n)
  }

  test("Initialization should be lazy") {
    var globalN: Int = 1

    class Foo:
      val fooN: Int = globalN

    Inject.set(new Module {
      bind[Foo] to Foo()
    })

    class Bar extends Injectable:
      def getN: Int =
        globalN = 2
        inject[Foo].fooN

    assertEquals(2, new Bar().getN)
  }

  test("Bind and inject more than one") {
    case class Foo(str: String)
    case class Baz(n: Int)

    val module: Module = new Module {
      bind[Foo] to Foo("foo")
      bind[Baz] to Baz(1)
    }

    class Bar extends Injectable(module):
      def str: String = inject[Foo].str
      def n: Int = inject[Baz].n

    assertEquals("foo", new Bar().str)
    assertEquals(1, new Bar().n)
  }

  test("Bind to provider") {
    case class Foo(n: Int)

    val module: Module = new Module {
      private var n: Int = 0
      bind[Foo] toProvider  { n += 1; Foo(n) }
    }

    class Bar extends Injectable(module):
      def n: Int = inject[Foo].n

    val bar = Bar()
    assertEquals(1, bar.n)
    assertEquals(2, bar.n)
  }

  test("Merge two modules") {
    case class Foo(str: String)

    val module1 = new Module {
      bind[Foo] to Foo("foo")
    }

    case class Bar(n: Int)

    val module2 = new Module {
      bind[Bar] to Bar(1)
    }

    val module3 = module1 :: module2

    class C extends Injectable(module3): // can inject both Foo and Bar
      def foo: String = inject[Foo].str
      def bar: Int = inject[Bar].n

    val c = C()
    assertEquals("foo", c.foo)
    assertEquals(1, c.bar)
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
