package com.local.ar44.service;

import com.local.ar44.dto.AppAccessLog;
import com.local.ar44.dto.Video;
import com.local.ar44.dto.VideoWatchLog;
import com.local.ar44.repo.AppAccessLogRepository;
import com.local.ar44.repo.VideoRepository;
import com.local.ar44.repo.VideoWatchLogRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StatsService {
    private final AppAccessLogRepository appAccessLogRepository;
    private final VideoWatchLogRepository videoWatchLogRepository;
    private final VideoRepository videoRepository;

    public StatsService(AppAccessLogRepository appAccessLogRepository,
                        VideoWatchLogRepository videoWatchLogRepository,
                        VideoRepository videoRepository) {
        this.appAccessLogRepository = appAccessLogRepository;
        this.videoWatchLogRepository = videoWatchLogRepository;
        this.videoRepository = videoRepository;
    }

    public void logAccess(String page, String username) {
        AppAccessLog log = new AppAccessLog();
        log.setPage(page);
        log.setUsername(username);
        log.setAccessedAt(LocalDateTime.now());
        appAccessLogRepository.save(log);
    }

    public void logWatchSession(Long videoId, String page, String username, Integer watchedSeconds) {
        VideoWatchLog log = new VideoWatchLog();
        log.setVideoId(videoId);
        log.setPage(page);
        log.setUsername(username);
        log.setWatchedSeconds(Math.max(0, watchedSeconds == null ? 0 : watchedSeconds));
        log.setWatchedAt(LocalDateTime.now());
        videoWatchLogRepository.save(log);
    }

    public Map<String, Object> getOverview() {
        List<AppAccessLog> accesses = appAccessLogRepository.findAll();
        List<VideoWatchLog> watches = videoWatchLogRepository.findAll();

        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(6);

        long totalAppAccesses = accesses.size();
        long todayAppAccesses = accesses.stream()
                .filter(a -> a.getAccessedAt() != null && a.getAccessedAt().toLocalDate().equals(today))
                .count();

        Map<Integer, Long> hourCounts = accesses.stream()
                .filter(a -> a.getAccessedAt() != null && a.getAccessedAt().toLocalDate().equals(today))
                .collect(Collectors.groupingBy(a -> a.getAccessedAt().getHour(), Collectors.counting()));

        List<Map<String, Object>> accessesByHourToday = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("hour", String.format("%02d:00", h));
            row.put("count", hourCounts.getOrDefault(h, 0L));
            accessesByHourToday.add(row);
        }

        Map<LocalDate, Long> accessByDay = accesses.stream()
                .filter(a -> a.getAccessedAt() != null)
                .map(a -> a.getAccessedAt().toLocalDate())
                .filter(d -> !d.isBefore(from) && !d.isAfter(today))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        List<Map<String, Object>> accessesLast7Days = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("day", d.toString());
            row.put("count", accessByDay.getOrDefault(d, 0L));
            accessesLast7Days.add(row);
        }

        long totalVideoViews = watches.size();
        long todayVideoViews = watches.stream()
                .filter(w -> w.getWatchedAt() != null && w.getWatchedAt().toLocalDate().equals(today))
                .count();

        long totalWatchSeconds = watches.stream()
                .map(VideoWatchLog::getWatchedSeconds)
                .filter(Objects::nonNull)
                .mapToLong(Integer::longValue)
                .sum();

        double averageWatchSeconds = totalVideoViews > 0 ? (double) totalWatchSeconds / totalVideoViews : 0d;

        Map<LocalDate, Long> viewCountByDay = watches.stream()
                .filter(w -> w.getWatchedAt() != null)
                .collect(Collectors.groupingBy(w -> w.getWatchedAt().toLocalDate(), Collectors.counting()));

        Map<LocalDate, Long> watchSecondsByDay = watches.stream()
                .filter(w -> w.getWatchedAt() != null)
                .collect(Collectors.groupingBy(w -> w.getWatchedAt().toLocalDate(),
                        Collectors.summingLong(w -> w.getWatchedSeconds() == null ? 0 : w.getWatchedSeconds())));

        List<Map<String, Object>> viewsLast7Days = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("day", d.toString());
            row.put("views", viewCountByDay.getOrDefault(d, 0L));
            row.put("watchSeconds", watchSecondsByDay.getOrDefault(d, 0L));
            viewsLast7Days.add(row);
        }

        Map<Long, List<VideoWatchLog>> groupedByVideo = watches.stream()
                .filter(w -> w.getVideoId() != null)
                .collect(Collectors.groupingBy(VideoWatchLog::getVideoId));

        List<Long> ids = new ArrayList<>(groupedByVideo.keySet());
        Map<Long, String> titlesById = videoRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Video::getId, v -> v.getTitle() == null ? "(Sans titre)" : v.getTitle()));

        List<Map<String, Object>> topVideos = groupedByVideo.entrySet().stream()
                .map(e -> {
                    Long id = e.getKey();
                    List<VideoWatchLog> list = e.getValue();
                    long views = list.size();
                    long seconds = list.stream()
                            .map(VideoWatchLog::getWatchedSeconds)
                            .filter(Objects::nonNull)
                            .mapToLong(Integer::longValue)
                            .sum();
                    LocalDateTime last = list.stream()
                            .map(VideoWatchLog::getWatchedAt)
                            .filter(Objects::nonNull)
                            .max(LocalDateTime::compareTo)
                            .orElse(null);

                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("videoId", id);
                    row.put("title", titlesById.getOrDefault(id, "Vidéo #" + id));
                    row.put("views", views);
                    row.put("watchSeconds", seconds);
                    row.put("lastViewedAt", last != null ? last.toString() : null);
                    return row;
                })
                .sorted((a, b) -> Long.compare((Long) b.get("views"), (Long) a.get("views")))
                .limit(10)
                .toList();

        List<Map<String, Object>> recentAccesses = accesses.stream()
                .filter(a -> a.getAccessedAt() != null)
                .sorted((a, b) -> b.getAccessedAt().compareTo(a.getAccessedAt()))
                .limit(30)
                .map(a -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("page", a.getPage());
                    row.put("username", a.getUsername());
                    row.put("at", a.getAccessedAt().toString());
                    return row;
                })
                .toList();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("totalAppAccesses", totalAppAccesses);
        out.put("todayAppAccesses", todayAppAccesses);
        out.put("accessesByHourToday", accessesByHourToday);
        out.put("accessesLast7Days", accessesLast7Days);

        out.put("totalVideoViews", totalVideoViews);
        out.put("todayVideoViews", todayVideoViews);
        out.put("totalWatchSeconds", totalWatchSeconds);
        out.put("averageWatchSeconds", averageWatchSeconds);
        out.put("viewsLast7Days", viewsLast7Days);
        out.put("topVideos", topVideos);

        out.put("recentAccesses", recentAccesses);
        return out;
    }
}
