package me.scidev5.application.push

import data.push.PushSubscriptionInfoKT
import nl.martijndwars.webpush.Subscription

fun PushSubscriptionInfoKT.convert() = Subscription(
    endpoint,
    Subscription.Keys(
        p256dh,
        auth,
    )
)