package com.christocentricus.easytracker.model;

public class Category {
    public long id;
    public String name;

    public Category(long id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
