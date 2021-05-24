package com.github.t1.graphql.federation.quarkus.extension.deployment;

import io.quarkus.builder.item.SimpleBuildItem;

import java.util.ArrayList;
import java.util.List;

public final class EntitiesBuildItem extends SimpleBuildItem {
    private final List<String> entities = new ArrayList<>();

    public void add(String name) { entities.add(name); }

    public List<String> getEntities() { return entities; }
}
