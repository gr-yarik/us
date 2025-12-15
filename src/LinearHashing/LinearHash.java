package LinearHashing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import UnsortedFile.StorableRecord;

public class LinearHash<T extends StorableRecord> {

    private static final int M = 2;
    private static final double SPLIT_THRESHOLD = 0.8;
    private static final double MERGE_THRESHOLD = 0.1;

    private BucketHeap<T> bucketHeap;
    private Function<T, Integer> keyExtractor;

    private int level;
    private int splitPointer;
    private int totalOverflowBlocks;
    private int totalPrimaryBuckets;

    public LinearHash(String mainBucketsPath, String overflowBlocksPath, int mainBucketsBlockSize,
            int overflowBlockSize, Class<T> recordClass, Function<T, Integer> keyExtractor) throws IOException {
        this.keyExtractor = keyExtractor;
        this.bucketHeap = new BucketHeap<>(mainBucketsPath, overflowBlocksPath, mainBucketsBlockSize, overflowBlockSize,
                recordClass);

        this.level = 0;
        this.splitPointer = 0;
        this.totalOverflowBlocks = 0;
        this.totalPrimaryBuckets = M;

        bucketHeap.getMainBucketsHeap().extendToBlockCount(M);
        updateMetadata();
    }

    private int hashU(int key) {
        int divisor = M * (1 << level);
        return key % divisor;
    }

    private int hashNext(int key) {
        int divisor = M * (1 << (level + 1));
        return key % divisor;
    }

    private int calculateBucketAddress(int key) {
        // Urči skupinu i, Ak i < S tak i := Hu+1(K)
        int address = hashU(key);
        if (address < splitPointer) {
            address = hashNext(key);
        }
        return address;
    }

    public boolean insert(T record) throws IOException {
        int key = keyExtractor.apply(record);
        int bucketAddress = calculateBucketAddress(key);

        boolean inserted = bucketHeap.insertIntoBucket(bucketAddress, record);
        if (inserted) {
            updateMetadata();
            checkAndSplit();
        }
        return inserted;
    }

    public T get(T partialRecord) throws IOException {
        int key = keyExtractor.apply(partialRecord);
        int bucketAddress = calculateBucketAddress(key);

        return bucketHeap.get(bucketAddress, partialRecord);
    }

    public boolean delete(T partialRecord) throws IOException {
        int key = keyExtractor.apply(partialRecord);
        int bucketAddress = calculateBucketAddress(key);

        boolean deleted = bucketHeap.delete(bucketAddress, partialRecord);
        if (deleted) {
            updateMetadata();

            // Ak hustota d klesla pod hranicu
            if (willShakingFreeOverflowBlocks(bucketAddress)) {
                performShaking(bucketAddress);
            }
            checkAndMerge();
        }
        return deleted;
    }

    private void checkAndSplit() throws IOException {
        double overflowRatio = calculateOverflowRatio();
        if (overflowRatio > SPLIT_THRESHOLD) {
            performSplit();
        }
    }

    private void checkAndMerge() throws IOException {
        double overflowRatio = calculateOverflowRatio();
        if (overflowRatio < MERGE_THRESHOLD && totalPrimaryBuckets > M) {
            performMerge();
        }
    }

    private double calculateOverflowRatio() {
        if (totalPrimaryBuckets == 0) {
            return 0.0;
        }
        return (double) totalOverflowBlocks / totalPrimaryBuckets;
    }

    private void performSplit() throws IOException {
        Bucket<T> bucketToSplit = (Bucket<T>) bucketHeap.getMainBucketsHeap().readBlock(splitPointer);
        if (bucketToSplit == null) {
            return;
        }

        List<T> allRecords = new ArrayList<>();
        bucketHeap.collectAllRecords(splitPointer, allRecords);

        Class<T> recordClass = (Class<T>) allRecords.get(0).getClass();
        bucketToSplit = new Bucket<>(
                bucketHeap.getMainBucketsHeap().getBlockingFactor(),
                bucketHeap.getBlockSize(),
                recordClass);

        int newBucketAddress = splitPointer + (M * (1 << level));
        // bucketHeap.ensureBucketExists(newBucketAddress);

        bucketHeap.clearOverflowChain(splitPointer);

        for (T record : allRecords) {
            int recordKey = keyExtractor.apply(record);
            int newAddress = hashNext(recordKey);

            if (newAddress == splitPointer) {
                bucketHeap.insertIntoBucket(splitPointer, record);
            } else {
                bucketHeap.insertIntoBucket(newBucketAddress, record);
            }
        }

        splitPointer++;
        if (splitPointer >= M * (1 << level)) {
            splitPointer = 0;
            level++;
        }

        totalPrimaryBuckets++;
        updateMetadata();
    }

