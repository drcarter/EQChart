package com.magimon.eq.waveform

/**
 * PCM 샘플을 최근 구간만 유지하기 위한 고정 길이 링버퍼.
 *
 * 내부 저장소 용량을 초과하면 가장 오래된 샘플부터 자동으로 덮어쓴다.
 */
internal class PcmRingBuffer(capacity: Int) {
    private var data = ShortArray(capacity.coerceAtLeast(1))
    private var writeIndex = 0
    private var size = 0

    /**
     * 버퍼를 비운다.
     */
    @Synchronized
    fun clear() {
        writeIndex = 0
        size = 0
    }

    /**
     * 버퍼 용량을 변경한다.
     *
     * 용량 축소 시 최신 샘플을 우선 보존한다.
     */
    @Synchronized
    fun setCapacity(newCapacity: Int) {
        val target = newCapacity.coerceAtLeast(1)
        if (target == data.size) return

        val snapshot = snapshotUnsafe()
        data = ShortArray(target)
        writeIndex = 0
        size = 0

        val from = (snapshot.size - target).coerceAtLeast(0)
        append(snapshot, from, snapshot.size)
    }

    /**
     * 버퍼 내용을 전달된 샘플로 완전히 교체한다.
     *
     * 입력이 용량보다 크면 뒤쪽(최신 구간)만 유지한다.
     */
    @Synchronized
    fun setAll(samples: ShortArray) {
        clear()
        val from = (samples.size - data.size).coerceAtLeast(0)
        append(samples, from, samples.size)
    }

    /**
     * 샘플 구간을 버퍼 뒤에 추가한다.
     *
     * @param samples 입력 PCM 샘플 배열
     * @param fromIndex 포함 시작 인덱스
     * @param toIndex 제외 끝 인덱스
     */
    @Synchronized
    fun append(samples: ShortArray, fromIndex: Int = 0, toIndex: Int = samples.size) {
        val start = fromIndex.coerceIn(0, samples.size)
        val end = toIndex.coerceIn(start, samples.size)
        if (start >= end) return

        for (idx in start until end) {
            data[writeIndex] = samples[idx]
            writeIndex = (writeIndex + 1) % data.size
            if (size < data.size) size++
        }
    }

    /**
     * 현재 저장된 샘플을 시간 순서대로 복사해 반환한다.
     */
    @Synchronized
    fun snapshot(): ShortArray = snapshotUnsafe()

    /**
     * 내부 버퍼 최대 용량(샘플 수)을 반환한다.
     */
    @Synchronized
    fun capacity(): Int = data.size

    /**
     * 현재 저장된 샘플 수를 반환한다.
     */
    @Synchronized
    fun size(): Int = size

    /**
     * 동기화가 걸린 상태에서 스냅샷을 생성한다.
     */
    private fun snapshotUnsafe(): ShortArray {
        if (size == 0) return ShortArray(0)

        val out = ShortArray(size)
        val start = (writeIndex - size + data.size) % data.size
        for (i in 0 until size) {
            out[i] = data[(start + i) % data.size]
        }
        return out
    }
}
