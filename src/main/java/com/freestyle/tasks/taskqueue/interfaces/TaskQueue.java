package com.freestyle.tasks.taskqueue.interfaces;

import com.freestyle.tasks.taskqueue.AbstractTask;

import java.util.function.Function;

/**
 * Created by rocklee on 2022/2/14 14:19
 */
public interface TaskQueue<T> {
  boolean offer(T task, long ...milliSecondsToWait);
  <T> T poll(long milliSecondsToWait);
  //获取任务队列长度
  int count();
  //获取真实的任务队列长度，理应与count()一样
  long queueCount();
  int remainingCapacity();
  TaskQueue<T> setConverter(Function<T,String> serializer, Function<String,T> deSerializer);
  void clear();
  void close() ;
}
