package io.github.makingthematrix.inject

import scala.annotation.targetName
import scala.quoted.*
import scala.collection.mutable

/**
 * Here you can set the default module after you created it, and later inject dependencies from it.
 */
object Inject:

  /**
   * Sets the default module. Bindings from it may later be used without explicitly specifying the module.
   *
   * Example:
   * ```scala
   * Inject.set(new Module {
   *   bind[Foo] to Foo("foo")
   * })
   * ```
   *
   * @param defaultModule The module which will be set as default
   */
  def set(defaultModule: Module): Unit =
    _defaultModule = Some(defaultModule)

  /**
   * Injects a dependency with a binding specified in the default module.
   *
   * Example:
   * ```scala
   * Inject.set(new Module {
   * bind[Foo] to Foo("foo")
   * })
   *
   * class Bar(val foo: Foo = Inject.inject[Foo])
   * ```
   *
   * @tparam T The type used in the binding
   * @throws io.github.makingthematrix.inject.NoBindingException
   * @return An instance bound to the type T in the default module
   */
  @throws(classOf[NoBindingException])
  inline def inject[T: TypeName]: T = defaultModule.apply[T]()

  private var _defaultModule = Option.empty[Module]
  private lazy val empty: Module = new Module

  private[inject] def defaultModule: Module = _defaultModule.getOrElse(empty)
  private[inject] def isDefault(module: Module): Boolean = _defaultModule.contains(module)

  private[inject] def typeName[A](using ev: TypeName[A]): String = ev.value

/**
 * Enables injecting dependencies with binding in the specified module or the default module if no other is specified.
 */
trait Injectable(private val module: Module = Inject.defaultModule):

  /**
   * Injects a dependency with a binding specified in the trait.
   *
   * Example:
   * ```scala
   * val module = new Module {
   *   bind[Foo] to Foo("foo")
   * }
   *
   * class Bar extends Injectable(module):
   *   def foo: Foo = inject[Foo]
   * ```
   *
   *
   * @tparam T The type used in the binding
   * @throws io.github.makingthematrix.inject.NoBindingException
   * @return An instance bound to the type T in the module specified in the trait
   */
  @throws(classOf[NoBindingException])
  final inline def inject[T: TypeName]: T = module.apply[T]()

/**
 * An extendable class used to create a list of bindings.
 */
class Module { self =>
  import Inject.typeName

  /**
   * Creates a binding between a given type and an instance that should be injected.
   * The instance might be either a singleton, initialized at the first injected and then used as it is (this is done
   * with the `to` method) or it can be instantiated at every injection (the `toProvider` method).
   *
   * Example:
   * ```scala
   * var n = 0
   * Inject.set(new Module {
   *   bind[Foo] to Foo("foo")
   *   bind[Baz] toProvider { n +=1; Baz(n) }
   * })
   *
   * class Bar extends Injectable:
   *   def str: String = inject[Foo].str
   *   def n: Int = inject[Baz].n
   * ```
   *
   * @tparam T The type identifying the binding
   * @return the binding itself, so that the methods `to` and `toProvider` can be used on it
   */
  protected final def bind[T: TypeName]: Binding[T] = Binding[T]()

  protected final class Binding[T: TypeName]:
    def to(fn: => T): Unit = bindings += typeName[T] -> Singleton(() => fn)
    def toProvider(fn: => T): Unit = bindings += typeName[T] -> Provider(() => fn)

  private final class Provider[T](fn: () => T) extends (() => T):
    def apply(): T = fn()

  private final class Singleton[T](fn: () => T) extends (() => T):
    private lazy val value: T = fn()
    def apply(): T = value

  /**
   * Joins this module with another one, creating a new module which contains bindings from both.
   * If there is binding under the same type in both modules, the one from the module on the right side of the `::`
   * operator will override the one from the left side.
   *
   * @param module The other module
   * @return A new module, containing bindings from both original ones
   */
  @targetName("join")
  final def ::(module: Module): Module = new Module {
    module.tail.setParent(self.head)

    override protected val head: Module = module.head
    override protected val tail: Module = self.tail

    override private[inject] def binding[T: TypeName]: Option[() => T] = head.binding

    override def toString: String =
      s"Module($module :: $self, bindings: ${bindings.keys.mkString(",")}, parent: $parent), default: ${Inject.isDefault(this)}"
  }

  override def toString: String =
    s"Module(bindings: ${bindings.keys.mkString(",")}), default: ${Inject.isDefault(this)}"

  private val bindings = new mutable.HashMap[String, () => _]

  private[inject] def binding[T: TypeName]: Option[() => T] =
    internal(implicitly[TypeName[T]]).orElse(parent.flatMap(_.binding[T]))

  private inline def internal[T](m: TypeName[T]): Option[() => T] =
    bindings.get(m.value).asInstanceOf[Option[() => T]]

  private[inject] inline def apply[T: TypeName](): T =
    binding[T].getOrElse(throw new NoBindingException(typeName[T], self)).apply()

  protected val head: Module = self
  protected val tail: Module = self

  protected def setParent(module: Module): Unit = _parent = Some(module)

  private var _parent = Option.empty[Module]
  private[inject] def parent: Option[Module] = _parent
}

/**
 * An exception thrown by `inject` methods if no binding for the given type can be found.
 * @param typeName The name of the type for which there was no binding found
 * @param module The specified module
 */
final class NoBindingException(typeName: String, module: Module) extends Exception(s"No binding for $typeName in $module")

private[inject] trait TypeNamePlatform:
  inline given [A]: TypeName[A] = ${TypeNamePlatform.impl[A]}

private[inject] object TypeNamePlatform:
  def impl[A](using t: Type[A], ctx: Quotes): Expr[TypeName[A]] = '{TypeName[A](${Expr(Type.show[A])})}

private[inject] final case class TypeName[A](value: String)
private[inject] object TypeName extends TypeNamePlatform
