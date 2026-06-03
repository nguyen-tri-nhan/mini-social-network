package com.nhan.social.post.job

import com.nhan.social.post.service.CounterService
import io.quarkus.scheduler.Scheduled
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class CounterFlushJob(private val counterService: CounterService) {

    @Scheduled(every = "30s")
    fun flush() = counterService.flushToDb()
}
