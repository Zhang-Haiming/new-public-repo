package edu.cmu.webgen;

import edu.cmu.webgen.parser.ProjectParser;
import edu.cmu.webgen.project.Article;
import edu.cmu.webgen.project.Project;
import edu.cmu.webgen.project.SubArticle;
import edu.cmu.webgen.project.SubSubArticle;
import edu.cmu.webgen.project.Topic;

import org.natty.Parser;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class WebGen {

    private static final Map<String, Integer> ID_COUNTER = new HashMap<>();
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT);

    public static void main(String[] args) {
        try {
            WebGenArgs options = new WebGenArgs(args);
            if (!options.projectSourceDirectoryExists() || options.isHelp()) {
                options.printHelp();
                return;
            }
            Project project = new ProjectParser().loadProject(options.getProjectSourceDirectory());
            new CLI(project).run(options);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * using external library to flexibly parse dates
     * <p>
     * requires a bit of hacking with different time formats
     *
     * @param inputDate input date string in human readable time/date format
     * @return parsed date as LocalDateTime
     * @throws ParseException if input date string cannot be parsed
     */
    public static LocalDateTime parseDate(String inputDate) throws ParseException {
        List<Date> dates = new Parser().parse(inputDate,
                Date.from(LocalDate.now().atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())).get(0).getDates();
        if (dates.isEmpty())
            throw new ParseException("Cannot parse date %s".formatted(inputDate), 0);
        return LocalDateTime.ofInstant(dates.get(0).toInstant(), ZoneId.systemDefault());
    }

    /**
     * print a date in a readable format
     *
     * @param date the date
     * @return string representing the date
     */
    public static String readableFormat(LocalDateTime date) {
        return FORMATTER.format(date.atZone(ZoneId.systemDefault()));
    }

    public static String genId(String title) {
        String id = title.toLowerCase().replaceAll("[^a-z0-9]", "_");
        if (ID_COUNTER.containsKey(id)) {
            int idIdx = ID_COUNTER.get(id) + 1;
            ID_COUNTER.put(id, idIdx);
            id = id + idIdx;
        } else
            ID_COUNTER.put(id, 1);
        return id;
    }

    /**
     * helper function to paginate content
     *
     * @param content  iterator of content
     * @param pageSize number of elements per page
     * @param <R>      type of the content
     * @return list of lists of content, where each inner list has pageSize entries (possibly except the last)
     */
    public static <R> List<List<R>> paginateContent(Iterator<R> content, int pageSize) {
        List<List<R>> result = new ArrayList<>();
        List<R> pageContent = new ArrayList<>(pageSize);
        while (content.hasNext()) {
            if (pageContent.size() >= pageSize) {
                result.add(pageContent);
                pageContent = new ArrayList<>(pageSize);
            }
            pageContent.add(content.next());
        }
        result.add(pageContent);
        return result;
    }

    public List<Object> findArticlesByTopic(Project project, Topic topic) {
        List<Object> result = new ArrayList<>();
        for (Article a : project.getArticles()) {
            if (project.getTopics(a).contains(topic))
                result.add(a);
            for (SubArticle sa : a.getInnerArticles()) {
                if (project.getTopics(sa).contains(topic))
                    result.add(sa);
                for (SubSubArticle ssa : sa.getInnerArticles()) {
                    if (project.getTopics(ssa).contains(topic))
                        result.add(ssa);
                }
            }
        }
        return result;
    }
}
