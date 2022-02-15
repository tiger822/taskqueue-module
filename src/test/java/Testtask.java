import com.fasterxml.jackson.core.type.TypeReference;
import com.freestyle.tasks.taskqueue.AbstractTask;
import com.freestyle.tasks.taskqueue.BlockedTaskQueue;
import com.freestyle.tasks.taskqueue.RedisJsonTaskQueue;
import com.freestyle.tasks.taskqueue.ThreadPool;
import com.freestyle.tasks.taskqueue.interfaces.TaskQueue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by rocklee on 2022/2/15 11:03
 */
public class Testtask {
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  private static class Order extends AbstractTask {
    private long orderId;
    private BigDecimal amount;

  }
  //因为处理订单需要1秒时间，耗时很久，所以开20个线程并发处理(实际上线应该设置更多),订单队列再多也必须通过这20个处理线程顺序处理。
  private ThreadPool threadPool=new ThreadPool(20,20,1800,4);
  @Before
  public void init(){

  }
  @After
  public void destroy(){
  }
  @Test
  public void testTask() throws InterruptedException {
    AtomicBoolean done= new AtomicBoolean(false);
    TaskQueue<Order> queue=new BlockedTaskQueue<>(200);
    //建立4个并行的订单处理线程
    List<CompletableFuture<Void>> consumeTasks=new ArrayList<>();
    for (int i=0;i<threadPool.getCorePoolSize();i++){
      consumeTasks.add(CompletableFuture.runAsync(()->{
        do {
          Order order= queue.poll(1000);
          if (order!=null){
            try {
              //做实质订单处理
              Thread.sleep(1000);
            } catch (Exception e) {
            }
            System.out.println(String.format("完成%d,当前线程:%d,剩余任务:%d,队列真实任务:%d",order.getOrderId(),Thread.currentThread().getId(),queue.count(),queue.queueCount()));
          }
        }while (!done.get());
      },threadPool));
    }

    //提供订单任务
    for (int i=0;i<300;i++){
      if (!queue.offer(new Order(i, BigDecimal.valueOf(i)),2000)){
        System.out.println("投递任务失败:"+i+",当前任务队列数:"+queue.count());
      }
      else{
        System.out.println("投递任务成功:"+i+",当前任务队列数:"+queue.count());
      }
     // Thread.sleep(200);
      //System.out.println("队列数:"+queue.count());
    }
    do {
      Thread.sleep(1000);
    }while(queue.count()>0);
    done.set(true);
    CompletableFuture.allOf(consumeTasks.toArray(new CompletableFuture[0])).join();
    queue.close();
    System.out.println("完成");
  }
  @Test
  public void testRedisTask() throws InterruptedException {
    AtomicBoolean done= new AtomicBoolean(false);
    TaskQueue<Order> queue=new RedisJsonTaskQueue<Order>("localhost", 6379, "12345678", "order-queue", 200, new TypeReference<Order>() {
    },threadPool.getCorePoolSize());
    //建立4个并行的订单处理线程
    List<CompletableFuture<Void>> consumeTasks=new ArrayList<>();
    for (int i=0;i<threadPool.getCorePoolSize();i++){
      consumeTasks.add(CompletableFuture.runAsync(()->{
        do {
          Order order= queue.poll(1000);
          if (order!=null){
            try {
              Thread.sleep(1000);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
            System.out.println(String.format("完成%d,当前线程:%d,剩余任务:%d,队列真实任务:%d",order.getOrderId(),Thread.currentThread().getId(),queue.count(),queue.queueCount()));
          }
        }while (!done.get());
      },threadPool));
    }

    //提供订单任务
    for (int i=0;i<1000;i++){
      if (!queue.offer(new Order(i, BigDecimal.valueOf(i)),2000)){
        System.out.println("投递任务失败:"+i+",当前任务队列数:"+queue.count());
      }
      else{
        System.out.println("投递任务成功:"+i+",当前任务队列数:"+queue.count());
      }
      //System.out.println("队列数:"+queue.count());
    }
    do {
      Thread.sleep(1000);
    }while(queue.count()>0);
    done.set(true);
    CompletableFuture.allOf(consumeTasks.toArray(new CompletableFuture[0])).join();
    queue.close();
    System.out.println("完成");
  }
}
