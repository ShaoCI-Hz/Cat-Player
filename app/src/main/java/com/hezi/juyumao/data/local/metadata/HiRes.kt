package com.hezi.juyumao.data.local.metadata

/**
 * Hi-Res 统一判定（JAS 近似定义）
 *
 * | 条件 | 判定 |
 * |------|------|
 * | 采样率 > 48kHz | HiRes |
 * | 位深 >= 24bit | HiRes |
 * | 扩展名 in {dsf, dff} | HiRes（DSD） |
 * | 其余（含 44.1/48kHz 16bit FLAC） | 非 HiRes |
 *
 * 本地扫描与 SMB 扫描共用，消除两套逻辑。
 */
object HiRes {
    private val DSD_EXTENSIONS = setOf("dsf", "dff")

    /**
     * @param sampleRate    Hz，0 表示未知
     * @param bitsPerSample 位深，0 表示未知
     * @param fileExtension 文件扩展名（不含点，可带路径），用于 DSD 判定兜底
     */
    fun isHiRes(
        sampleRate: Int = 0,
        bitsPerSample: Int = 0,
        fileExtension: String? = null,
    ): Boolean {
        if (sampleRate > 48_000) return true
        if (bitsPerSample >= 24) return true
        val ext = fileExtension?.substringAfterLast('.', "")?.lowercase() ?: ""
        return ext in DSD_EXTENSIONS
    }
}
