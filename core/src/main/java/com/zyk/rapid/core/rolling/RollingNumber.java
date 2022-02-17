package com.zyk.rapid.core.rolling;

import com.zyk.gateway.common.util.Pair;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantLock;

public class RollingNumber {

    private static final Time ACTUAL_TIME = new ActualTime();

    private final Time time;

    final int timeInMilliseconds;

    final int numberOfBuckets;

    final int bucketSizeInMillisecond;

    final String uniqueKey;

    public final BucketCircularArray buckets;

    private final CumulativeSum cumulativeSum;

    private final ReentrantLock lock = new ReentrantLock();

    public RollingNumber(int timeInMilliseconds, int numberOfBuckets, String uniqueKey, BlockingQueue<Pair<String, Long>> blockingQueue) {
        this(ACTUAL_TIME, timeInMilliseconds, numberOfBuckets, uniqueKey, blockingQueue);
    }

    public RollingNumber(Time time, int timeInMilliseconds, int numberOfBuckets, String uniqueKey, BlockingQueue<Pair<String, Long>> blockingQueue) {
        this.time = time;
        this.timeInMilliseconds = timeInMilliseconds;
        this.numberOfBuckets = numberOfBuckets;
        if (timeInMilliseconds % numberOfBuckets != 0) {
            throw new IllegalArgumentException("The timeInMilliseconds must divide equally into numberOfBuckets. For example 1000/10 is ok, 1000/11 is not.");
        }
        bucketSizeInMillisecond = timeInMilliseconds / numberOfBuckets;
        this.uniqueKey = uniqueKey;
        this.buckets = new BucketCircularArray(numberOfBuckets);
        this.cumulativeSum = new CumulativeSum(uniqueKey, blockingQueue);
    }

    public void increment(RollingNumberEvent type) {
        getCurrentBucket().getAdder(type).increment();
    }

    public void recordRT(int rt) {
        LongAdder longAdder = getCurrentBucket().getRTBottle().computeIfAbsent(rt, k -> new LongAdder());
        longAdder.increment();
    }

    public void add(RollingNumberEvent type, long value) {
        getCurrentBucket().getAdder(type).add(value);
    }

    public void updateRollingMax(RollingNumberEvent type, long value) {
        getCurrentBucket().getMaxUpdater(type).update(value);
    }

    public long getRollingSum(RollingNumberEvent type) {
        Bucket currentBucket = getCurrentBucket();
        if (currentBucket == null) {
            return 0;
        }
        long sum = 0;
        for (Bucket bucket : buckets) {
            sum += bucket.getAdder(type).sum();
        }
        return sum;
    }

    public long getValueOfLatestBucket(RollingNumberEvent type) {
        Bucket currentBucket = getCurrentBucket();
        if (currentBucket == null) return 0;
        return currentBucket.get(type);
    }

    public long[] getValues(RollingNumberEvent type) {
        Bucket currentBucket = getCurrentBucket();
        if (currentBucket == null) {
            return new long[0];
        }

        Bucket[] array = buckets.getArray();
        int i = 0;
        long[] values = new long[array.length];
        for (Bucket bucket : array) {
            if (type.isCounter()) {
                values[i++] = bucket.getAdder(type).sum();
            } else {
                values[i++] = bucket.getMaxUpdater(type).max();
            }
        }
        return values;
    }

    public long getRollingMaxValue(RollingNumberEvent type) {
        long[] values = getValues(type);
        if (values.length == 0) {
            return 0;
        } else {
            Arrays.sort(values);
            return values[values.length - 1];
        }
    }

    public Bucket getCurrentBucket() {
        long currentTimeInMills = time.getCurrentTimeInMills();
        Bucket currentBucket = buckets.peekLast();

        if (currentBucket != null && currentTimeInMills < currentBucket.windowStart + this.bucketSizeInMillisecond) {
            return currentBucket;
        }
        // 如果没有找到当前桶，就必须创建一个返回
        if (lock.tryLock()) {
            try {
                if (buckets.peekLast() == null) {
                    Bucket newBucket = new Bucket(currentTimeInMills);
                    buckets.addLast(newBucket);
                    return newBucket;
                } else {
                    // 如果元素数组已经放满了 此时就利用环形数组的特性 交替替换桶，重复利用
                    for (int i = 0; i < numberOfBuckets; i++) {
                        Bucket lastBucket = buckets.peekLast();
                        if (currentTimeInMills < lastBucket.windowStart + this.bucketSizeInMillisecond) {
                            return lastBucket;
                        } else if (currentTimeInMills - (lastBucket.windowStart + this.bucketSizeInMillisecond) > timeInMilliseconds) {
                            // 执行reset重置操作
                            reset();
                            // 再获取一次当前桶
                            return getCurrentBucket();
                        } else {
                            buckets.addLast(new Bucket(lastBucket.windowStart + this.bucketSizeInMillisecond));
                            cumulativeSum.addBucket(lastBucket);
                        }
                    }
                    return buckets.peekLast();
                }
            } finally {
                lock.unlock();
            }
        } else {
            // 如果没有获取到锁 一定是另一个线程创建了桶
            currentBucket = buckets.peekLast();
            if (currentBucket != null) {
                return currentBucket;
            } else {
                try {
                    Thread.sleep(5L);
                } catch (InterruptedException e) {
                }
                return getCurrentBucket();
            }
        }
    }

