package com.github.edgar615.direwolves.http.loadbalance;

import io.vertx.servicediscovery.Record;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 从给定列表中轮询选择一个节点.
 * <p>
 * 由于给定的节点列表会变化，并不是严格意义上的轮询算法.
 *
 * @author Edgar  Date 2016/8/5
 */
class RoundRobinStrategy implements ChooseStrategy {

  private final AtomicInteger integer = new AtomicInteger(0);

  @Override
  public Record get(List<Record> records) {
    if (records == null || records.isEmpty()) {
      return null;
    }
    int index = Math.abs(integer.getAndIncrement());
    return records.get(index % records.size());
  }

}