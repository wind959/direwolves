package com.github.edgar615.direwolves.verticle;

import com.github.edgar615.direwolves.core.cmd.CmdRegister;
import com.github.edgar615.direwolves.core.utils.Log;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * API定义的Verticle.
 *
 * @author Edgar  Date 2016/9/13
 */
public class ApiDefinitionVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(ApiDefinitionVerticle.class);

  @Override
  public void start(Future<Void> startFuture) throws Exception {
    Log.create(LOGGER)
            .setEvent("definition.deploying")
            .addData("config", config())
            .info();

    initialize(startFuture);
  }

  public void initialize(Future<Void> startFuture) {
    //读取命令
    new CmdRegister().initialize(vertx, config(), startFuture);

//    CompositeFuture.all(importCmdFuture)
//            .setHandler(ar -> {
//              if (ar.succeeded()) {
//                Log.create(LOGGER)
//                        .setEvent("definition.deployed.succeeed")
//                        .addData("verticle", this.getClass())
//                        .info();
//                startFuture.complete();
//              } else {
//                Log.create(LOGGER)
//                        .setEvent("definition.deployed.failed")
//                        .addData("verticle", this.getClass())
//                        .setThrowable(ar.cause())
//                        .info();
//                startFuture.fail(ar.cause());
//              }
//            });
  }


}
