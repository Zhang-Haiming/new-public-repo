package edu.cmu.webgen.project;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

public class ProjectTest {
    private Project project;
    private Article a;
    private SubArticle sa;
    private SubSubArticle ssa;
    private HashMap<Object, Set<Topic>> topics;
    private Set<Topic> aTopics = Collections.singleton(mock(Topic.class));
    private Set<Topic> saTopics = Collections.singleton(mock(Topic.class));
    private Set<Topic> ssaTopics = Collections.singleton(mock(Topic.class));
    

    @Before
    public void setUp() {
        topics = new HashMap<>();
        ssa = new SubSubArticle(Collections.emptyList(), "", LocalDateTime.now(), LocalDateTime.now());
        sa = new SubArticle(Collections.emptyList(), Collections.singletonList(ssa), null, LocalDateTime.now(), LocalDateTime.now());
        a = new Article(Collections.emptyList(), Collections.singletonList(sa), "", LocalDateTime.now(), LocalDateTime.now());
        topics.put(a, aTopics);
        topics.put(sa, saTopics);
        topics.put(ssa, ssaTopics);
        project = new Project("project", "test", Collections.emptyList(), Collections.emptyList(), topics);
    }

    @Test
    public void testEmptyTopics() {
        assertEquals(Collections.emptySet(), project.getTopics(new Object()));
    }

    @Test
    public void testArticleGetTopics() {
        Set<Topic> aPlusSaPlusSsaTopics = Stream
            .of(aTopics, saTopics, ssaTopics)
            .flatMap(Set::stream)
            .collect(Collectors.toSet());
        assertEquals(aPlusSaPlusSsaTopics, project.getTopics(a));
    }

    @Test
    public void testSubArticleGetTopics() {
        Set<Topic> saPlusSsaTopics = Stream
            .of(saTopics, ssaTopics)
            .flatMap(Set::stream)
            .collect(Collectors.toSet());
        assertEquals(saPlusSsaTopics, project.getTopics(sa));
    }

    @Test
    public void testSubSubArticleGetTopics() {
        assertEquals(ssaTopics, project.getTopics(ssa));
    }

}
