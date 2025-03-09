package org.demo.jobmaster.executor;

import org.demo.jobmaster.entity.Job;
import org.demo.jobmaster.entity.Runtime;

import java.util.function.Consumer;

public interface JobExecutor {

    Runtime getRuntime();

    void execute(Job job, Consumer<String> onSuccess, Consumer<String> onError);
}
