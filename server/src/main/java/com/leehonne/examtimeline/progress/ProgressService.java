package com.leehonne.examtimeline.progress;

import com.leehonne.examtimeline.common.BizException;
import com.leehonne.examtimeline.common.StudyDays;
import com.leehonne.examtimeline.entity.CheckIn;
import com.leehonne.examtimeline.entity.Entry;
import com.leehonne.examtimeline.entity.StudySession;
import com.leehonne.examtimeline.entity.Subject;
import com.leehonne.examtimeline.enums.EntryType;
import com.leehonne.examtimeline.repository.CheckInRepository;
import com.leehonne.examtimeline.repository.EntryRepository;
import com.leehonne.examtimeline.repository.StudySessionRepository;
import com.leehonne.examtimeline.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 进度模块业务（契约 §5.3 / §8.1）：统计页「学习进度总结」的数据引擎。
 *
 * <p>口径（数据库设计 §4，全部精确）：
 * <ul>
 *   <li>时长：session 净学习秒数按 {@code started_at} 的<strong>学习日</strong> + 科目汇总；
 *       科目来自 session.subject_id（一次 session 一门，2026-10-24 起精确统计）</li>
 *   <li>条数：entry 按 {@code created_at} 的学习日 + type（1=note / 2=错题）计数</li>
 *   <li>签到：t_check_in.check_in_date 落在区间内即计入</li>
 *   <li>「天」= 04:00–次日 04:00，记在起始日（StudyDays）</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ProgressService {

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final StudySessionRepository sessionRepository;
    private final EntryRepository entryRepository;
    private final CheckInRepository checkInRepository;
    private final SubjectRepository subjectRepository;

    /** 统计（§8.1）：from/to 缺省 = 本月 1 日 ~ 今天（学习日） */
    public ProgressResponse progress(Long userId, String fromStr, String toStr) {
        LocalDate today = StudyDays.today();
        LocalDate to = parseDay(toStr, today, "to");
        LocalDate from = parseDay(fromStr, to.withDayOfMonth(1), "from");
        if (from.isAfter(to)) {
            throw BizException.badRequest("from 不能晚于 to");
        }

        // 学习日区间 [from, to] 的时间窗口 = [from 04:00, to+1 04:00)
        LocalDateTime winStart = StudyDays.dayStart(from);
        LocalDateTime winEnd = StudyDays.dayStart(to.plusDays(1));

        // ---- 时长：session 按学习日 + 科目汇总 ----
        Map<Long, String> subjectNames = new HashMap<>();
        for (Subject s : subjectRepository.findByUserIdOrderBySortOrderAsc(userId)) {
            subjectNames.put(s.getId(), s.getName());
        }
        // day -> (subjectName -> seconds)
        Map<LocalDate, Map<String, Integer>> byDaySubject = new HashMap<>();
        Map<LocalDate, Integer> daySeconds = new HashMap<>();
        for (StudySession s : sessionRepository.findByUserIdAndStartedAtGreaterThanEqualAndStartedAtLessThan(
                userId, winStart, winEnd)) {
            LocalDate day = StudyDays.studyDate(s.getStartedAt());
            int sec = s.getDurationSeconds() == null ? 0 : s.getDurationSeconds();
            daySeconds.merge(day, sec, Integer::sum);
            String name = subjectNames.get(s.getSubjectId());
            if (name == null) {
                continue; // 科目被删不可能（FK RESTRICT），防御性跳过
            }
            byDaySubject.computeIfAbsent(day, k -> new HashMap<>()).merge(name, sec, Integer::sum);
        }

        // ---- 条数：entry 按学习日 + type 计数 ----
        Map<LocalDate, int[]> dayCounts = new HashMap<>(); // [note, wrong]
        for (Entry e : entryRepository.findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                userId, winStart, winEnd)) {
            LocalDate day = StudyDays.studyDate(e.getCreatedAt());
            int[] c = dayCounts.computeIfAbsent(day, k -> new int[2]);
            if (e.getType() == EntryType.WRONG) {
                c[1]++;
            } else {
                c[0]++;
            }
        }

        // ---- 签到日期 ----
        List<String> checkInDates = new ArrayList<>();
        for (CheckIn c : checkInRepository.findByUserIdAndCheckInDateGreaterThanEqualAndCheckInDateLessThanEqualOrderByCheckInDateAsc(
                userId, from, to)) {
            checkInDates.add(DAY_FMT.format(c.getCheckInDate()));
        }

        // ---- 稠密 days：[from, to] 每天一项（无数据=0/空，与前端 mock 语义一致）----
        List<DayStatResponse> days = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            Map<String, Integer> subj = byDaySubject.getOrDefault(d, Map.of());
            List<SubjectSecondsResponse> bySubject = subj.entrySet().stream()
                    .map(en -> new SubjectSecondsResponse(en.getKey(), en.getValue()))
                    .sorted(Comparator.comparingInt(SubjectSecondsResponse::getSeconds).reversed())
                    .toList();
            int[] c = dayCounts.getOrDefault(d, new int[2]);
            days.add(new DayStatResponse(
                    d,
                    daySeconds.getOrDefault(d, 0),
                    bySubject,
                    c[0],
                    c[1]));
        }
        return new ProgressResponse(days, checkInDates);
    }

    private LocalDate parseDay(String s, LocalDate fallback, String field) {
        if (s == null || s.isBlank()) {
            return fallback;
        }
        try {
            return LocalDate.parse(s.trim(), DAY_FMT);
        } catch (DateTimeParseException e) {
            throw BizException.badRequest(field + " 日期格式错误（应为 yyyy-MM-dd）");
        }
    }
}
