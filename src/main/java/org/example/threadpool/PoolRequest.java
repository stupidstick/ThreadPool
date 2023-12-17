package org.example.threadpool;

public abstract class PoolRequest implements Request {
    @Override
    public void execute() {
        System.out.println("executing");
    }

    @Override
    public void onFinish() {
        System.out.println("finish");
    }

    @Override
    public void onException() {
        System.out.println("exception");
    }

    @Override
    public void onCancel() {
        System.out.println("cancel");
    }
}
