package com.freestyle.tasks.taskqueue;

import com.freestyle.tasks.taskqueue.interfaces.TaskQueue;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

import javax.swing.*;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Created by rocklee on 2022/2/14 14:27
 */
public class RedisTaskQueue<T > implements TaskQueue<T> ,AutoCloseable{
  private final int capacity;
  private final String taskName;
  private Function<T,String> serializer;
  private Function<String,T> deSerializer;
  private final Semaphore semaphore;
  private final JedisPoolConfig jedisPoolConfig;
  private JedisPool jedisPool;
  private final ConcurrentLinkedQueue<Jedis> jedisQueue=new ConcurrentLinkedQueue<>();
  private final ThreadLocal<Jedis> jedisThreadLocal=new ThreadLocal<Jedis>(){
    @Override
    protected Jedis initialValue() {
       Jedis jedis=jedisPool.getResource();
       jedisQueue.offer(jedis);
       return jedis;
    }
  };
  private Jedis getJedis(){
    return jedisThreadLocal.get();
  }
  /*private  Semaphore getSemaphore(){
    if (semaphore.containsKey(taskName)){
      return semaphore.get(taskName);
    }
    Semaphore inst=new Semaphore(capacity);
    semaphore.put(taskName,inst);
    Long cc=this.jedis.llen(taskName);
    if (cc!=null&&cc>0){
      try {
        inst.acquire(cc.intValue());
       // System.out.println("当前可用任务量："+inst.availablePermits());
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    return inst;
  }*/
  public RedisTaskQueue(String host,int port,String password,String taskName,int capacity,int... redisPoolSize) {
    this.taskName=taskName;
    this.capacity=capacity;
    jedisPoolConfig=new JedisPoolConfig();
    if (redisPoolSize.length==0){
      jedisPoolConfig.setMaxTotal(redisPoolSize[0]);
    }
    else{
      jedisPoolConfig.setMaxTotal(50);
    }
    jedisPool=new JedisPool(jedisPoolConfig,host,port, Protocol.DEFAULT_TIMEOUT,password);
    semaphore=new Semaphore(capacity);
  }

  public boolean offer(T task, long... milliSecondsToWait) {
    boolean flag;
    if (milliSecondsToWait.length==0){
      flag=semaphore.tryAcquire();
    }
    else{
      try {
        flag=semaphore.tryAcquire(milliSecondsToWait[0], TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        e.printStackTrace();
        flag=false;
      }
    }
    if (!flag)return false;
    String value=serializer.apply(task);
    getJedis().rpush(taskName,value);
    return true;
  }

  public <T> T poll(long milliSecondsToWait) {
    int s=(int)(milliSecondsToWait/1000);
    List<String> list=getJedis().blpop(s<=0?1:s,taskName);
    T ret= list==null||list.size()==0?null: (T) deSerializer.apply(list.get(1));
    if (ret!=null){
      //System.out.println("发送取出通知");
      semaphore.release();
    }
    return ret;
  }

  public int count() {
    return capacity- semaphore.availablePermits();
  }

  @Override
  public long queueCount() {
    return getJedis().llen(taskName).longValue();
  }

  public int remainingCapacity() {
    return semaphore.availablePermits();
  }

  @Override
  public TaskQueue<T> setConverter(Function<T, String> serializer, Function<String, T> deSerializer) {
    this.serializer=serializer;
    this.deSerializer=deSerializer;
    return this;
  }

  @Override
  public void clear() {
    getJedis().del(taskName);
  }

  @Override
  public void close() {
    if (jedisPool!=null){
      jedisPoolConfig.setTestOnReturn(false);
      jedisQueue.forEach(j->{
        j.close();
      });
      jedisPool.destroy();
      jedisPool=null;
    }
  }
}
