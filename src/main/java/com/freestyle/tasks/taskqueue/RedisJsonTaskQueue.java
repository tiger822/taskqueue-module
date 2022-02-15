package com.freestyle.tasks.taskqueue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.freestyle.netty.easynetty.common.Utils;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Created by rocklee on 2022/2/15 10:00
 */
public class RedisJsonTaskQueue<T > extends RedisTaskQueue<T>{
  public RedisJsonTaskQueue(String host,int port,String password, String taskName, int capacity,final TypeReference<T>  typeReference,int ...redisPoolSize) {
    super(host,port,password, taskName, capacity,redisPoolSize);
    JedisPoolConfig a;
    setConverter(Utils::toJsonString, s -> Utils.fromJson(s,typeReference));
  }
}
