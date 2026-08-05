package com.curiq.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class CuriqApiApplication

fun main(args: Array<String>) {
    runApplication<CuriqApiApplication>(*args)
}