    public void reset() {
        Bucket bucket = buckets.peekLast();
        if (bucket != null) {
            cumulativeSum.addBucket(bucket);
        }
        buckets.clear();
    }


    public interface Time {
        long getCurrentTimeInMills();
    }

    private static class ActualTime implements Time {
        @Override
        public long getCurrentTimeInMills() {
            return System.currentTimeMillis();
        }
    }

    public static class Bucket {
        // 窗口开始时间
        final long windowStart;

        // 事件集合数组
        final LongAdder[] adderForCounterType;

        // 事件集合数组，累积更新
        final LongMaxUpdater[] updaterForCounterType;

        // RT
        final ConcurrentHashMap<Integer, LongAdder> rtBottle;

        public Bucket(long windowStart) {
            this.windowStart = windowStart;
            adderForCounterType = new LongAdder[RollingNumberEvent.values().length];
            for (RollingNumberEvent event : RollingNumberEvent.values()) {
                if (event.isCounter()) {
                    adderForCounterType[event.ordinal()] = new LongAdder();
                }
            }
            updaterForCounterType = new LongMaxUpdater[RollingNumberEvent.values().length];
            for (RollingNumberEvent event : RollingNumberEvent.values()) {
                if (event.isMaxUpdater()) {
                    updaterForCounterType[event.ordinal()] = new LongMaxUpdater();
                    updaterForCounterType[event.ordinal()].update(0);
                }
            }
            rtBottle = new ConcurrentHashMap<>();
        }

        long get(RollingNumberEvent type) {
            if (type.isCounter()) {
                return adderForCounterType[type.ordinal()].sum();
            }
            if (type.isMaxUpdater()) {
                return updaterForCounterType[type.ordinal()].max();
            }
            throw new IllegalStateException("Unknown type of event: " + type.name());
        }

        LongAdder getAdder(RollingNumberEvent type) {
            if (!type.isCounter()) {
                throw new IllegalStateException("Type is not a Counter: " + type.name());
            }
            return adderForCounterType[type.ordinal()];
        }

        LongMaxUpdater getMaxUpdater(RollingNumberEvent type) {
            if (!type.isMaxUpdater()) {
                throw new IllegalStateException("Type is not a MaxUpdater: " + type.name());
            }
            return updaterForCounterType[type.ordinal()];
        }

        //	返回Map集合rtBottle
        ConcurrentHashMap<Integer, LongAdder> getRTBottle() {
            return rtBottle;
        }

        long getWindowStart() {
            return windowStart;
        }
    }

    public static class CumulativeSum {
        // 事件集合数组
        final LongAdder[] adderForCounterType;

        // 事件集合数组，累积更新
        final LongMaxUpdater[] updaterForCounterType;

        final String uniqueKey;

        final BlockingQueue<Pair<String, Long>> blockingQueue;

        public CumulativeSum(String uniqueKey, BlockingQueue<Pair<String, Long>> blockingQueue) {
            this.uniqueKey = uniqueKey;
            this.blockingQueue = blockingQueue;

            adderForCounterType = new LongAdder[RollingNumberEvent.values().length];
            for (RollingNumberEvent event : RollingNumberEvent.values()) {
                if (event.isCounter()) {
                    adderForCounterType[event.ordinal()] = new LongAdder();
                }
            }
            updaterForCounterType = new LongMaxUpdater[RollingNumberEvent.values().length];
            for (RollingNumberEvent event : RollingNumberEvent.values()) {
                if (event.isMaxUpdater()) {
                    updaterForCounterType[event.ordinal()] = new LongMaxUpdater();
                    updaterForCounterType[event.ordinal()].update(0);
                }
            }
        }

