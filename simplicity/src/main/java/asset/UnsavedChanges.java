package asset;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class UnsavedChanges {

    private final Set<Savable> dirty = new LinkedHashSet<>();

    public void markDirty(Savable s) {
        dirty.add(s);
    }

    /** Back to unchanged without saving, e.g. when undo returns to the saved state. */
    public void markClean(Savable s) {
        dirty.remove(s);
    }

    public boolean isDirty(Savable s) {
        return dirty.contains(s);
    }

    public boolean any() {
        return !dirty.isEmpty();
    }

    /** The names of everything unsaved, in the order it was changed (for the "unsaved changes" popup). */
    public List<String> displayNames() {
        List<String> names = new ArrayList<>();
        for (Savable savable : dirty) names.add(savable.displayName());
        return names;
    }

    public void save(Savable s) throws IOException {
        s.save(); dirty.remove(s);
    }

    public List<String> saveAll() {
        List<String> failed = new ArrayList<>();
        Iterator<Savable> it = dirty.iterator();
        while (it.hasNext()) {
            Savable savable = it.next();
            try {
                savable.save();
                it.remove();                                   
            } catch (Exception e) {                            
                failed.add(savable.displayName() + ": " + e.getMessage());
            }
        }
        return failed;
    }
}