    private void performMerge() throws IOException {
        int a; // last group address
        int b; // target group address

        // 4a. Ak S > 0, tak záznamy uložené v poslednej skupine a
        // (a = S + M*2^u - 1) presuň do skupiny na pozícii b = S-1
        // a poslednú skupinu zruš. Nastav S := b;
        if (splitPointer > 0) {
            a = splitPointer + M * (1 << level) - 1;
            b = splitPointer - 1;
        }
        // 4b. Ak S = 0 a u > 0, tak záznamy uložené v poslednej skupine
        // (a = M*2^u - 1) presuň do skupiny na pozícii
        // b = M*2^(u-1) - 1 a poslednú skupinu zruš.
        // Nastav S := b; Nastav u := u – 1;
        else if (splitPointer == 0 && level > 0) {
            a = M * (1 << level) - 1;
            b = M * (1 << (level - 1)) - 1;
        } else {
            return;
        }

        Bucket<T> lastBucket = (Bucket<T>) bucketHeap.getMainBucketsHeap().readBlock(a);
        if (lastBucket == null) {
            return;
        }

        List<T> recordsToMerge = new ArrayList<>();
        bucketHeap.collectAllRecords(a, recordsToMerge);

        bucketHeap.clearOverflowChain(a);

        for (T record : recordsToMerge) {
            bucketHeap.insertIntoBucket(b, record);
        }

        @SuppressWarnings("unchecked")
        Class<T> recordClass = (Class<T>) recordsToMerge.get(0).getClass();
        Bucket<T> emptyBucket = new Bucket<>(
                bucketHeap.getMainBucketsHeap().getBlockingFactor(),
                bucketHeap.getBlockSize(),
                recordClass);
        bucketHeap.getMainBucketsHeap().writeBlock(a, emptyBucket);

        if (splitPointer > 0) {
            splitPointer = b; // S := b
        } else if (splitPointer == 0 && level > 0) {
            splitPointer = b; // S := b
            level--; // u := u - 1
        }

        totalPrimaryBuckets--;
        updateMetadata();
    }

    private boolean willShakingFreeOverflowBlocks(int bucketAddress) throws IOException {
        Bucket<T> bucket = (Bucket<T>) bucketHeap.getMainBucketsHeap().readBlock(bucketAddress);
        if (bucket == null || bucket.getOverflowBucketCount() == 0) {
            return false;
        }

        int currentOverflowBlocks = bucket.getOverflowBucketCount();
        int totalElements = bucket.getTotalElementCount();
        int blockingFactor = bucketHeap.getMainBucketsHeap().getBlockingFactor();

        int recordsNeedingOverflow = Math.max(0, totalElements - blockingFactor);

        int minOverflowBlocksNeeded;
        if (recordsNeedingOverflow == 0) {
            minOverflowBlocksNeeded = 0;
        } else {
            minOverflowBlocksNeeded = (recordsNeedingOverflow + blockingFactor - 1) / blockingFactor;
        }

        return minOverflowBlocksNeeded < currentOverflowBlocks;
    }

    private void performShaking(int bucketAddress) throws IOException {
        Bucket<T> bucket = (Bucket<T>) bucketHeap.getMainBucketsHeap().readBlock(bucketAddress);
        if (bucket == null || bucket.getOverflowBucketCount() == 0) {
            return;
        }

        List<T> allRecords = new ArrayList<>();
        bucketHeap.collectAllRecords(bucketAddress, allRecords);

        if (allRecords.isEmpty()) {
            return;
        }

        @SuppressWarnings("unchecked")
        Class<T> recordClass = (Class<T>) allRecords.get(0).getClass();
        bucket = new Bucket<>(
                bucketHeap.getMainBucketsHeap().getBlockingFactor(),
                bucketHeap.getBlockSize(),
                recordClass);
        bucketHeap.clearOverflowChain(bucketAddress);

        for (T record : allRecords) {
            bucketHeap.insertIntoBucket(bucketAddress, record);
        }

        updateMetadata();
    }

    private void updateMetadata() {
        try {
            int calculatedOverflowBlocks = 0;
            for (int i = 0; i < totalPrimaryBuckets; i++) {
                Bucket<T> bucket = (Bucket<T>) bucketHeap.getMainBucketsHeap().readBlock(i);
                if (bucket != null) {
                    calculatedOverflowBlocks += bucket.getOverflowBucketCount();
                }
            }
            totalOverflowBlocks = calculatedOverflowBlocks;
        } catch (IOException e) {
        }
    }

    public void close() throws IOException {
        if (bucketHeap != null) {
            bucketHeap.close();
        }
    }

    public int getLevel() {
        return level;
    }

    public int getSplitPointer() {
        return splitPointer;
    }

    public int getTotalOverflowBlocks() {
        return totalOverflowBlocks;
    }

    public int getTotalPrimaryBuckets() {
        return totalPrimaryBuckets;
    }

    public double getOverflowRatio() {
        return calculateOverflowRatio();
    }
}
