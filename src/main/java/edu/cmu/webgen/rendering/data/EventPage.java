package edu.cmu.webgen.rendering.data;

import org.eclipse.jdt.annotation.NonNull;

import java.util.List;

public final class EventPage extends EntryPage {
    private final @NonNull String startDate;
    private final @NonNull String endDate;

    public EventPage(SiteData siteData, String pageTitle, List<SiteLink> breadcrumbs, @NonNull String startDate,
                     @NonNull String endDate, List<SiteLink> topics, List<ContentFragment> content) {
        super(siteData, pageTitle, breadcrumbs, topics, content);
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public String getTemplate() {
        return "event.html";
    }


    public @NonNull String getStartDate() {
        return this.startDate;
    }

    public @NonNull String getEndDate() {
        return this.endDate;
    }


}
