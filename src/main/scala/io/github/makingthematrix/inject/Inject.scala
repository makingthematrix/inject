package io.github.makingthematrix.inject

import scala.quoted.*
import scala.collection.mutable

object Inject:
  def set(defaultModule: Module): Unit =
    _defaultModule = Some(defaultModule)

  def inject[T: TypeName]: T = defaultModule.apply[T]()

  private var _defaultModule = Option.empty[Module]
  private lazy val empty: Module = new Module

  private[inject] def defaultModule: Module = _defaultModule.getOrElse(empty)

  private[inject] def typeName[A](using ev: TypeName[A]): String = ev.value

trait Injectable(private val module: Module = Inject.defaultModule):
  final def inject[T: TypeName]: T = module.apply[T]()

class Module { self =>
  import Inject.typeName

  protected final class Binding[T: TypeName]:
    def to(fn: => T): Unit = bindings += typeName[T] -> Singleton(() => fn)
    def toProvider(fn: => T): Unit = bindings += typeName[T] -> Provider(() => fn)

  private final class Provider[T](fn: () => T) extends (() => T):
    def apply(): T = fn()

  private final class Singleton[T](fn: () => T) extends (() => T):
    private lazy val value: T = fn()
    def apply(): T = value

  protected final def bind[T: TypeName]: Binding[T] = new Binding[T]()

  final def ::(module: Module): Module = new Module {
    module.tail.setParent(self.head)

    override protected val head: Module = module.head
    override protected val tail: Module = self.tail

    override private[inject] def binding[T: TypeName]: Option[() => T] = head.binding

    override def toString: String = s"Module($module :: $self, bindings: ${bindings.keys.mkString(",")}, parent: $parent)"
  }

  override def toString: String = s"Module(bindings: ${bindings.keys.mkString(",")}, parent: $parent)"

  private val bindings = new mutable.HashMap[String, () => _]

  private[inject] def binding[T: TypeName]: Option[() => T] =
    internal(implicitly[TypeName[T]]).orElse(parent.flatMap(_.binding[T]))

  private inline def internal[T](m: TypeName[T]): Option[() => T] =
    bindings.get(m.value).asInstanceOf[Option[() => T]]

  private[inject] def apply[T: TypeName](): T =
    binding[T].getOrElse(throw new Exception(s"No binding for: ${typeName[T]} in $this")).apply()

  protected val head: Module = self
  protected val tail: Module = self

  protected def setParent(module: Module): Unit = _parent = Some(module)

  private var _parent = Option.empty[Module]
  private[inject] def parent: Option[Module] = _parent
}

private[inject] trait TypeNamePlatform:
  inline given [A]: TypeName[A] = ${TypeNamePlatform.impl[A]}

private[inject] object TypeNamePlatform:
  def impl[A](using t: Type[A], ctx: Quotes): Expr[TypeName[A]] = '{TypeName[A](${Expr(Type.show[A])})}

private[inject] final case class TypeName[A](value: String)
private[inject] object TypeName extends TypeNamePlatform
