package com.github.edgar615.direvolves.plugin.auth;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import com.github.edgar615.direwolves.core.definition.ApiDefinition;
import com.github.edgar615.direwolves.core.definition.ApiPlugin;
import com.github.edgar615.direwolves.core.definition.SimpleHttpEndpoint;
import com.github.edgar615.direwolves.core.dispatch.ApiContext;
import com.github.edgar615.direwolves.core.dispatch.Filter;
import com.github.edgar615.direwolves.core.utils.CacheUtils;
import com.github.edgar615.direwolves.core.utils.Filters;
import com.github.edgar615.util.base.Randoms;
import com.github.edgar615.util.exception.DefaultErrorCode;
import com.github.edgar615.util.exception.SystemException;
import com.github.edgar615.util.vertx.cache.Cache;
import com.github.edgar615.util.vertx.cache.CacheOptions;
import com.github.edgar615.util.vertx.task.Task;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Edgar on 2016/10/31.
 *
 * @author Edgar  Date 2016/10/31
 */
@RunWith(VertxUnitRunner.class)
public class CachedUserLoadFilterTest {

  private final List<Filter> filters = new ArrayList<>();

  Filter filter;

  int port = Integer.parseInt(Randoms.randomNumber(4));

  String userId = UUID.randomUUID().toString();

  private Vertx vertx;

  @Before
  public void setUp() {
    vertx = Vertx.vertx();
    filters.clear();
  }

  @Test
  public void testNoUser(TestContext testContext) {
    mockCache();
    JsonObject config = new JsonObject()
            .put("cacheEnable",true);
    filter = Filter.create(UserLoaderFilter.class.getSimpleName(), vertx,
                           new JsonObject().put("user", config));
    filters.add(filter);
    ApiContext apiContext = createContext();

    JsonObject body = new JsonObject()
            .put("userId", UUID.randomUUID().toString());
    apiContext.setPrincipal(body);

    Task<ApiContext> task = Task.create();
    task.complete(apiContext);
    Async async = testContext.async();
    Filters.doFilter(task, filters)
            .andThen(context -> {
              JsonObject user = context.principal();
              System.out.println(user);
              testContext.assertFalse(user.containsKey("username"));
              testContext.fail();
            })
            .onFailure(e -> {
              testContext.assertTrue(e instanceof SystemException);
              SystemException ex = (SystemException) e;
              testContext.assertEquals(DefaultErrorCode.UNKOWN_ACCOUNT.getNumber(),
                                       ex.getErrorCode().getNumber());

              async.complete();
            });
  }

  @Test
  public void testLoadSuccess(TestContext testContext) {
    mockCache();
    JsonObject config = new JsonObject()
            .put("cacheEnable",true);
    filter = Filter.create(UserLoaderFilter.class.getSimpleName(), vertx,
                           new JsonObject().put("user", config));
    filters.add(filter);
    ApiContext apiContext = createContext();

    JsonObject body = new JsonObject()
            .put("userId", userId);
    apiContext.setPrincipal(body);

    Task<ApiContext> task = Task.create();
    task.complete(apiContext);
    Async async = testContext.async();
    Filters.doFilter(task, filters)
            .andThen(context -> {
              JsonObject user = context.principal();
              System.out.println(user);
              testContext.assertTrue(user.containsKey("username"));
              async.complete();
            })
            .onFailure(throwable -> {
              throwable.printStackTrace();
              testContext.fail();
            });
  }

  private ApiContext createContext() {
    Multimap<String, String> params = ArrayListMultimap.create();
    params.put("q3", "v3");
    Multimap<String, String> headers = ArrayListMultimap.create();
    headers.put("h3", "v3");
    headers.put("h3", "v3.2");
    ApiContext apiContext =
            ApiContext.create(HttpMethod.GET, "/devices", headers, params, null);
    SimpleHttpEndpoint httpEndpoint =
            SimpleHttpEndpoint.http("add_device", HttpMethod.GET, "devices/", 80, "localhost");
    ApiDefinition definition = ApiDefinition.create("add_device", HttpMethod.GET, "devices/", Lists
            .newArrayList(httpEndpoint));
    apiContext.setApiDefinition(definition);
    UserLoaderPlugin plugin = (UserLoaderPlugin) ApiPlugin.create(UserLoaderPlugin.class.getSimpleName());
    apiContext.apiDefinition().addPlugin(plugin);
    return apiContext;
  }

  private void mockCache() {
    AtomicBoolean complete = new AtomicBoolean();
    Cache<String, JsonObject> cache = CacheUtils.createCache(vertx, "user", new CacheOptions());
    JsonObject jsonObject = new JsonObject()
            .put("userId", userId)
            .put("username", "edgar615");
    cache.put("user:" + userId, jsonObject, ar -> {
      complete.set(true);
    });
    Awaitility.await().until(() -> complete.get());
  }

}
