package com.edgar.direwolves.plugin.arg;

import com.edgar.direwolves.core.dispatch.Filter;
import com.edgar.direwolves.core.dispatch.FilterFactory;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * Created by edgar on 16-10-28.
 */
public class UrlArgFilterFactory implements FilterFactory {

  @Override
  public String name() {
    return UrlArgFilter.class.getSimpleName();
  }

  @Override
  public Filter create(Vertx vertx, JsonObject config) {
    return new UrlArgFilter();
  }
}
