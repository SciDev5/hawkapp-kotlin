package util

import wsTransaction.KWSTransactor

interface WithTxr {
    var txr: KWSTransactor
    fun withTxrI(txr: KWSTransactor): WithTxr

    companion object {
        operator fun invoke():WithTxr = WithTxrImpl()
    }
}
fun <T> T.withTxr(txr: KWSTransactor):T where T : WithTxr = this.also { withTxrI(txr) }
private class WithTxrImpl : WithTxr {
    override lateinit var txr: KWSTransactor

    override fun withTxrI(txr: KWSTransactor): WithTxr {
        this.txr = txr
        return this
    }
}