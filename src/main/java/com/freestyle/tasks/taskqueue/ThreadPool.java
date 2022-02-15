package com.freestyle.tasks.taskqueue;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by rocklee on 2022/2/15 10:53
 */
public class ThreadPool extends ThreadPoolExecutor {
  /***
   *
   * @param fixThreadSize  最低核心线程数
   * @param maxThreadSize  最大线程数
   * @param extThreadTimeOut 当线程数大于核心线程数的那些线程，过多少秒被回收，0为即时回收
   * @param taskQueueSize 任务队列的长度
   */
  public ThreadPool(int fixThreadSize,int maxThreadSize,int extThreadTimeOut,int taskQueueSize){
    super(fixThreadSize,maxThreadSize,extThreadTimeOut, TimeUnit.SECONDS,new ArrayBlockingQueue<>(taskQueueSize));
  }
}
