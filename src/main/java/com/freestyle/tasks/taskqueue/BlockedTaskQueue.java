package com.freestyle.tasks.taskqueue;

import com.freestyle.tasks.taskqueue.interfaces.TaskQueue;

import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 内存版任务队列
 * Created by rocklee on 2022/2/15 14:32
 */
public class BlockedTaskQueue<T> extends ArrayBlockingQueue<T> implements TaskQueue<T> {
  private int capacity;
  public BlockedTaskQueue(int capacity) {
    super(capacity);
  }

  public BlockedTaskQueue(int capacity, boolean fair) {
    super(capacity, fair);
  }

  public BlockedTaskQueue(int capacity, boolean fair, Collection<? extends T> c) {
    super(capacity, fair, c);
    this.capacity=capacity;
  }

  @Override
  public boolean offer(T task, long... milliSecondsToWait) {
    if (milliSecondsToWait.length==0){
      if (size()==capacity) return false;
      return offer(task);
    }
    try {
      return offer(task,milliSecondsToWait[0], TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      e.printStackTrace();
      return false;
    }
  }

  @Override
  public <T1> T1 poll(long milliSecondsToWait) {
    try {
      return (T1) super.poll(milliSecondsToWait,TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
     // e.printStackTrace();
      return null;
    }
  }

  @Override
  public int count() {
    return super.size();
  }

  @Override
  public long queueCount() {
    return count();
  }

  @Override
  public int remainingCapacity() {
    return capacity-count();
  }

  @Override
  public TaskQueue<T> setConverter(Function<T, String> serializer, Function<String, T> deSerializer) {
    return this;
  }

  @Override
  public void clear() {
    super.clear();
  }

  @Override
  public void close() {

  }
}
