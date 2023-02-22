package wsTransaction

data class KWSTransactionHandle(
    val key: String,
    val block: suspend KWSTransactor.Transaction.Scope.()->Unit
)