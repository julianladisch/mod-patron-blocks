package org.folio.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CustomCompletableFutureContext {
  private final ScheduledExecutorService SERVICE = Executors.newSingleThreadScheduledExecutor();

  private final long timeout;

  public CustomCompletableFutureContext(long timeout) {
    this.timeout = timeout;
  }

  public void schedule(Runnable runnable) {
    schedule(runnable, this.timeout);
  }

  public void schedule(Runnable runnable, long timeout) {
    SERVICE.schedule(runnable, timeout, TimeUnit.MILLISECONDS);
  }
}
