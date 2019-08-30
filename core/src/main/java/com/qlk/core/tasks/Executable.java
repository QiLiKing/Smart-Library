package com.qlk.core.tasks;

public interface Executable<Executor> {
    void execute(Executor executor);
}