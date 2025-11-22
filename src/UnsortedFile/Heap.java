package UnsortedFile;

import java.util.ArrayList;
import java.util.List;

public class Heap<T extends StorableRecord> {
    
    private List<T> records = new ArrayList<>();
    
    public int insert(T instance) {
        records.add(instance);
        int index = records.size() - 1;
        return index;
    }

    public T get(int index, T partialRecord) {
        if (index < 0 || index >= records.size()) {
            return null;
        }

        T element = null;

        for (T t : records) {
            if (partialRecord.equals(t)) {
                element = t;
                break;
            }
        }

        return element;
    }
}
