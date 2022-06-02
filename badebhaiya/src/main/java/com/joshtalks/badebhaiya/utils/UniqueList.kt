package com.joshtalks.badebhaiya.utils

import java.util.function.Predicate

/**
 * UniqueList
 *
 * An implementation of [List] that is random accessible
 * and contains unique elements.
 *
 * @param E The type of elements stored in this list
 * @param T The type of the identifying property of [E]
 */
class UniqueList<E : UniqueList.Unique<T>, T> : ArrayList<E>() {

    /**
     * Ensures the uniqueness of this list's elements
     */
    private val set = hashSetOf<T>()

    override fun add(element: E): Boolean {
        if (!set.contains(element.id)) {
            set.add(element.id)
            return super.add(element)
        }
        return false
    }

    override fun add(index: Int, element: E) {
        if (!set.contains(element.id)) {
            set.add(element.id)
            super.add(index, element)
        }
    }

    override fun addAll(elements: Collection<E>): Boolean {
        val filtered = filterElements(elements)
        trackElements(filtered)
        return super.addAll(filtered)
    }

    override fun addAll(index: Int, elements: Collection<E>): Boolean {
        val filtered = filterElements(elements)
        trackElements(filtered)
        return super.addAll(index, filtered)
    }

    override fun clear() {
        set.clear()
        super.clear()
    }

    override fun contains(element: E): Boolean {
        return set.contains(element.id)
    }

    override fun containsAll(elements: Collection<E>): Boolean {
        for (element in elements)
            if (!set.contains(element.id))
                return false
        return true
    }

    override fun remove(element: E): Boolean {
        if (!set.contains(element.id)) return false
        set.remove(element.id)
        return super.remove(element)
    }

    override fun removeAll(elements: Collection<E>): Boolean {
        set.removeAll(elements.map { it.id })
        return super.removeAll(elements)
    }

    override fun removeAt(index: Int): E {
        val element = super.removeAt(index)
        set.remove(element.id)
        return element
    }

    override fun removeIf(filter: Predicate<in E>): Boolean {
        set.removeAll(this.filter(filter::test).map { it.id })
        return super.removeIf(filter)
    }

    override fun removeRange(fromIndex: Int, toIndex: Int) {
        set.removeAll(this.subList(fromIndex, toIndex).map { it.id })
        super.removeRange(fromIndex, toIndex)
    }

    override fun retainAll(elements: Collection<E>): Boolean {
        set.retainAll(elements.map { it.id })
        return super.retainAll(elements)
    }

    /**
     * Track elements about to be added.
     *
     * This is called before adding the elements to this instance.
     *
     * @param filtered [List] of elements to track as unique
     * @throws [IllegalArgumentException] if the given list is not of unique elements
     */
    private fun trackElements(filtered: List<E>) {
        val filteredSet = filtered.map { it.id }.toSet()

        require(filteredSet.size == filtered.size) {
            "Elements must be unique according to their id"
        }

        set.addAll(filteredSet)
    }

    /**
     * Filter the elements
     *
     * @param elements [Collection] of elements to filter
     * @return [List] of elements not contained in this instance's bucket.
     */
    private fun filterElements(elements: Collection<E>): List<E> {
        return elements.distinctBy { it.id }.filterNot { set.contains(it.id) }
    }

    /**
     * Interface marking a unique element to be used
     * in [UniqueList]
     *
     * NB: [T] should produce unique hash code
     */
    interface Unique<T> {

        /**
         * Uniqueness identifier property.
         */
        val id: T

    }

}