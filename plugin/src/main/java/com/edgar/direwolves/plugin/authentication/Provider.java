package com.edgar.direwolves.plugin.authentication;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Launcher;
import io.vertx.core.Vertx;
import io.vertx.serviceproxy.ProxyHelper;

/**
 * Created by edgar on 16-11-30.
 */
public class Provider extends AbstractVerticle {

  public static void main(String[] args) {
    new Launcher().execute("run", Provider.class.getName(), "--cluster");
  }

  @Override
  public void start() throws Exception {
    SomeService someService = SomeService.createService(vertx, config());
    ProxyHelper.registerService(SomeService.class, vertx, someService, SomeService.SERVICE_ADDRESS);
  }

}