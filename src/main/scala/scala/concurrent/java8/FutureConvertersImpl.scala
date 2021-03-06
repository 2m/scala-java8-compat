/*
 * Copyright (C) 2012-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package scala.concurrent.java8

// Located in this package to access private[concurrent] members

import scala.concurrent.{ Future, Promise, ExecutionContext, ExecutionContextExecutorService, ExecutionContextExecutor, impl }
import java.util.concurrent.{ CompletionStage, Executor, ExecutorService, CompletableFuture }
import scala.util.{ Try, Success, Failure }
import java.util.function.{ BiConsumer, Function ⇒ JF, Consumer, BiFunction }

// TODO: make thie private[scala] when genjavadoc allows for that.
object FuturesConvertersImpl {
  def InternalCallbackExecutor = Future.InternalCallbackExecutor

  class CF[T] extends CompletableFuture[T] with (Try[T] => Unit) {
    override def apply(t: Try[T]): Unit = t match {
      case Success(v) ⇒ complete(v)
      case Failure(e) ⇒ completeExceptionally(e)
    }

    /*
     * Ensure that completions of this future cannot hold the Scala Future’s completer hostage.
     */
    override def thenApply[U](fn: JF[_ >: T, _ <: U]): CompletableFuture[U] = thenApplyAsync(fn)

    override def thenAccept(fn: Consumer[_ >: T]): CompletableFuture[Void] = thenAcceptAsync(fn)

    override def thenRun(fn: Runnable): CompletableFuture[Void] = thenRunAsync(fn)

    override def thenCombine[U, V](cs: CompletionStage[_ <: U], fn: BiFunction[_ >: T, _ >: U, _ <: V]): CompletableFuture[V] = thenCombineAsync(cs, fn)

    override def thenAcceptBoth[U](cs: CompletionStage[_ <: U], fn: BiConsumer[_ >: T, _ >: U]): CompletableFuture[Void] = thenAcceptBothAsync(cs, fn)

    override def runAfterBoth(cs: CompletionStage[_], fn: Runnable): CompletableFuture[Void] = runAfterBothAsync(cs, fn)

    override def applyToEither[U](cs: CompletionStage[_ <: T], fn: JF[_ >: T, U]): CompletableFuture[U] = applyToEitherAsync(cs, fn)

    override def acceptEither(cs: CompletionStage[_ <: T], fn: Consumer[_ >: T]): CompletableFuture[Void] = acceptEitherAsync(cs, fn)

    override def runAfterEither(cs: CompletionStage[_], fn: Runnable): CompletableFuture[Void] = runAfterEitherAsync(cs, fn)

    override def thenCompose[U](fn: JF[_ >: T, _ <: CompletionStage[U]]): CompletableFuture[U] = thenComposeAsync(fn)

    override def whenComplete(fn: BiConsumer[_ >: T, _ >: Throwable]): CompletableFuture[T] = whenCompleteAsync(fn)

    override def handle[U](fn: BiFunction[_ >: T, Throwable, _ <: U]): CompletableFuture[U] = handleAsync(fn)

    override def exceptionally(fn: JF[Throwable, _ <: T]): CompletableFuture[T] = {
      val cf = new CompletableFuture[T]
      whenCompleteAsync(new BiConsumer[T, Throwable] {
        override def accept(t: T, e: Throwable): Unit = {
          if (e == null) cf.complete(t)
          else {
            val n: AnyRef =
              try {
                fn(e).asInstanceOf[AnyRef]
              } catch {
                case thr: Throwable ⇒ cf.completeExceptionally(thr); this
              }
            if (n ne this) cf.complete(n.asInstanceOf[T])
          }
        }
      })
      cf
    }

    override def toCompletableFuture(): CompletableFuture[T] =
      throw new UnsupportedOperationException("this CompletionStage represents a read-only Scala Future")

    override def toString: String = super[CompletableFuture].toString
  }

  class P[T] extends impl.Promise.DefaultPromise[T] with BiConsumer[T, Throwable] {
    override def accept(v: T, e: Throwable): Unit = {
      if (e == null) complete(Success(v))
      else complete(Failure(e))
    }
  }
}