        public void addBucket(Bucket lastBucket) {
            for (RollingNumberEvent type : RollingNumberEvent.values()) {
                if (type.isCounter()) {
                    long sum = lastBucket.getAdder(type).sum();
                    getAdder(type).add(sum);
                    if (sum != 0) {
                        // 数据上报
                        System.out.println("QPS：" + sum);
                        if (blockingQueue != null) {
                            blockingQueue.add(new Pair<String, Long>(type.name(), sum));
                        }
                    }
                }
                if (type.isMaxUpdater()) {
                    //	获取最后一个桶的统计值, 计数到对应的类型匹配的LongMaxUpdater
                    long max = lastBucket.getMaxUpdater(type).max();
                    getMaxUpdater(type).update(max);
                    if (max != 0) {
                        //	每次上报数据的时机
                        if (blockingQueue != null) {
                            blockingQueue.add(new Pair<String, Long>(type.name(), max));
                        }
                    }
                }
            }
            String rtValue = rtBottleToString(lastBucket.getRTBottle());
            if (StringUtils.isNotEmpty(rtValue)) {
                //	每次上报rt数据的时机
            }

        }

        private String rtBottleToString(ConcurrentHashMap<Integer, LongAdder> bottle) {
            if (bottle == null || bottle.size() == 0) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            bottle.forEach((key, value) -> sb.append(key).append(":").append(value.longValue()).append(","));
            if ((",").equals(sb.substring(sb.length() - 1, sb.length()))) {
                sb.delete(sb.length() - 1, sb.length());
            }
            return sb.toString();
        }

        long get(RollingNumberEvent type) {
            if (type.isCounter()) {
                return adderForCounterType[type.ordinal()].sum();
            }
            if (type.isMaxUpdater()) {
                return updaterForCounterType[type.ordinal()].max();
            }
            throw new IllegalStateException("Unknown type of event: " + type.name());
        }

        LongAdder getAdder(RollingNumberEvent type) {
            if (!type.isCounter()) {
                throw new IllegalStateException("Type is not a Counter: " + type.name());
            }
            return adderForCounterType[type.ordinal()];
        }

        LongMaxUpdater getMaxUpdater(RollingNumberEvent type) {
            if (!type.isMaxUpdater()) {
                throw new IllegalStateException("Type is not a MaxUpdater: " + type.name());
            }
            return updaterForCounterType[type.ordinal()];
        }
    }

    public static class BucketCircularArray implements Iterable<Bucket> {

        private final int dataLength;

        private final int numBuckets;
        private final AtomicReference<ListState> state;

        public BucketCircularArray(int size) {
            AtomicReferenceArray<Bucket> _buckets = new AtomicReferenceArray<>(size + 1);
            state = new AtomicReference<>(new ListState(_buckets, 0, 0));
            dataLength = _buckets.length();
            numBuckets = size;
        }

        @Override
        public Iterator<Bucket> iterator() {
            return Collections.unmodifiableCollection(Arrays.asList(getArray())).iterator();
        }

        private class ListState {

            private final AtomicReferenceArray<Bucket> data;

            private final int size;

            private final int head;

            private final int tail;

            private ListState(AtomicReferenceArray<Bucket> data, int head, int tail) {
                this.head = head;
                this.data = data;
                this.tail = tail;
                if (head == 0 && tail == 0) {
                    size = 0;
                } else {
                    this.size = (tail + dataLength - head) % dataLength;
                }
            }

            public Bucket tail() {
                if (size == 0) {
                    return null;
                } else {
                    return data.get(convert(size - 1));
                }
            }

            private Bucket[] getArray() {
                ArrayList<Bucket> arrayList = new ArrayList<>();
                for (int i = 0; i < size; i++) {
                    arrayList.add(data.get(convert(i)));
                }
                return arrayList.toArray(new Bucket[0]);
            }

            private ListState incrementTail() {
                if (size == numBuckets) {
                    return new ListState(data, (head + 1) % dataLength, (tail + 1) % dataLength);
                } else {
                    return new ListState(data, head, (tail + 1) % dataLength);
                }
            }

            public ListState clear() {
                return new ListState(new AtomicReferenceArray<>(dataLength), 0, 0);
            }

            public ListState addBucket(Bucket b) {
                data.set(tail, b);
                return incrementTail();
            }

            public int convert(int index) {
                return (index + head) % dataLength;
            }
        }


        public void clear() {
            while (true) {
                ListState current = state.get();
                ListState newState = current.clear();
                if (state.compareAndSet(current, newState)) {
                    return;
                }
            }
        }

        public void addLast(Bucket o) {
            ListState currentState = state.get();
            ListState newState = currentState.addBucket(o);
            if (state.compareAndSet(currentState, newState)) {
                return;
            } else {
                return;
            }
        }

        public Bucket getLast() {
            return peekLast();
        }

        public int size() {
            return state.get().size;
        }

        public Bucket peekLast() {
            return state.get().tail();
        }

        public Bucket[] getArray() {
            return state.get().getArray();
        }
    }
}
