package com.code.codesandbox.service.utils;

import com.code.codesandbox.pojo.CodeResult;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.FutureTask;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * &#064;description:  a simple pool of code exec jobs
 * Created by Lxy on 2024/4/26 11:09
 */
public class JobPool extends Thread {
    private static final int MAX_POOL_SIZE = 5;
    private static Queue<FutureTask<CodeResult>> jobQueue = new ConcurrentLinkedDeque<>();
    private static final ReentrantLock lock = new ReentrantLock();
    private static final Condition condition = lock.newCondition();
    public void addJob(FutureTask<CodeResult> job) {
        jobQueue.add(job);
        condition.signalAll();
    }

    @Override
    public void run() {

        while (true) {
            try {
                sleep(200);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    }
}
