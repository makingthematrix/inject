[![Scala CI](https://github.com/makingthematrix/inject/actions/workflows/scala.yml/badge.svg)](https://github.com/makingthematrix/inject/actions/workflows/scala.yml)

# Inject
A minimalistic dependency injection micro-library

## Overview

**Inject** provides basic dependency injection functionality for small and medium Scala 3 projects.
All you need to do is:
1. extend the `Module` class and provide a list of bindings, line by line, of what dependency should be injected if a given type is requested
2. use `inject[<typename>]` to inject the dependency where you need it.

In contrast to more advanced DI libraries, **Inject** doesn't provide you with constructor parameters behind the scenes.
You need to call the `inject` method explicitly. This requirement might prove unfit for some more complex situations, but then, **Inject**
is mainly aimed at small projects which need only basic DI functionality. If you are searching for something more powerful,
please take a look at [macwire](https://github.com/softwaremill/macwire).

## How to use

**sbt**:
```sbt
  libraryDependencies += "io.github.makingthematrix" %% "inject" % "1.0.0"
```

**Maven**:
```xml
<dependency>
    <groupId>io.github.makingthematrix</groupId>
    <artifactId>inject_3</artifactId>
    <version>1.0.0</version>
</dependency>
```

**Mill**:
```
ivy"io.github.makingthematrix::inject:1.0.0"
```

**Gradle**:
```
compile group: 'io.github.makingthematrix', name: 'inject_3', version: '1.0.0'
```

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
In this case, injecting dependencies in the constructor parameters list is impossible because we don't have
a default module, and the `Injectable` trait is added later.

```scala 3
case class Foo(str: String)

val module = new Module {
bind[Foo] to Foo("foo")
}

class Bar extends Injectable(module):
def foo: Foo = inject[Foo]
```

This way, you can create different modules for different parts of your project. You can also join them together:
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

Also, on top of binding dependencies as singletons with `to,` you can use `toProvider` to get a new instance every time:
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

## Use cases

1. **Basic dependency injection for small and medium projects in Scala 3**. It's a bit verbose on the injecting side, as
   calling `inject[<typename>]` needs to be explicit, but on the other hand, the binding side is simple to code.
2. **Lazy initialization of singletons**. In Scala, it's possible to use `object`s as singletons, but all data held in
   them are initialized eagerly unless it is explicitly marked as `lazy`. With **Inject** you can write a singleton as
   a class, bind its initialization to a type with `bind[<typename>] to <initialization>`, and the initialization will
   be called only at the first call to `inject[<typename>]`.
3. **Swapping implementations in tests**. Suppose you use **Inject** together with the pattern of having separate pairs of
   a trait with public API and an implementation class for that trait. In that case, you can easily swap the implementation in your
   tests for a fake one, a simpler one, or one with more logging info. Just create a separate module and set it as default,
   or provide it to the class under test.

## Frequently Asked Questions
(that is, I have just asked them myself)

### What is a micro-library?

A micro-library is a library so small that you can include its source code into your project - and you are
free to do so, as long as you don't break the license. **Inject** consists of only one Scala 3 file and just a handful 
of methods in its API: you can create a module and a list of bindings, set the module as default, or join it with other 
modules, and then use it to inject dependencies - that's it. 

Another advantage of all this simplicity is that a micro-library can be easily covered with unit tests, and once it works,
there is little risk that it will ever have breaking changes (or that it will change at all).

### What about circular dependencies?

Try not to make them.

**Inject** initializes bound dependencies lazily, i.e., every singleton is initialized only when `inject` is called on it
for the first time. Mixed with the ability to call `inject` directly in the place in the code where the dependency
is needed, this feature may help with some problems with circular dependencies by postponing the initialization of a dependency that would
otherwise create an infinite loop of initializations. But the rest is up to you. **Inject** will not check the dependency
graph of your project, and if you're not careful, it will happily freeze your application at init.

## Acknowledgments

**Inject** is based on:
1. A minimalistic DI written a long time ago for [wire-android](https://github.com/wireapp/wire-android), a now-deprecated 
   Wire client app for Android written in Scala 2.11. The author's name is long lost, but I have a feeling it could 
   have been [Zbigniew Szymański](https://github.com/zbsz).
2. [typename](https://github.com/tpolecat/typename) by [Rob Norris](https://github.com/tpolecat), a micro-library for, well,
   finding, type names. It's based on `scala.quotes`, so it's more lightweight than, say, if we used `ClassTag` or `Manifest`.

---
If you like what you see here, take a look at [signals3](https://github.com/makingthematrix/signals3), an lightweight event streams
library written in Scala 3, which I also salvaged from Wire Android.
