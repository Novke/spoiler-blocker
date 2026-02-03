package trif.novica.spoilerchecker.data.model

data class NotificationInfo(
    val key: String,
    val packageName: String,
    val title: String?,
    val text: String?,
    val postTime: Long
) {
    val fullText: String
        get() = listOfNotNull(title, text).joinToString(" ").trim()
}
