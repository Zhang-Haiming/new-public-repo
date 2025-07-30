package edu.cmu.webgen.rendering.data;

import org.eclipse.jdt.annotation.Nullable;

public final class ContentFragment {
    private final @Nullable String title;
    private final String fragmentContent;

    public ContentFragment(@Nullable String title, String fragmentContent) {
        this.title = title;
        this.fragmentContent = fragmentContent;
    }

    public @Nullable String getTitle() {
        return this.title;
    }

    public String getFragmentContent() {
        return this.fragmentContent;
    }
}
