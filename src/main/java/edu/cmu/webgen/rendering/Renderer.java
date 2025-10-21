//CHECKSTYLE:OFF
package edu.cmu.webgen.rendering;

import edu.cmu.webgen.WebGen;
import edu.cmu.webgen.WebGenArgs;
import edu.cmu.webgen.project.*;
import edu.cmu.webgen.rendering.data.*;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This class uses the internal data structures and creates pages to be rendered.
 * <p>
 * This class performs all the decision on what pages to create, what is
 * contained on them, and how they link to each other.
 * <p>
 * It creates objects from the `Website` class and passes them to the template engine
 * for the actual rendering.
 */
public class Renderer {
    public static final String EVENTS_ADDRESS = "/events/";
    public static final String TOPICS_ADDRESS = "/topics/";
    public static final String ARTICLES_ADDRESS = "/articles/";
    public static final String ENTRY_ADDRESS = "/p/";
    public static final String HOME_ADDRESS = "/";
    private final SiteLink HOME_LINK = new SiteLink(createURL(HOME_ADDRESS), "Home");
    private final SiteLink ARTICLES_LINK = new SiteLink(createURL(ARTICLES_ADDRESS), "Articles");
    private final SiteLink EVENTS_LINK = new SiteLink(createURL(EVENTS_ADDRESS), "Events");
    private final SiteLink TOPICS_LINK = new SiteLink(createURL(TOPICS_ADDRESS), "Topics");

    public final File targetDirectory;
    public final TemplateEngine templateEngine;
    public final String siteGenerationTime;
    public final WebGenArgs.ArticleSorting sorting;
    public List<SiteLink> headers = null;

    public Renderer(File targetDirectory, WebGenArgs.ArticleSorting sorting, TemplateEngine templateEngine) {
        this.targetDirectory = targetDirectory;
        this.templateEngine = templateEngine;
        this.sorting = sorting;
        this.siteGenerationTime = WebGen.readableFormat(LocalDateTime.now());
    }

    /**
     * create a Pagination object with links to the different pages of a result
     *
     * @param selectedPageIdx index of the selected page
     * @param pageCount       number of pages
     * @param genLink         function to generate the link to a specific page number
     * @return returns a Pagination object that can be rendered
     */
    private static Pagination createPagination(int selectedPageIdx, int pageCount, Function<Integer, SiteURL> genLink) {
        if (pageCount == 1)
            return new Pagination(Collections.emptyList());

        assert selectedPageIdx < pageCount;
        assert pageCount > 0;
        List<List<SiteLink>> links = new ArrayList<>();
        int segmentStart = 0;
        int segmentEnd = pageCount - 1;
        if (pageCount > 10) {
            if (selectedPageIdx < 5) segmentEnd = 8;
            else if (selectedPageIdx > pageCount - 6) {
                segmentStart = pageCount - 9;
            } else {
                segmentStart = selectedPageIdx - 3;
                segmentEnd = selectedPageIdx + 3;
            }
        }
        if (segmentStart != 0) {
            links.add(List.of(new SiteLink(genLink.apply(0), "1", false)));
        }
        List<SiteLink> segmentLinks = new ArrayList<>(segmentEnd - segmentStart);
        for (int idx = segmentStart; idx <= segmentEnd; idx++) {
            segmentLinks.add(new SiteLink(genLink.apply(idx), "" + (idx + 1), selectedPageIdx == idx));
        }
        links.add(segmentLinks);
        if (segmentEnd != pageCount - 1) {
            links.add(List.of(new SiteLink(genLink.apply(pageCount - 1), "" + pageCount,
                    selectedPageIdx == pageCount - 1)));
        }

        return new Pagination(links);
    }

    public String createPaginatedPath(String basePath, int page) {
        assert basePath.startsWith("/");
        if (page == 0) return basePath;
        return basePath + (page + 1) + "/";
    }

    /**
     * returns relative path back to / from the current path
     *
     * @param currentPath path from where the relative path to root is computed
     * @return this is "." if we are in the root directory, ".." if we are a level above, "../.." above, etc
     */
    public String getRelPath(String currentPath) {
        assert currentPath.startsWith("/");
        int nestingLevel = (int) currentPath.chars().filter((c) -> c == '/').count();
        assert nestingLevel > 0;
        if (nestingLevel == 1) return ".";
        String path = "../".repeat(nestingLevel - 1);
        return path.substring(0, path.length() - 1);
    }

