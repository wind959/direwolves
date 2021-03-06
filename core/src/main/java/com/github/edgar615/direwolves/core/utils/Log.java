package com.github.edgar615.direwolves.core.utils;

import com.google.common.base.Strings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统一的日志格式.
 * [应用,traceId] [类型] [事件] [k1:v1;k2:v2;…]  message
 *
 * @author Edgar  Date 2017/7/4
 */
public class Log {

  private final Logger defaultLogger = LoggerFactory.getLogger("root");

  /**
   * 数据
   */
  private final Map<String, Object> data = new HashMap<>();

  private final List<Object> args = new ArrayList<>();

  private Logger logger = defaultLogger;

  /**
   * 应用
   */
  private String application = "api-gateway";

  /**
   * 方法或者事件
   */
  private String event;

  private LogType logType = LogType.LOG;

  /**
   * 简要描述
   */
  private String message;

  /**
   * 跟踪ID
   */
  private String traceId;

  /**
   * 异常
   */
  private Throwable throwable;

  /**
   * 日志中是否输出应用
   */
  private boolean logApplication;

  private Log(Logger logger) {
    this.logger = logger;
  }

  public static Log create(Logger logger) {
    return new Log(logger);
  }

  public void trace() {
    try {
      if (logger.isTraceEnabled()) {
        LogData logData = new LogData().get();
        logger.trace(logData.getLogFormat(), logData.getLogArgs().toArray());
      }
    } catch (Exception e) {
      defaultLogger.error("log error", e);
    }
  }

  public void debug() {
    try {
      if (logger.isDebugEnabled()) {
        LogData logData = new LogData().get();
        logger.debug(logData.getLogFormat(), logData.getLogArgs().toArray());
      }
    } catch (Exception e) {
      defaultLogger.error("log error", e);
    }
  }

  public void info() {
    try {
      if (logger.isInfoEnabled()) {
        LogData logData = new LogData().get();
        logger.info(logData.getLogFormat(), logData.getLogArgs().toArray());
      }
    } catch (Exception e) {
      defaultLogger.error("log error", e);
    }
  }

  public void warn() {
    try {
      if (logger.isWarnEnabled()) {
        LogData logData = new LogData().get();
        logger.warn(logData.getLogFormat(), logData.getLogArgs().toArray());
      }
    } catch (Exception e) {
      defaultLogger.error("log error", e);
    }
  }

  public void error() {
    try {
      if (logger.isErrorEnabled()) {
        LogData logData = new LogData().get();
        logger.error(logData.getLogFormat(), logData.getLogArgs().toArray());
      }
    } catch (Exception e) {
      defaultLogger.error("log error", e);
    }
  }

  public Log setLogType(LogType logType) {
    this.logType = logType;
    return this;
  }

  public Log setLogApplication(boolean logApplication) {
    this.logApplication = logApplication;
    return this;
  }

  public Log setApplication(String application) {
    this.application = application;
    return this;
  }

  public Log setEvent(String event) {
    this.event = event;
    return this;
  }

  public Log setMessage(String message) {
    this.message = message;
    return this;
  }

  public Log setThrowable(Throwable throwable) {
    this.throwable = throwable;
    return this;
  }

  public Log setTraceId(String traceId) {
    this.traceId = traceId;
    return this;
  }

  public Log addData(String key, Object data) {
    this.data.put(key, data);
    return this;
  }

  public Log addDatas(Map<String, Object> data) {
    this.data.putAll(data);
    return this;
  }

  public Log addArg(Object arg) {
    this.args.add(arg);
    return this;
  }

  private class LogData {
    private String logFormat;

    private List<Object> logArgs;

    public String getLogFormat() {
      return logFormat;
    }

    public List<Object> getLogArgs() {
      return logArgs;
    }

    public LogData get() {
      logFormat = "[{}] [{}] [{}]";

      logArgs = new ArrayList<>();
      if (logApplication) {
        logFormat = "[{},{}] " + logFormat;
        logArgs.add(application);
        if (traceId == null) {
          logArgs.add("");
        } else {
          logArgs.add(traceId);
        }
      }
      logArgs.add(logType.name());
      logArgs.add(event == null ? "log" : event);

      if (data.isEmpty()) {
        logArgs.add("no data");
      } else {
        logArgs.add(dataFormat(data));
      }

      if (!Strings.isNullOrEmpty(message)) {
        logFormat += " " + message;
        logArgs.addAll(args);
      }

      if (throwable != null) {
        logArgs.add(throwable);
      }
      return this;
    }

    private String dataFormat(Map<String, Object> data) {
      StringBuilder sb = new StringBuilder();
      for (String field : data.keySet()) {
        sb.append(field)
                .append(":")
                .append(data.get(field))
                .append(";");
      }
      return sb.toString();
    }
  }
}
