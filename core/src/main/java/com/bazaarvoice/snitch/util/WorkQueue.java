/*
 * Copyright 2011 Bazaarvoice
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bazaarvoice.snitch.util;

import com.google.common.collect.MapMaker;
import com.google.common.collect.Sets;

import java.lang.ref.WeakReference;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * An implementation of a work queue that atomically allows for removing all entries of the queue in batch.
 * <p/>
 * This implementation holds weak references to the work entries.
 */
public class WorkQueue<T> {
    private final Set<T> _workSet = Sets.newSetFromMap(new MapMaker().weakKeys().<T, Boolean>makeMap());
    private final LinkedBlockingQueue<WeakReference<T>> _workQueue = new LinkedBlockingQueue<WeakReference<T>>();

    public void add(T item) {
        if (_workSet.add(item)) {
            _workQueue.offer(new WeakReference<T>(item));
        }
    }

    public Set<T> removeAll() {
        Set<T> items = Sets.newHashSet();

        WeakReference<T> ref;
        while ((ref = _workQueue.poll()) != null) {
            T item = ref.get();
            if (item != null) {
                _workSet.remove(item);
                items.add(item);
            }
        }

        return items;
    }
}
