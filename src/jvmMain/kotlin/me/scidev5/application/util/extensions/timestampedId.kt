package me.scidev5.application.util.extensions

import data.TimestampedId
import kotlin.random.Random


fun TimestampedId.Companion.generate() =
    TimestampedId(Random.nextInt(),(System.currentTimeMillis()/1000).toInt())