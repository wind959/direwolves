package com.github.edgar615.direwolves.plugin.transformer;

import com.github.edgar615.direwolves.core.dispatch.Filter;
import com.github.edgar615.direwolves.core.dispatch.FilterFactory;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * response_transfomer.
 * <p>
 * </pre>
 * <p>
 * Created by edgar on 16-9-20.
 */
public class ResponseTransformerFilterFactory implements FilterFactory {


  @Override
  public String name() {
    return ResponseTransformerFilter.class.getSimpleName();
  }

  @Override
  public Filter create(Vertx vertx, JsonObject config) {
    return new ResponseTransformerFilter(config);
  }
}