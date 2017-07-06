package com.edgar.direwolves.cmd;

import com.edgar.direwolves.core.cmd.ApiCmd;
import com.edgar.direwolves.core.definition.ApiDefinition;
import com.edgar.direwolves.core.definition.ApiDiscovery;
import com.edgar.direwolves.verticle.ApiDefinitionRegistry;
import com.edgar.util.exception.DefaultErrorCode;
import com.edgar.util.vertx.eventbus.Event;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Edgar on 2017/1/19.
 *
 * @author Edgar  Date 2017/1/19
 */
@RunWith(VertxUnitRunner.class)
public class ApiImporterCmdTest extends BaseApiCmdTest {

  @Test
  public void testMissNameShouldThrowValidationException(TestContext testContext) {
    AtomicBoolean check = new AtomicBoolean();
    Event event = Event.builder()
            .setAddress("direwolves.eb.api.import")
            .setBody(new JsonObject())
            .build();
    vertx.eventBus().<Event>send("direwolves.eb.api.import", event, ar -> {
      if (ar.succeeded()) {
        testContext.fail();
      } else {
        ar.cause().printStackTrace();
        testContext.assertTrue(ar.cause() instanceof ReplyException);
        testContext.assertEquals(DefaultErrorCode.INVALID_ARGS.getNumber(),
                                 ReplyException.class.cast(ar.cause()).failureCode());
        check.set(true);
      }
    });
    Awaitility.await().until(() -> check.get());

  }

  @Test
  public void testImportDirSuccess(TestContext testContext) {
    JsonObject jsonObject = new JsonObject()
            .put("namespace", namespace)
        .put("path", "src/test/resources/api");
    AtomicBoolean check1 = new AtomicBoolean();
    Event event = Event.builder()
            .setAddress("direwolves.eb.api.import")
            .setBody(jsonObject)
            .build();
    vertx.eventBus().<Event>send("direwolves.eb.api.import", event, ar -> {
      if (ar.succeeded()) {
        check1.set(true);
      } else {
        testContext.fail();
      }
    });
    Awaitility.await().until(() -> check1.get());

    AtomicBoolean check3 = new AtomicBoolean();
    discovery.getDefinitions(new JsonObject(), ar -> {
      if (ar.failed()) {
        testContext.fail();
        return;
      }
      System.out.println(ar.result());
      testContext.assertEquals(2, ar.result().size());
      check3.set(true);
    });
    Awaitility.await().until(() -> check3.get());
  }

  @Test
  public void testImportFileSuccess(TestContext testContext) {

    JsonObject jsonObject = new JsonObject()
            .put("namespace", namespace)
            .put("path", "src/test/resources/api/device_add.json");
    AtomicBoolean check1 = new AtomicBoolean();
    Event event = Event.builder()
            .setAddress("direwolves.eb.api.import")
            .setBody(jsonObject)
            .build();
    vertx.eventBus().<Event>send("direwolves.eb.api.import", event, ar -> {
      if (ar.succeeded()) {
        check1.set(true);
      } else {
        testContext.fail();
      }
    });
    Awaitility.await().until(() -> check1.get());

    AtomicBoolean check3 = new AtomicBoolean();
    discovery.getDefinitions(new JsonObject(), ar -> {
      if (ar.failed()) {
        testContext.fail();
        return;
      }
      System.out.println(ar.result());
      testContext.assertEquals(1, ar.result().size());
      check3.set(true);
    });
    Awaitility.await().until(() -> check3.get());
  }

  @Test
  public void testInvalidJsonShouldNotAddAnyApi(TestContext testContext) {

    JsonObject jsonObject = new JsonObject()
            .put("namespace", namespace)
            .put("path", "src/test/resources/invalid");
    AtomicBoolean check1 = new AtomicBoolean();
    Event event = Event.builder()
            .setAddress("direwolves.eb.api.import")
            .setBody(jsonObject)
            .build();
    vertx.eventBus().<Event>send("direwolves.eb.api.import", event, ar -> {
      if (ar.succeeded()) {
        check1.set(true);
      } else {
        testContext.fail();
      }
    });
    Awaitility.await().until(() -> check1.get());

    AtomicBoolean check3 = new AtomicBoolean();
    discovery.getDefinitions(new JsonObject(), ar -> {
      if (ar.failed()) {
        testContext.fail();
        return;
      }
      System.out.println(ar.result());
      testContext.assertEquals(0, ar.result().size());
      check3.set(true);
    });
    Awaitility.await().until(() -> check3.get());
  }

}