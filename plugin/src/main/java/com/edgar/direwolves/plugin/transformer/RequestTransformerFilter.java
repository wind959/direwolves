package com.edgar.direwolves.plugin.transformer;

import com.edgar.direwolves.core.definition.HttpEndpoint;
import com.edgar.direwolves.core.dispatch.ApiContext;
import com.edgar.direwolves.core.dispatch.Filter;
import com.edgar.direwolves.core.utils.JsonUtils;
import com.edgar.util.exception.DefaultErrorCode;
import com.edgar.util.exception.SystemException;
import com.edgar.util.vertx.task.Task;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.servicediscovery.Record;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 将endpoint转换为json对象.
 * params和header的json均为{"k1", ["v1"]}，{"k1", ["v1", "v2]}格式的json对象.
 * <p/>
 * <pre>
 *   {
 * "id" : "5bbbe06b-df08-4728-b5e2-166faf912621",
 * "type" : "http",
 * "path" : "/devices",
 * "method" : "POST",
 * "params" : {
 * "q3" : [ "v3" ]
 * },
 * "headers" : {
 * "h3" : [ "v3", "v3.2" ]
 * },
 * "body" : {
 * "foo" : "bar"
 * },
 * "host" : "localhost",
 * "port" : 8080
 * }
 * </pre>
 * <p/>
 * Created by edgar on 16-9-20.
 */
public class RequestTransformerFilter implements Filter {

  private static final String NAME = "request";

  private Vertx vertx;

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public String type() {
    return PRE;
  }

  @Override
  public void config(Vertx vertx, JsonObject config) {
    this.vertx = vertx;
  }

  @Override
  public boolean shouldFilter(ApiContext apiContext) {
    if (apiContext.apiDefinition() == null) {
      return false;
    }
    return true;
//    List<String> filters = apiContext.apiDefinition().filters();
//    return filters.contains(NAME);
  }

  @Override
  public void doFilter(ApiContext apiContext, Future<ApiContext> completeFuture) {

    List<Future<Record>> futures = new ArrayList<>();
    apiContext.apiDefinition().endpoints().stream()
        .filter(e -> e instanceof HttpEndpoint)
        .map(e -> ((HttpEndpoint) e).service())
        .collect(Collectors.toSet())
        .forEach(s -> futures.add(serviceFuture(s)));

    Task.par(futures)
        .andThen(records -> {
          apiContext.apiDefinition().endpoints().stream()
              .filter(e -> e instanceof HttpEndpoint)
              .map(e -> toJson(apiContext, (HttpEndpoint) e, records))
              .forEach(json -> {
                //transformer
                transformer(apiContext, json);
                //替换变量
                replace(apiContext, json);
                apiContext.addRequest(json);
              });
        })
        .andThen(records -> completeFuture.complete(apiContext))
        .onFailure(throwable -> {
//          throwable.printStackTrace();
          completeFuture.fail(throwable);
        });
  }

  private Future<Record> serviceFuture(String service) {
    //服务发现
    Future<Record> serviceFuture = Future.future();
    vertx.eventBus().<JsonObject>send("service.discovery.select", service, ar -> {
      if (ar.succeeded()) {
        JsonObject serviceJson = ar.result().body();
        Record record = new Record(serviceJson);
        serviceFuture.complete(record);
      } else {
        serviceFuture.fail(ar.cause());
      }
    });
    return serviceFuture;
  }

  private JsonObject toJson(ApiContext apiContext, HttpEndpoint endpoint, List<Record> records) {
    JsonObject request = new JsonObject();
    request.put("id", UUID.randomUUID().toString());
    request.put("name", endpoint.name());
    request.put("type", "http");
    request.put("path", endpoint.path());
    request.put("method", endpoint.method().name());
    JsonObject params = JsonUtils.mutlimapToJson(apiContext.params());
    request.put("params", params);
    JsonObject headers = JsonUtils.mutlimapToJson(apiContext.headers());
    request.put("headers", headers);
    if (apiContext.body() != null) {
      JsonObject body = apiContext.body().copy();
      request.put("body", body);
    }
    List<Record> recordList = records.stream()
        .filter(r -> endpoint.service().equalsIgnoreCase(r.getName()))
        .collect(Collectors.toList());
    if (records.isEmpty()) {
      throw SystemException.create(DefaultErrorCode.UNKOWN_REMOTE);
    }
    Record record = recordList.get(0);
    request.put("host",
        record.getLocation().getString("host"));
    request.put("port",
        record.getLocation().getInteger("port"));
    return request;
  }

  private void transformer(ApiContext apiContext, JsonObject request) {
    String name = request.getString("name");
    RequestTransformerPlugin plugin =
        (RequestTransformerPlugin) apiContext.apiDefinition()
            .plugin(RequestTransformerPlugin.NAME);

    RequestTransformer transformer = plugin.transformer(name);
    if (transformer != null) {
      tranformerParams(request.getJsonObject("params"), transformer);
      tranformerHeaders(request.getJsonObject("headers"), transformer);
      if (request.containsKey("body")) {
        tranformerBody(request.getJsonObject("body"), transformer);
      }
    }
  }

  private void replace(ApiContext apiContext, JsonObject request) {
    JsonObject newParams = new JsonObject();
    JsonObject params = request.getJsonObject("params", new JsonObject());
    for (String key : params.fieldNames()) {
      Object newVal = apiContext.getValueByKeyword(params.getValue(key));
      if (newVal != null) {
        newParams.put(key, newVal);
      }
    }
    request.put("params", newParams);

    JsonObject newHeaders = new JsonObject();
    JsonObject headers = request.getJsonObject("headers", new JsonObject());
    for (String key : headers.fieldNames()) {
      Object newVal = apiContext.getValueByKeyword(headers.getValue(key));
      if (newVal != null) {
        newHeaders.put(key, newVal);
      }
    }
    request.put("headers", newHeaders);
    if (request.containsKey("body")) {
      JsonObject newBody = new JsonObject();
      JsonObject body = request.getJsonObject("body");
      for (String key : body.fieldNames()) {
        Object newVal = apiContext.getValueByKeyword(body.getValue(key));
        if (newVal != null) {
          newBody.put(key, newVal);
        }
      }
      request.put("body", newBody);
    }

  }


  private void tranformerParams(JsonObject params,
                                RequestTransformer transformer) {
    transformer.paramRemoved().forEach(p -> params.remove(p));
    transformer.paramReplaced().forEach(entry -> params.put(entry.getKey(), entry.getValue()));
    transformer.paramAdded().forEach(entry -> params.put(entry.getKey(), entry.getValue()));
  }

  private void tranformerHeaders(JsonObject headers,
                                 RequestTransformer transformer) {
    transformer.headerRemoved().forEach(h -> headers.remove(h));
    transformer.headerReplaced().forEach(entry -> headers.put(entry.getKey(), entry.getValue()));
    transformer.headerAdded().forEach(entry -> headers.put(entry.getKey(), entry.getValue()));
  }

  private void tranformerBody(final JsonObject body,
                              RequestTransformer transformer) {
    if (body != null) {
      transformer.bodyRemoved().forEach(b -> body.remove(b));
    }
    //replace
    if (body != null) {
      transformer.bodyReplaced().forEach(entry -> body.put(entry.getKey(), entry.getValue()));
    }

    //add
    if (body != null) {
      transformer.bodyAdded().forEach(entry -> body.put(entry.getKey(), entry.getValue()));
    }
  }

}