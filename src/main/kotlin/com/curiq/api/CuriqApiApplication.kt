package com.curiq.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CuriqApiApplication

fun main(args: Array<String>) {
    runApplication<CuriqApiApplication>(*args)
}
