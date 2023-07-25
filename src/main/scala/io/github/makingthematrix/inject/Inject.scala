package io.github.makingthematrix.inject

import scala.quoted.*
import scala.collection.mutable

import Inject.typeName

object Inject:
  private[inject] def typeName[A](using ev: TypeName[A]): String = ev.value

  private var defaultModule = Option.empty[Module]
  private lazy val emptyModule: Module = new Module

  private[inject] def module: Module = defaultModule.getOrElse(emptyModule)

  def set(module: Module): Unit =
    defaultModule = Some(module)

private[inject] trait TypeNamePlatform:
  inline given [A]: TypeName[A] = ${TypeNamePlatform.impl[A]}

private[inject] object TypeNamePlatform:
  def impl[A](using t: Type[A], ctx: Quotes): Expr[TypeName[A]] = '{TypeName[A](${Expr(Type.show[A])})}

private[inject] final case class TypeName[A](value: String)
private[inject] object TypeName extends TypeNamePlatform

private[inject] final case class Provider[T](fn: () => T) extends (() => T):
  def apply(): T = fn()

private[inject] final case class Singleton[T](fn: () => T) extends (() => T):
  private lazy val value: T = fn()
  def apply(): T = value

trait Injectable(module: Module = Inject.module):
  def inject[T: TypeName]: T = module.apply[T]()

class Module { self =>
  private val bindings = new mutable.HashMap[String, () => _]

  final protected def bind[T: TypeName]: Binding[T] = new Binding[T]()

  protected final class Binding[T: TypeName]():
    def to(fn: => T): Unit = bindings += typeName[T] -> Singleton(() => fn)
    def toProvider(fn: => T): Unit = bindings += typeName[T] -> Provider(() => fn)

  private[inject] def binding[T: TypeName]: Option[() => T] =
    internal(implicitly[TypeName[T]]).orElse(parent.flatMap(_.binding[T]))

  private inline def internal[T](m: TypeName[T]): Option[() => T] =
    bindings.get(m.value).asInstanceOf[Option[() => T]]

  private[inject] def apply[T: TypeName](): T =
    binding[T].getOrElse(throw new Exception(s"No binding for: ${typeName[T]} in $this")).apply()

  protected val head: Module = self
  protected val tail: Module = self

  private var _parent = Option.empty[Module]
  private[inject] def parent: Option[Module] = _parent
  protected def setParent(module: Module): Unit = _parent = Some(module)

  final def ::(module: Module): Module = new Module {
    module.tail.setParent(self.head)

    override protected val head: Module = module.head
    override protected val tail: Module = self.tail

    override private[inject] def binding[T: TypeName]: Option[() => T] = head.binding

    override def toString: String = s"Module($module :: $self, bindings: ${bindings.keys.mkString(",")}, parent: $parent)"
  }

  override def toString: String = s"Module(bindings: ${bindings.keys.mkString(",")}, parent: $parent)"
}
