/*
 * AdjacencyGraph, authored by Advanced-Rocketry and used under MIT License (as in hbm-space)
 *
 * MIT License
 *
 * Copyright (c) 2017
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.ntmspace.atmosphere;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/** hbm-space's {@code com.hbm.util.AdjacencyGraph} (from Advanced Rocketry): the node graph of a sealed room. */
public class AdjacencyGraph<T> {

    private final HashMap<T, HashSet<T>> adjacencyMatrix = new HashMap<>();

    /** Adds a node to the graph with its adjacent nodes (which must already be in it). */
    public void add(T object, HashSet<T> adjNodes) {
        if (!contains(object)) {
            adjacencyMatrix.put(object, adjNodes);
            for (T adjacent : adjNodes) adjacencyMatrix.get(adjacent).add(object);
        }
    }

    public boolean contains(T node) {
        return adjacencyMatrix.get(node) != null;
    }

    public boolean isNeighbor(T a, T b) {
        HashSet<T> set = adjacencyMatrix.get(a);
        return set != null && set.contains(b);
    }

    public final Set<T> getKeys() {
        return adjacencyMatrix.keySet();
    }

    /** Every node connected to {@code node}, itself included. */
    public HashSet<T> getAllNodesConnectedToNode(T node) {
        HashSet<T> removableNodes = new HashSet<>();
        Deque<T> stack = new ArrayDeque<>();
        stack.push(node);
        removableNodes.add(node);
        while (!stack.isEmpty()) {
            T stackElement = stack.pop();
            for (T nextElement : adjacencyMatrix.get(stackElement)) {
                if (!removableNodes.contains(nextElement)) {
                    stack.push(nextElement);
                    removableNodes.add(nextElement);
                }
            }
        }
        return removableNodes;
    }

    /** Whether a path from {@code from} to {@code to} exists. */
    public boolean doesPathExist(T from, T to) {
        HashSet<T> visited = new HashSet<>();
        Deque<T> stack = new ArrayDeque<>();
        stack.push(from);
        while (!stack.isEmpty()) {
            T stackElement = stack.pop();
            visited.add(stackElement);
            for (T nextElement : adjacencyMatrix.get(stackElement)) {
                if (to.equals(nextElement)) return true;
                if (!visited.contains(nextElement)) stack.push(nextElement);
            }
        }
        return false;
    }

    /** Removes all nodes connected to {@code node}, returning them. */
    public Collection<T> removeAllNodesConnectedTo(T node) {
        HashSet<T> removable = getAllNodesConnectedToNode(node);
        for (T each : removable) adjacencyMatrix.remove(each);
        return removable;
    }

    /** Removes a single node from the graph. */
    public void remove(T node) {
        HashSet<T> set = adjacencyMatrix.get(node);
        if (set != null) {
            for (T adjacent : set) adjacencyMatrix.get(adjacent).remove(node);
        }
        adjacencyMatrix.remove(node);
    }

    public void clear() {
        adjacencyMatrix.clear();
    }

    public int size() {
        return adjacencyMatrix.size();
    }
}