    /**
     * create link to targetPath from the currentPath
     *
     * @param targetPath target path (starts and ends with a "/")
     * @return the SiteURL object for this path
     */
    public SiteURL createURL(String targetPath) {
        assert targetPath.startsWith("/");
        assert targetPath.endsWith("/");
        return new SiteURL(targetPath + "index.html");
    }

    /**
     * create all the files for this project
     */
    public void renderProject(Project project) throws IOException {
        // render main page
        renderHomepage(project);

        //render each entry
        renderArticles(project);

        //lists
        renderArticleList(project);

        renderTopicList(project);

        //each topic has a page
        renderTopics(project);

        //basic static elements
        copyCSS();
    }

    private List<Article> getSortedArticles(Project project) {
    return project.getArticles().stream()
            .sorted(new ArticleComparator(this.sorting, project))
            .collect(Collectors.toList());
    }
    
    public void renderHomepage(Project project) throws IOException {
        String relPath = getRelPath(HOME_ADDRESS);
        List<ArticlePreview> articles = getSortedArticles(project).stream()
            .limit(5)
            .map(a -> renderArticlePreview(a, relPath, ""))
            .collect(Collectors.toList());
//        List<Website.EventListing> upcomingEvents = genEventListing(project.getUpcomingEvents(5));
        List<EventListing> upcomingEvents = Collections.emptyList(); // not yet implemented
        SiteData siteData = genSiteData(project, relPath);
        Homepage homepage = new Homepage(
                siteData,
                articles,
                upcomingEvents,
                ARTICLES_LINK.getAddress(),
                EVENTS_LINK.getAddress());
        File targetFile = new File(this.targetDirectory, "index.html");
        this.templateEngine.render(homepage.getTemplate(), homepage, targetFile);
    }


    public List<EventListing> genEventListing(List<Event> events) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    public void copyCSS() throws IOException {
        File cssDir = new File(targetDirectory, "css");
        cssDir.mkdir();
        InputStream source = this.getClass().getResourceAsStream("/css/main.css");
        OutputStream out = new FileOutputStream(new File(cssDir, "main.css"));
        IOUtils.copy(source, out);
    }

    public void renderArticles(Project project) throws IOException {
        for (Article article : project.getArticles()) {
            renderArticleRecursive(project, article);
        }
    }

    private void renderArticleRecursive(Project project, Article article) throws IOException {
        renderArticle(project, article);
        for (Article child : article.getInnerArticles()) {
            renderArticleRecursive(project, child);
        }
    }

    public void renderArticle(Project project, Article article) throws IOException {
        String pagePath = getArticlePath(article);
        String relPath = getRelPath(pagePath);
        SiteData siteData = genSiteData(project, relPath);
        List<SiteLink> topics = project.getTopics(article)
                .stream().sorted().map(this::mkTopicLink).collect(Collectors.toList());
        List<SiteLink> breadcrumbs = getBreadcrumbs(article);

        ArticlePage page = new ArticlePage(
                siteData,
                article.getTitle(),
                breadcrumbs,
                WebGen.readableFormat(article.getPublishedDate()),
                topics,
                getArticleContent(article, relPath));
        File targetFile = new File(new File(this.targetDirectory, pagePath), "index.html");
        this.templateEngine.render(page.getTemplate(), page, targetFile);
    }

    /**
     * links for breadcrumb navigation for Entries
     *
     * @param article target entry
     * @return list of links to this and its parent entries
     */
    public List<SiteLink> getBreadcrumbs(Article article) {
        List<SiteLink> result = new ArrayList<>();
        // add all ancestors
        for(Article ancestor:article.getAncestors()){
            result.add(new SiteLink(getArticleURL(ancestor), ancestor.getTitle(), true));
        }
        // add current article
        result.add(new SiteLink(getArticleURL(article), article.getTitle(), true));
        return result;
    }

    

