package tech.hombre.bluetoothchatter.data.service.message

enum class PayloadType(val value: Int) {

    TEXT(0),
    IMAGE(1),
    FILE(2),
    AUDIO(3);

    companion object {
        fun from(findValue: Int) = values().first { it.value == findValue }
    }
}
