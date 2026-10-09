package com.example.giaodien.data.model

/** Accept generated upload paths, historical bare filenames and absolute remote images. */
fun menuImageUrl(image: String, baseUrl: String): String {
    if (image.isBlank()) return ""
    if (image.startsWith("https://") || image.startsWith("http://")) return image
    val relative = image.trimStart('/')
    return baseUrl.trimEnd('/') + "/" + if (relative.startsWith("uploads/") || relative.startsWith("demo-assets/")) relative else "uploads/$relative"
}
