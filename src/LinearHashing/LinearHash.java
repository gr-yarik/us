package LinearHashing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import LinearHashing.BucketHeap.BlockAndNumber;
import UnsortedFile.StorableRecord;

public class LinearHash<T extends StorableRecord> {

    private static final int M = 2;
    private static final double SPLIT_THRESHOLD = 0.9;
    private static final double MERGE_THRESHOLD = 0.15;

    private BucketHeap<T> bucketHeap;
    private Function<T, Integer> keyExtractor;

    private int level;
    private int splitPointer;
    private int totalPrimaryBuckets;

    private int debugInfoTotalOverflowBlocks;

    public LinearHash(String mainBucketsPath, String overflowBlocksPath, int mainBucketsBlockSize,
            int overflowBlockSize, Class<T> recordClass, Function<T, Integer> keyExtractor) {
        this.keyExtractor = keyExtractor;
        this.bucketHeap = new BucketHeap<>(mainBucketsPath, overflowBlocksPath, mainBucketsBlockSize, overflowBlockSize,
                recordClass);

        this.level = 0;
        this.splitPointer = 0;
        this.debugInfoTotalOverflowBlocks = 0;
        this.totalPrimaryBuckets = M;

        bucketHeap.getMainBucketsHeap().extendToBlockCount(M);
        updateDebugInfo();
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

    public boolean insert(T record) {
        int key = keyExtractor.apply(record);
        int bucketAddress = calculateBucketAddress(key);

        boolean inserted = bucketHeap.insertIntoBucket(bucketAddress, record);
        if (inserted) {
            updateDebugInfo();

            double overflowRatio = calculateOverflowRatio();
            if (overflowRatio > SPLIT_THRESHOLD) {
                performSplit();
            }
        }
        return inserted;
    }

    public T get(T partialRecord) {
        int key = keyExtractor.apply(partialRecord);
        int bucketAddress = calculateBucketAddress(key);

        return bucketHeap.get(bucketAddress, partialRecord);
    }

    public boolean delete(T partialRecord) {
        int key = keyExtractor.apply(partialRecord);
        int bucketAddress = calculateBucketAddress(key);

        boolean deleted = bucketHeap.delete(bucketAddress, partialRecord);
        if (deleted) {
            updateDebugInfo();
            mergeIfNeeded();
        }
        return deleted;
    }

    private void mergeIfNeeded() {
        double overflowRatio = calculateOverflowRatio();
        // Ak hustota d klesla pod hranicu
        if (overflowRatio < MERGE_THRESHOLD && totalPrimaryBuckets > M) {
            performMerge();
        }
    }

    private double calculateOverflowRatio() {
        int mainBlocks = bucketHeap.getMainBucketsHeap().getTotalBlockCount();
        int overflowBlocks = bucketHeap.getOverflowHeap().getTotalBlockCount();
        return (double) overflowBlocks / mainBlocks;
    }

    private void performSplit() {
        Bucket<T> bucketToSplit = (Bucket<T>) bucketHeap.getMainBucketsHeap().readBlock(splitPointer);
        List<T> allRecords = new ArrayList<>();
        List<BlockAndNumber> overflowBlocks= new ArrayList<>();
        bucketHeap.collectAllRecords(bucketToSplit, allRecords, overflowBlocks);
        bucketHeap.clearOverflowChain(overflowBlocks);
        bucketHeap.getMainBucketsHeap().writeBlock(splitPointer, bucketToSplit);

        int newBucketAddress = splitPointer + (M * (1 << level));
    
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
        updateDebugInfo();
    }

    private void performMerge() {
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
        List<T> recordsToMerge = new ArrayList<>();
        List<BlockAndNumber> overflowBlocks = new ArrayList<>();
        bucketHeap.collectAllRecords(lastBucket, recordsToMerge, overflowBlocks);
        bucketHeap.clearOverflowChain(overflowBlocks);

        for (T record : recordsToMerge) {
            bucketHeap.insertIntoBucket(b, record);
        }

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
        updateDebugInfo();
    }

    private void updateDebugInfo() {
        int calculatedOverflowBlocks = 0;
        for (int i = 0; i < totalPrimaryBuckets; i++) {
            Bucket<T> bucket = (Bucket<T>) bucketHeap.getMainBucketsHeap().readBlock(i);
            if (bucket != null) {
                calculatedOverflowBlocks += bucket.getTotalOverflowBlockCount();
            }
        }
        debugInfoTotalOverflowBlocks = calculatedOverflowBlocks;
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

    public int getDebugInfoTotalOverflowBlocks() {
        return debugInfoTotalOverflowBlocks;
    }

    public int getTotalPrimaryBuckets() {
        return totalPrimaryBuckets;
    }

    public double getOverflowRatio() {
        return calculateOverflowRatio();
    }

    public BucketHeap<T> getBucketHeap() {
        return bucketHeap;
    }
}
