package util

import wsTransaction.KWSTransactor

open class WithTxr {
    lateinit var txr: KWSTransactor

    fun <T> T.withTxr(txr: KWSTransactor):T where T : WithTxr {
        this.txr = txr
        return this
    }
}