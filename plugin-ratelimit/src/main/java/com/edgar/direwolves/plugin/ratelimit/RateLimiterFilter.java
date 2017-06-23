package com.edgar.direwolves.plugin.ratelimit;

import com.google.common.base.Strings;

import com.edgar.direwolves.core.cache.RedisProvider;
import com.edgar.direwolves.core.dispatch.ApiContext;
import com.edgar.direwolves.core.dispatch.Filter;
import com.edgar.util.exception.DefaultErrorCode;
import com.edgar.util.exception.SystemException;
import com.edgar.util.vertx.JsonUtils;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ProxyHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Created by Edgar on 2017/1/22.
 *
 * @author Edgar  Date 2017/1/22
 */
public class RateLimiterFilter implements Filter {

  private static final Logger LOGGER = LoggerFactory.getLogger(RateLimiterFilter.class);

  private final String namespace;

  private final RedisProvider redisProvider;

  private final Vertx vertx;

  private final List<RateLimiterPolicy> policies = new ArrayList<>();

  RateLimiterFilter(Vertx vertx, JsonObject config) {
    this.vertx = vertx;
    this.namespace = config.getString("namespace", "");
    String address = RedisProvider.class.getName();
    if (!Strings.isNullOrEmpty(namespace)) {
      address = namespace + "." + address;
    }
    this.redisProvider = ProxyHelper.createProxy(RedisProvider.class, vertx, address);

    JsonObject rateLimter = config.getJsonObject("rate.limiter", new JsonObject());
    for (String name : rateLimter.fieldNames()) {
      JsonObject jsonObject = rateLimter.getJsonObject(name);
      String subject = jsonObject.getString("key");
      long limit = jsonObject.getLong("limit");
      long interval = jsonObject.getLong("interval");
      String unit = jsonObject.getString("unit").toUpperCase();
      TimeUnit timeUnit = TimeUnit.valueOf(unit);
      RateLimiterPolicy policy = RateLimiterPolicy.create(name, subject, limit, interval, timeUnit);
      policies.add(policy);
    }
  }

  @Override
  public String type() {
    return PRE;
  }

  @Override
  public int order() {
    return 20000;
  }

  @Override
  public boolean shouldFilter(ApiContext apiContext) {
    RateLimiterPlugin plugin =
            (RateLimiterPlugin) apiContext.apiDefinition()
                    .plugin(RateLimiterPlugin.class.getSimpleName());
    return plugin != null && !plugin.rateLimiters().isEmpty();
  }

  @Override
  public void doFilter(ApiContext apiContext, Future<ApiContext> completeFuture) {
    RateLimiterPlugin plugin =
            (RateLimiterPlugin) apiContext.apiDefinition()
                    .plugin(RateLimiterPlugin.class.getSimpleName());
    JsonArray limiter = new JsonArray();
    plugin.rateLimiters().forEach(r -> {
      Optional<RateLimiterPolicy> optional = policies.stream()
              .filter(p -> p.name().equals(r.name()))
              .findFirst();
      if (optional.isPresent()) {
        RateLimiterPolicy policy = optional.get();
        String subject = policy.key();
        if (subject.startsWith("$")
            && apiContext.getValueByKeyword(subject) != null) {
          subject = apiContext.getValueByKeyword(subject).toString();
        }
        JsonObject rule = new JsonObject()
                .put("name", r.name())
                .put("subject", r.name() + "." + subject)
                .put("refillAmount", policy.limit())
                .put("burst", r.burst())
                .put("refillTime", policy.unit().toMillis(policy.interval()));
        limiter.add(rule);
      }
    });
    if (limiter.size() == 0) {
      completeFuture.complete(apiContext);
      return;
    }
    redisProvider.acquireToken(limiter, ar -> {
      if (ar.failed()) {
        completeFuture.fail(ar.cause());
        return;
      }
      JsonObject result = ar.result();
      if (result.getBoolean("passed", true)) {
        //增加响应头
        JsonArray details = result.getJsonArray("details", new JsonArray());
        for (int i = 0; i < details.size(); i++) {
          JsonObject detail = details.getJsonObject(i);
          apiContext.addVariable("resp.header:X-Rate-Limit-Limit", detail.getLong("limit"));
          apiContext.addVariable("resp.header:X-Rate-Limit-Remaining", detail.getLong("remaining"));
          apiContext.addVariable("resp.header:X-Rate-Limit-Reset",
                                 Math.round(detail.getLong("reset") / 1000));
        }
        completeFuture.complete(apiContext);
      } else {
        JsonArray details = result.getJsonArray("details", new JsonArray());
        SystemException se = SystemException.create(DefaultErrorCode.TOO_MANY_REQ);
        for (int i = 0; i < details.size(); i++) {
          JsonObject detail = details.getJsonObject(i);
          se.set("resp.header:X-Rate-Limit-Limit", detail.getLong("limit"));
          se.set("resp.header:X-Rate-Limit-Remaining", detail.getLong("remaining"));
          se.set("resp.header:X-Rate-Limit-Reset",
                                 Math.round(detail.getLong("reset") / 1000));
        }
        completeFuture.fail(se);
      }
    });

  }
}
