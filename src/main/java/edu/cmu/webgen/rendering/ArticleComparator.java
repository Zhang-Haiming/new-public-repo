package edu.cmu.webgen.rendering;

import edu.cmu.webgen.WebGenArgs;
import edu.cmu.webgen.project.Article;
import edu.cmu.webgen.project.Project;

import java.util.Comparator;

/**
 * Comparator for sorting articles based on different criteria.
 * Encapsulates all article sorting logic in one place.
 */
public class ArticleComparator implements Comparator<Article> {
    
    private final WebGenArgs.ArticleSorting sorting;
    private final Project project;
    
    public ArticleComparator(WebGenArgs.ArticleSorting sorting, Project project) {
        this.sorting = sorting;
        this.project = project;
    }
    
    @Override
    public int compare(Article o1, Article o2) {
        // Handle pinned articles first if PINNED sorting
        if (sorting == WebGenArgs.ArticleSorting.PINNED) {
            boolean o1Pinned = project.isArticlePinned(o1);
            boolean o2Pinned = project.isArticlePinned(o2);
            if (o1Pinned && !o2Pinned) return -1;
            if (!o1Pinned && o2Pinned) return 1;
        }
        
        // Apply date-based sorting
        if (sorting == WebGenArgs.ArticleSorting.PUBLISHED_FIRST) {
            if (!o1.getPublishedDate().equals(o2.getPublishedDate())) {
                return -o1.getPublishedDate().compareTo(o2.getPublishedDate());
            }
        }
        
        if (sorting == WebGenArgs.ArticleSorting.PUBLISHED_LAST) {
            if (!o1.getPublishedDate().equals(o2.getPublishedDate())) {
                return o1.getPublishedDate().compareTo(o2.getPublishedDate());
            }
        }
        
        if (sorting == WebGenArgs.ArticleSorting.EDITED) {
            if (!o1.getLastUpdate().equals(o2.getLastUpdate())) {
                return o1.getLastUpdate().compareTo(o2.getLastUpdate());
            }
        }
        
        // Default: sort by title
        return o1.getTitle().compareTo(o2.getTitle());
    }
}
