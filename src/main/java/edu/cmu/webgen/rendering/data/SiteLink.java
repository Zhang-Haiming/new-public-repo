package edu.cmu.webgen.rendering.data;

public class SiteLink {
    private final SiteURL address;
    private final String title;
    private final boolean isCurrent;

    public SiteLink(SiteURL address, String title, boolean isCurrent) {
        this.address = address;
        this.title = title;
        this.isCurrent = isCurrent;
    }

    public SiteLink(SiteURL address, String title) {
        this.address = address;
        this.title = title;
        this.isCurrent = false;
    }

    public SiteURL getAddress() {
        return this.address;
    }

    public String getTitle() {
        return this.title;
    }

    public boolean getIsCurrent() {
        return this.isCurrent;
    }
}
