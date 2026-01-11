package LinearHashing;

import UnsortedFile.StorableRecord;

public interface HashableStorableRecord extends StorableRecord {

    public int hashableIdentifier();
    
}