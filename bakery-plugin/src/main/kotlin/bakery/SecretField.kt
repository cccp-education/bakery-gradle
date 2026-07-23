package bakery

sealed class SecretField {
    abstract val value: String

    data class Password(override val value: String) : SecretField()
    data class ApiKey(override val value: String) : SecretField()
    data class Token(override val value: String) : SecretField()
    data class DeviceKey(override val value: String) : SecretField()
}

fun maskSecret(field: SecretField): String =
    if (field.value.isBlank()) "(not set)"
    else when (field) {
        is SecretField.Password -> "***"
        is SecretField.ApiKey -> "${field.value.take(4)}***${field.value.takeLast(4)}"
        is SecretField.Token -> "***[${field.value.length} chars]"
        is SecretField.DeviceKey -> "ssh-ed25519***[${field.value.length} chars]"
    }