    public void renderEvent(Project project, Event event) throws IOException {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    /**
     * basic site information used on every page, including title and owner
     *
     * @param project the project to render
     * @param relPath relative path to the current page
     * @return SiteData object
     */
    public SiteData genSiteData(Project project, String relPath) {
        return new SiteData(
                relPath,
                project.getTitle(),
                project.getOwnerOrg(),
                genHeaders(project),
                this.siteGenerationTime);
    }

    /**
     * creates a ContentFragment object that contains HTML output for a node.
     * The HTML output for a node is created by invoking the rendering engine on a template
     *
     * @param storyNode node to render
     * @param relPath   relative path of the current page
     * @return a ContentFragment
     */
    public ContentFragment getStoryContentFragment(AbstractContent storyNode, String relPath) throws IOException {
        return storyNode.toContentFragment(this.templateEngine, relPath);
    }

    /**
     * collect all the content fragments of an entry
     */
    public List<ContentFragment> getArticleContent(Article story, String relPath) throws IOException {
        List<ContentFragment> result = new ArrayList<>();
        for (AbstractContent content : story.getContent())
            result.add(getStoryContentFragment(content, relPath));
        for (Article child : story.getInnerArticles())
            result.add(getArticleFragment(child, relPath));
        return result;
    }

    /**
     * Create a preview fragment for a child article.
     * This replaces getSubArticleFragment, getSubSubArticleFragment.
     */
    public ContentFragment getArticleFragment(Article article, String relPath) throws IOException {
        StringWriter w = new StringWriter();
        this.templateEngine.render("article-preview",
                renderArticlePreview(article, relPath, "Read on: "), w);
        return new ContentFragment(article.getTitle(), w.toString());
    }

    public void renderTopics(Project project) throws IOException {
        for (Topic topic : findAllTopics(project)) {
            renderTopic(project, topic);
        }
    }


    public List<Article> findAllArticles(Project project) {
        List<Article> result = new ArrayList<>();
        
        for (Article a : getSortedArticles(project)) {
            collectArticlesRecursive(a, result);
        }
        return result;
    }

    /**
     * Recursively collect all articles in tree.
     */
    private void collectArticlesRecursive(Article article, List<Article> accumulator) {
        accumulator.add(article);
        for (Article child : article.getInnerArticles()) {
            collectArticlesRecursive(child, accumulator);
        }
    }

    public void renderTopic(Project project, Topic topic) throws IOException {
        List<Article> allArticles = findArticlesByTopic(project, topic);
        List<List<Article>> articlePages = WebGen.paginateContent(allArticles.iterator(), 5);
        String basePath = getTopicPath(topic);
        for (int pageIdx = 0; pageIdx < articlePages.size(); pageIdx++) {
            String pagePath = createPaginatedPath(basePath, pageIdx);
            List<Article> articles = articlePages.get(pageIdx);
            Pagination pagination = createPagination(pageIdx, articlePages.size(), (i) -> createURL(createPaginatedPath(basePath, i)));
            List<ArticlePreview> previews = new ArrayList<>();
            String relPath = getRelPath(pagePath);
            for (Article article : articles) {
                previews.add(renderArticlePreview(article, relPath, ""));
            }

            ArticleListPage page = new ArticleListPage(
                    genSiteData(project, relPath),
                    "Articles for: " + topic.name(),
                    hasPagination(pagination),
                    pagination,
                    previews);
            File targetFile = new File(new File(this.targetDirectory, pagePath), "index.html");
            this.templateEngine.render(page.getTemplate(), page, targetFile);
        }
    }

    /**
     * Find all articles for a specific topic at all nesting levels.
     */
    private List<Article> findArticlesByTopic(Project project, Topic topic) {
        List<Article> result = new ArrayList<>();
        for (Article rootArticle : project.getArticles()) {
            collectArticlesByTopicRecursive(project, rootArticle, topic, result);
        }
        return result;
    }

    /**
     * Recursively collect articles matching a topic.
     */
    private void collectArticlesByTopicRecursive(Project project, Article article, 
                                                 Topic topic, List<Article> accumulator) {
        if (project.getTopics(article).contains(topic)) {
            accumulator.add(article);
        }
        for (Article child : article.getInnerArticles()) {
            collectArticlesByTopicRecursive(project, child, topic, accumulator);
        }
    }

    public boolean hasPagination(Pagination pagination) {
        if (pagination.getPages().size() == 0) return false;
        if (pagination.getPages().size() > 1) return true;
        return pagination.getPages().get(0).size() != 1;
    }

    public Set<Topic> findAllTopics(Project project) {
        Set<Topic> topics = new HashSet<>();
        
        for (Article a : getSortedArticles(project)) {
            collectTopicsRecursive(project,a,topics);
        }
        return topics;
    }

    /**
     * Recursively collect all topics from an article and its descendants.
     */
    private void collectTopicsRecursive(Project project, Article article, Set<Topic> accumulator) {
        accumulator.addAll(project.getTopics(article));
        
        for (Article child : article.getInnerArticles()) {
            collectTopicsRecursive(project, child, accumulator);
        }
    }

    public void renderTopicList(Project project) throws IOException {
        List<List<Topic>> topicPages = WebGen.paginateContent(findAllTopics(project).iterator(), 5);
        String basePath = TOPICS_ADDRESS;
        for (int pageIdx = 0; pageIdx < topicPages.size(); pageIdx++) {
            String pagePath = createPaginatedPath(basePath, pageIdx);
            List<SiteLink> topics = topicPages.get(pageIdx).stream().map(this::mkTopicLink).collect(Collectors.toList());
            Pagination pagination = createPagination(pageIdx, topicPages.size(),
                    (i) -> createURL(createPaginatedPath(basePath, i)));
            TopicListPage page = new TopicListPage(
                    genSiteData(project, getRelPath(pagePath)),
                    "Topics",
                    hasPagination(pagination),
                    pagination,
                    topics);
            File targetFile = new File(new File(this.targetDirectory, pagePath), "index.html");
            this.templateEngine.render(page.getTemplate(), page, targetFile);
        }
    }

    public SiteLink mkTopicLink(Topic topic) {
        return new SiteLink(createURL(getTopicPath(topic)), topic.name());
    }

    public String getTopicPath(Topic topic) {
        return TOPICS_ADDRESS + topic.getId() + "/";
    }

    public SiteURL getArticleURL(Article article) {
        return createURL(getArticlePath(article));
    }

    /**
     * get a path with all the parents of other (sub)articles
     * or (sub)events -- this corresponds to the relative URL of
     * the article
     */
    public String getArticlePath(Article article) {
        return ENTRY_ADDRESS + article.getFullPath();
    }

    public void renderArticleList(Project project) throws IOException {
        List<List<Article>> articlePages = WebGen.paginateContent(findAllArticles(project).iterator(), 5);
        String basePath = ARTICLES_ADDRESS;
        for (int pageIdx = 0; pageIdx < articlePages.size(); pageIdx++) {
            String pagePath = createPaginatedPath(basePath, pageIdx);
            List<Article> articles = articlePages.get(pageIdx);
            Pagination pagination = createPagination(pageIdx, articlePages.size(),
                    (i) -> createURL(createPaginatedPath(basePath, i)));
            List<ArticlePreview> previews = new ArrayList<>();
            String relPath = getRelPath(pagePath);
            for (Article article : articles) {
                previews.add(renderArticlePreview(article, relPath, ""));
            }

            ArticleListPage page = new ArticleListPage(
                    genSiteData(project, relPath),
                    "Articles",
                    hasPagination(pagination),
                    pagination,
                    previews);
            File targetFile = new File(new File(this.targetDirectory, pagePath), "index.html");
            this.templateEngine.render(page.getTemplate(), page, targetFile);
        }
    }


    public void renderEventList(Project project) throws IOException {
        throw new UnsupportedOperationException("Events not yet implemented");
    }

    /**
     * collect links for the navigation bar in the page header
     *
     * @param project project to be rendered
     * @return a list of named links
     */
    public List<SiteLink> genHeaders(Project project) {
        if (this.headers == null) {
            this.headers = new ArrayList<>(3);
            this.headers.add(this.HOME_LINK);
            this.headers.add(this.ARTICLES_LINK);
            if (!findAllTopics(project).isEmpty())
                this.headers.add(this.TOPICS_LINK);
        }
        return this.headers;
    }

    /**
     * create a preview snippet of an article
     *
     * @param article the article
     * @param relPath the relative path of the current page
     * @param prefix  a prefix for the article's title
     * @return an ArticlePreview object for the template engine
     */
    public ArticlePreview renderArticlePreview(Article article, String relPath, String prefix) {
        StringWriter w = new StringWriter();
        int previewLength = 200;
        for (AbstractContent c : article.getContent()) {
            if (c instanceof FormattedTextDocument&& previewLength > 0) {
                previewLength = ((FormattedTextDocument) c).toPreview(w, previewLength);
            }
        }

        return new ArticlePreview(
                prefix,
                article.getTitle(),
                WebGen.readableFormat(article.getPublishedDate()),
                w.toString(),
                relPath,
                getArticleURL(article));
    }

    /**
     * create a preview snippet of an event
     *
     * @param event   the event
     * @param relPath the relative path of the current page
     * @return an EventPreview object for the template engine
     */
    public EventPreview renderEventPreview(Event event, String relPath) throws IOException {
        throw new UnsupportedOperationException("Events not yet implemented");
    }
}
