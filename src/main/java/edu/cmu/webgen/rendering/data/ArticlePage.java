package edu.cmu.webgen.rendering.data;

import org.eclipse.jdt.annotation.NonNull;

import java.util.List;

public class ArticlePage extends EntryPage {
    private final String publishedDate;

    public ArticlePage(SiteData siteData, String pageTitle, List<SiteLink> breadcrumbs, @NonNull String publishedDate,
                       List<SiteLink> topics, List<ContentFragment> content) {
        super(siteData, pageTitle, breadcrumbs, topics, content);
        this.publishedDate = publishedDate;
    }

    @Override
    public String getTemplate() {
        return "article.html";
    }

    public String getPublishedDate() {
        return this.publishedDate;
    }

}
