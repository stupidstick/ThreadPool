package org.example.threadpool;

public interface Request {
    void execute();
    void onFinish();
    void onException();
    void onCancel();
}

