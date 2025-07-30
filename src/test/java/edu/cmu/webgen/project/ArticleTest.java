package edu.cmu.webgen.project;

import org.junit.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ArticleTest {
    private static final LocalDateTime JAN_FIRST = LocalDateTime.of(2024, 1, 1, 0, 0);
    private static final LocalDateTime JAN_SECOND = LocalDateTime.of(2024, 1, 2, 0, 0);
    private static final LocalDateTime FEB_SECOND = LocalDateTime.of(2024, 2, 2, 0, 0);

    @Test
    public void testArticle() {
        List<AbstractContent> content = new ArrayList<>();
        List<SubArticle> subArticles = new ArrayList<>();
        Article article = new Article(content, subArticles, "dir", FEB_SECOND, JAN_FIRST);
        System.out.println(article.getMetadata().toString());
        article.addMetadata(new Metadata(Collections.singletonMap("title", "Sample Title")));
        // compareTo
        assertEquals(0, article.compareTo(article));

        // getID
        assertEquals("sample_tiitle", article.getId());

        // getLastUpdate
        Optional<LocalDateTime> innerLastUpdateArticle = article.getInnerArticles()
                .stream().map(SubArticle::getLastUpdate).max(LocalDateTime::compareTo);
        LocalDateTime last = article.getLastUpdate();
        if (innerLastUpdateArticle.isPresent() && innerLastUpdateArticle.get().compareTo(last) > 0)
            last = innerLastUpdateArticle.get();
        assertEquals(last, article.getLastUpdate());

        // getCreated
        Optional<LocalDateTime> innerCreatedArticle = article.getInnerArticles()
                .stream().map(SubArticle::getCreated).max(LocalDateTime::compareTo);
        last = article.getCreated();
        if (innerCreatedArticle.isPresent() && innerCreatedArticle.get().compareTo(last) > 0)
            last = innerCreatedArticle.get();
        assertEquals(last, article.getCreated());

        // getInnerArticles
        assertEquals(0, article.getInnerArticles().size());

        // getTitle
        assertEquals("Sample Title", article.getTitle());

        // addMetadata and getMetadata
        article.addMetadata(new Metadata(Collections.singletonMap("key1", "value1")));
        Set<String> keys = new HashSet<>();
        keys.addAll(List.of("key", "title"));
        assertEquals(keys, article.getMetadata().getKeys());

        // addContent and getContent
        AbstractContent content1 = new Image(null, null, null, 0);
        AbstractContent content2 = new Video(null, null, null, 0);
        article.addContent(content1);
        article.addContent(content2);
        assertEquals(2, article.getContent().size());
        assertTrue(article.getContent().contains(content1));
        assertTrue(article.getContent().contains(content2));

        // getPublishedDate
        article.addMetadata(new Metadata(Collections.singletonMap("date", "January 2nd 2024 at midnight")));
        assertEquals(JAN_SECOND, article.getPublishedDate());
    }
}