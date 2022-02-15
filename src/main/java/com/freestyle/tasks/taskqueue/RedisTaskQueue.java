package com.freestyle.tasks.taskqueue;

import com.freestyle.tasks.taskqueue.interfaces.TaskQueue;
import redis.clients.jedis.Jedis;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Created by rocklee on 2022/2/14 14:27
 */
public class RedisTaskQueue<T > implements TaskQueue<T> ,AutoCloseable{
  private Jedis jedis;
  private final int capacity;
  private final String taskName;
  private Function<T,String> serializer;
  private Function<String,T> deSerializer;
  private static final ConcurrentHashMap<String,Semaphore> semaphore=new ConcurrentHashMap<>();
  private  Semaphore getSemaphore(){
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
  }
  public RedisTaskQueue(String host,int port,String password,String taskName,int capacity) {
    this.jedis=new Jedis(host,port);
    this.jedis.auth(password);
    this.jedis.connect();
    this.taskName=taskName;
    this.capacity=capacity;
  }

  public boolean offer(T task, long... milliSecondsToWait) {
    boolean flag;
    Semaphore lock=getSemaphore();
    if (milliSecondsToWait.length==0){
      flag=lock.tryAcquire();
    }
    else{
      try {
        flag=lock.tryAcquire(milliSecondsToWait[0], TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        e.printStackTrace();
        flag=false;
      }
    }
    if (!flag)return false;
    String value=serializer.apply(task);
    jedis.rpush(taskName,value);
    return true;
  }

  public <T> T poll(long milliSecondsToWait) {
    int s=(int)(milliSecondsToWait/1000);
    List<String> list=jedis.blpop(s<=0?1:s,taskName);
    T ret= list==null||list.size()==0?null: (T) deSerializer.apply(list.get(1));
    if (ret!=null){
      //System.out.println("发送取出通知");
      getSemaphore().release();
    }
    return ret;
  }

  public int count() {
    return capacity- getSemaphore().availablePermits();
  }

  @Override
  public long queueCount() {
    return jedis.llen(taskName).longValue();
  }

  public int remainingCapacity() {
    return getSemaphore().availablePermits();
  }

  @Override
  public TaskQueue<T> setConverter(Function<T, String> serializer, Function<String, T> deSerializer) {
    this.serializer=serializer;
    this.deSerializer=deSerializer;
    return this;
  }

  @Override
  public void clear() {
    jedis.del(taskName);
  }

  @Override
  public void close() {
    jedis.close();
  }
}
