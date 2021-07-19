package org.folio.util;

import io.vertx.core.Future;

import java.util.concurrent.CompletableFuture;

public class CustomCompletableFuture<V> extends CompletableFuture<V> {
  private final Future<V> future;

  private final long timeout;

  public CustomCompletableFuture(Future<V> future) {
    this(future,7000);
  }

  public CustomCompletableFuture(Future<V> future,long timeout) {
    this.future = future;
    this.timeout = timeout;
    new CustomCompletableFutureContext(timeout).schedule(this::tryToComplete);
  }

  private void tryToComplete(){
    if (future.isComplete()){
      complete(future.result());
      return;
    }

    if (future.failed()){
      cancel(true);
      return;
    }
    new CustomCompletableFutureContext(timeout).schedule(this::tryToComplete);
  }

}
