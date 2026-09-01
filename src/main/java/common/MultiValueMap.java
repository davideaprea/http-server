package common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MultiValueMap<K, V> extends HashMap<K, List<V>> {
    public MultiValueMap<K, V> add(K key, V value) {
        computeIfAbsent(key, k -> new ArrayList<>()).add(value);

        return this;
    }
}
