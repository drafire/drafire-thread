package com.drafire.drafirethread;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

@RunWith(SpringRunner.class)
@SpringBootTest
/**
 * 使用wait()、noticefy()、队列来做生产者和消费者
 * 实现了产品数量超过限制就自动停止的功能
 */
public class ProducerConsumerTest {
    //生产数量原子计数器
    private static AtomicLong producerCount = new AtomicLong();
    //消费数量计数器
    private static AtomicLong consumerCount = new AtomicLong();

    /**
     * 最大产品数量
     */
    private static Integer MAX_SIZE = 5;

    @Test
    public void test() throws InterruptedException {
        Queue buffer = new LinkedList();
        int maxSize = 2;

        Thread producer = new Producer(buffer, maxSize);
        Thread consumer = new Consumer(buffer, maxSize);

        producer.start();
        consumer.start();

        //主线程必须等待子线程结束后才能结束
        producer.join();
        consumer.join();
    }

    class Producer extends Thread {
        private Queue queue;
        private int maxSize;

        public Producer(Queue<Integer> queue, int maxSize) {
            this.queue = queue;
            this.maxSize = maxSize;
        }

        @Override
        public void run() {
            while (true) {
                synchronized (queue) {
                    while (queue.size() == maxSize) {
                        try {
                            System.out.println("生产端->队列已满，正在等待消费...");
                            queue.wait();

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }


                    long oldValue = producerCount.get();
                    long newValue = oldValue + 1;
                    while (producerCount.compareAndSet(oldValue, newValue)) {
                        int i = new Random().nextInt(100);
                        System.out.println("生产者增加数据->" + i);
                        queue.add(i);
                        queue.notifyAll();
                    }

                }

                if (producerCount.get() >= MAX_SIZE) {
                    break;
                }
            }
        }
    }

    class Consumer extends Thread {
        private Queue queue;
        private int maxSize;

        Consumer(Queue queue, int maxSize) {
            this.maxSize = maxSize;
            this.queue = queue;
        }

        @Override
        public void run() {
            while (true) {
                synchronized (queue) {
                    while (queue.isEmpty()) {
                        try {
                            System.out.println("消费者->队列已空，正在等待...");
                            queue.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    long oldValue = consumerCount.get();
                    long newValue = oldValue + 1;
                    while (consumerCount.compareAndSet(oldValue, newValue)) {
                        System.out.println("消费者消费->" + queue.remove());
                        queue.notifyAll();
                    }
                }

                if (consumerCount.get() >= MAX_SIZE) {
                    break;
                }
            }

        }
    }
}
