package org.folio.util;

import java.util.List;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.impl.CompositeFutureImpl;

public interface CustomCompositeFuture extends CompositeFuture {
  static <T> CompositeFuture all(List<Future<T>> futures) {
    return CompositeFutureImpl.all(futures.toArray(new Future[0]));
  }
}
