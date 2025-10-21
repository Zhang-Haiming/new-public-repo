package edu.cmu.webgen;

import edu.cmu.webgen.project.Article;
import edu.cmu.webgen.project.Project;
import edu.cmu.webgen.project.Topic;
import edu.cmu.webgen.rendering.ArticleComparator;
import edu.cmu.webgen.rendering.Renderer;
import edu.cmu.webgen.rendering.TemplateEngine;
import java.io.File;
import java.io.IOException;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Set;
import org.apache.commons.io.FileUtils;

/**
 * Command-line interface processing all actions
 */
public class CLI {

    private final Project project;

    public CLI(Project project) {
        this.project = project;
    }

    /**
     * run the program with the provided options
     *
     * @param options parsed command-line arguments
     */
    public void run(WebGenArgs options) {

        if (options.isListArticles())
            printArticles(options.isListAll(), options.isListTopics(), options.getArticleSorting());
        if (options.isListEvents())
            printEvents(options.isListAll(), options.isListTopics());
        if (options.isListTopics())
            printTopics();
        if (options.printSize())
            printSize();

        if (options.isRender()) {
            if (options.cleanTargetDirectory() && options.getTargetDirectory().exists()) {
                cleanTargetDirectory(options.getTargetDirectory());
            }
            options.getTargetDirectory().mkdirs();
            try {
                new Renderer(options.getTargetDirectory(), options.getArticleSorting(), new TemplateEngine())
                        .renderProject(this.project);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void printSize() {
        long size = this.project.getTotalSize();
        System.out.println("Project size in bytes: %d".formatted(size));
    }

    private void cleanTargetDirectory(File targetDirectory) {
        if (targetDirectory.exists()) {
            for (File f : targetDirectory.listFiles()) {
                if (f.isDirectory()) {
                    try {
                        FileUtils.deleteDirectory(f);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    f.delete();
                }
            }
        }
    }

    private void printTopics() {
        System.out.println("Topics: ");
        findAllTopics().stream().sorted().forEach(t -> System.out.println(" - %s".formatted(t.getName())));
    }

    private Set<Topic> findAllTopics() {
        return this.project.getAllTopics();
    }

    private void printArticles(boolean all, boolean topics, WebGenArgs.ArticleSorting sorting) {
        List<Article> topLeveLArticles = project.getArticles().stream()
            .sorted(new ArticleComparator(sorting, project))
            .collect(Collectors.toList());

        System.out.println("Articles: ");
        for(Article article: topLeveLArticles) {
            printArticleRecursive(article,0,all,topics);
        }
    }

    /**
     * Recursively print an article and its children.
     * This replaces the nested loops in the old code.
     * 
     * @param article The article to print
     * @param depth Current nesting depth (for indentation)
     * @param printChildren Whether to print child articles
     * @param showTopics Whether to show topic information
     */
    private void printArticleRecursive(Article article, int depth, 
                                      boolean printChildren, boolean showTopics) {
        // Create indentation based on depth (2 spaces per level)
        String indent = " ".repeat(depth * 2);
        
        // Format topics if requested
        String topicStr = showTopics ? getTopicsStr(this.project.getTopics(article)) : "";
        
        // Print this article
        System.out.println("%s - %s (%s) %s".formatted(
            indent,
            article.getTitle(),
            WebGen.readableFormat(article.getPublishedDate()),
            topicStr
        ));
        
        // Recursively print children if requested
        if (printChildren) {
            for (Article child : article.getInnerArticles()) {
                printArticleRecursive(child, depth + 1, printChildren, showTopics);
            }
        }
    }

    private void printEvents(boolean all, boolean topics) {
        System.out.println("Events not yet supported");
    }

    private String getTopicsStr(Set<Topic> topics) {
        return topics.stream().sorted().map(Topic::getName).collect(Collectors.joining(", ", " [", "]"));
    }
}
