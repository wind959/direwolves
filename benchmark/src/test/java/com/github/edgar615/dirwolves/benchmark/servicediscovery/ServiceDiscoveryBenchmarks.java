package com.github.edgar615.dirwolves.benchmark.servicediscovery;

import com.github.edgar615.util.base.Randoms;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.types.HttpEndpoint;
import org.openjdk.jmh.annotations.*;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Created by Edgar on 2017/7/31.
 *
 * @author Edgar  Date 2017/7/31
 */
@State(Scope.Benchmark)
public class ServiceDiscoveryBenchmarks {

  @TearDown(Level.Trial)
  public void tearDown(DiscoveryBackend pool) {
    pool.close();
  }

  @Benchmark
  @BenchmarkMode(Mode.Throughput)
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  @Fork(1)
  @OperationsPerInvocation(10000)
  public void testThroughput(DiscoveryBackend pool) {
    final CountDownLatch latch = new CountDownLatch(1);
    pool.getDefinitions(r -> "test".equals(r.getName()), ar -> {
      latch.countDown();
    });
    try {
      latch.await();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  @Fork(1)
  @OperationsPerInvocation(10000)
  public void testAverage(DiscoveryBackend backend) {
    final CountDownLatch latch = new CountDownLatch(1);
    backend.getDefinitions(r -> "test".equals(r.getName()), ar -> {
      latch.countDown();
    });
    try {
      latch.await();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  @State(Scope.Benchmark)
  public static class DiscoveryBackend {
    private Vertx vertx;

    private ServiceDiscovery discovery;

    public DiscoveryBackend() {
      vertx = Vertx.vertx();
      discovery = ServiceDiscovery.create(vertx);
      for (int i = 0; i < 10; i++) {
        Record record = HttpEndpoint.createRecord("test", "localhost", 8081 + i, "/");
        discovery.publish(record, ar -> {
        });
      }
      for (int i = 0; i < 1000; i++) {
        Record record = HttpEndpoint.createRecord(
                Randoms.randomAlphabet(5), "localhost", 8081 + i, "/");
        discovery.publish(record, ar -> {
        });
      }
      try {
        TimeUnit.SECONDS.sleep(3);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    public void getDefinitions(Function<Record, Boolean> filter,
                               Handler<AsyncResult<List<Record>>>
                                       handler) {
      discovery.getRecords(filter, handler);
    }

    public void close() {
      vertx.close();
    }
  }
}
