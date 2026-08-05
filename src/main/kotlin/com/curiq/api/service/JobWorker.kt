package com.curiq.api.service

import com.curiq.api.model.Job

interface JobWorker {
    fun canHandle(job: Job): Boolean
    fun process(job: Job)
}
