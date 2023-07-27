# Inject
A minimalistic dependency injection micro-library

## Overview

**Inject** provides basic dependency injection functionality for your small and medium Scala 3 projects. 
All you need to do is: 
1. extend the `Module` class and provide a list of bindings, line by line, of what dependency should be injected if a given type is requested
2. use `inject[<typename>]` to inject the dependency in the place it is needed.

Note that in contrast to more advanced DI libraries, **Inject** doesn't provide you with constructor parameters behind the scenes.
You need to explicitly call the `inject` method. This might prove unfit to some more complex situations, but then, **Inject**
is mostly aimed at small projects which require only basic DI functionality. If you search for something more powerful,
please take a look at [macwire](https://github.com/softwaremill/macwire).

## Examples

The most basic way to use **Inject** is to create a single `Module` with all the bindings, set it as default, and then
inject the dependencies in one of two ways:

1. In the constructor, using the `Inject` object:
```scala 3
  case class Foo(str: String)
  
  Inject.set(new Module {
    bind[Foo] to Foo("foo")
  })

  import Inject.inject
  
  class Bar(val foo: Foo = inject[Foo])
```

2. Directly where it is used, thanks to the `Injectable` decorator trait:
```scala 3
  case class Foo(str: String)
  
  Inject.set(new Module {
    bind[Foo] to Foo("foo")
  })

  class Bar extends Injectable:
    def foo: Foo = inject[Foo]
```

A more complex way involves creating separate modules and providing them explicitly to your `Injectable` traits.
In this case, it's not possible to inject dependencies in the constructor parameters list, because we don't have 
a default module and the `Injectable` trait is added later.

```scala 3
  case class Foo(str: String)
  
  val module = new Module {
    bind[Foo] to Foo("foo")
  }

  class Bar extends Injectable(module):
    def foo: Foo = inject[Foo]
```

This way you can create different modules for different parts of your project. You can also join them together:
```scala 3
  case class Foo(str: String)
  
  val module1 = new Module {
    bind[Foo] to Foo("foo")
  }
  
  case class Bar(n: Int)
  
  val module2 = new Module {
   bind[Bar] to Bar(1)
  }

  class A extends Injectable(module1) // can inject Foo, can't inject Bar
  class B extends Injectable(module2) // can inject Bar, can't inject Foo
  
  val module3 = module1 :: module2
  class C extends Injectable(module3) // can inject both Foo and Bar  
```

Also, on top of binding dependencies as singletons with `to`, you can use `toProvider` to get a new instance every time:
```scala 3
  case class Foo(n: Int)

  Inject.set(new Module {
    private var n: Int = 0
    bind[Foo] toProvider  { n += 1; Foo(n) }
  })

  class Bar extends Injectable:
    def n: Int = inject[Foo].n

  val bar = Bar()
  bar.n // returns 1
  bar.n // returns 2
```

## Frequently Asked Questions
(that is, I have just asked them to myself)

### What is a micro-library?

A micro-library is a library so small that you can basically just include its source code into your project - and you are
free to do so (as long as you don't break the license). **Inject** consists of only one Scala 3 file, and just a handful 
of methods in its API: you can create a module and a list of bindings, set the module as default, or join it with other 
modules, and then use it to inject dependencies - that's it. 

Another advantage of all this simplicity is that a micro-library can be easily covered with unit tests, and once it works,
there is little risk that it will ever have breaking changes (or that it will change at all).

### What about circular dependencies?

Try not to make them.

**Inject** initializes bound dependencies lazily, i.e. every singleton is initialized only when `inject` is called on it
for the first time. This, together with the ability to call `inject` directly in the place in the code where the dependency
is needed, may help with some problems with circular dependencies by postponing initialization of a dependency that would
otherwise create an infinite loop of initializations. But the rest is up to you. **Inject** will not check the dependency
graph of your project, and if you're not careful, it will happily freeze your application at init.

## Acknowledgments

**Inject** is based on:
1. A minimalistic DI written long time ago for [wire-android](https://github.com/wireapp/wire-android), a now-deprecated 
   Wire client app for Android written in Scala 2.11. The name of the author is long lost, but I have a feeling it could 
   have been [Zbigniew Szymański](https://github.com/zbsz).
2. [typename](https://github.com/tpolecat/typename) by [Rob Norris](https://github.com/tpolecat), a micro-library for
   finding, well, type names.
