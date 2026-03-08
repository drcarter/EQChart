package com.magimon.eq.waveform

/**
 * Fixed-length ring buffer that keeps only the most recent PCM samples.
 *
 * When capacity is exceeded, the oldest samples are overwritten first.
 */
internal class PcmRingBuffer(capacity: Int) {
    private var data = ShortArray(capacity.coerceAtLeast(1))
    private var writeIndex = 0
    private var size = 0

    /**
     * Clears the buffer.
     */
    @Synchronized
    fun clear() {
        writeIndex = 0
        size = 0
    }

    /**
     * Changes buffer capacity.
     *
     * On shrink, the newest samples are preserved first.
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
     * Replaces buffer contents with the provided sample array.
     *
     * If input is larger than capacity, only the tail (most recent part) is kept.
     */
    @Synchronized
    fun setAll(samples: ShortArray) {
        clear()
        val from = (samples.size - data.size).coerceAtLeast(0)
        append(samples, from, samples.size)
    }

    /**
     * Appends a sample range to the end of the buffer.
     *
     * @param samples Input PCM sample array
     * @param fromIndex Inclusive start index
     * @param toIndex Exclusive end index
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
     * Returns a time-ordered copy of currently stored samples.
     */
    @Synchronized
    fun snapshot(): ShortArray = snapshotUnsafe()

    /**
     * Returns maximum buffer capacity in sample count.
     */
    @Synchronized
    fun capacity(): Int = data.size

    /**
     * Returns current number of stored samples.
     */
    @Synchronized
    fun size(): Int = size

    /**
     * Builds a snapshot while synchronization is held.
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
