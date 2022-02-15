package com.freestyle.tasks.taskqueue;

import java.io.Serializable;

/**
 * Created by rocklee on 2022/2/15 11:18
 */
public abstract class AbstractTask implements Serializable {
  private long id;

  public AbstractTask() {
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }
}
