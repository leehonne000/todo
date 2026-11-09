package com.leehonne.examtimeline.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.leehonne.examtimeline.common.StudyDays;
import com.leehonne.examtimeline.entity.CheckIn;
import com.leehonne.examtimeline.entity.Entry;
import com.leehonne.examtimeline.entity.StudySession;
import com.leehonne.examtimeline.entity.Subject;
import com.leehonne.examtimeline.enums.EntryType;
import com.leehonne.examtimeline.exception.BusinessException;
import com.leehonne.examtimeline.mapper.CheckInMapper;
import com.leehonne.examtimeline.mapper.EntryMapper;
import com.leehonne.examtimeline.mapper.StudySessionMapper;
import com.leehonne.examtimeline.mapper.SubjectMapper;
import com.leehonne.examtimeline.service.ProgressService;
import com.leehonne.examtimeline.vo.DayStatVO;
import com.leehonne.examtimeline.vo.ProgressVO;
import com.leehonne.examtimeline.vo.SubjectSecondsVO;
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
 * 进度模块业务实现——持久层 MyBatis-Plus（BaseMapper + LambdaQueryWrapper）。
 */
@Service
@RequiredArgsConstructor
public class ProgressServiceImpl implements ProgressService {

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final StudySessionMapper sessionMapper;
    private final EntryMapper entryMapper;
    private final CheckInMapper checkInMapper;
    private final SubjectMapper subjectMapper;

    @Override
    public ProgressVO progress(Long userId, String fromStr, String toStr) {
        LocalDate today = StudyDays.today();
        LocalDate to = parseDay(toStr, today, "to");
        LocalDate from = parseDay(fromStr, to.withDayOfMonth(1), "from");
        if (from.isAfter(to)) {
            throw BusinessException.badRequest("from 不能晚于 to");
        }

        // 学习日区间 [from, to] 的时间窗口 = [from 04:00, to+1 04:00)
        LocalDateTime winStart = StudyDays.dayStart(from);
        LocalDateTime winEnd = StudyDays.dayStart(to.plusDays(1));

        // ---- 时长：session 按学习日 + 科目汇总 ----
        Map<Long, String> subjectNames = new HashMap<>();
        for (Subject s : subjectMapper.selectList(new LambdaQueryWrapper<Subject>()
                .eq(Subject::getUserId, userId)
                .orderByAsc(Subject::getSortOrder))) {
            subjectNames.put(s.getId(), s.getName());
        }
        // day -> (subjectName -> seconds)
        Map<LocalDate, Map<String, Integer>> byDaySubject = new HashMap<>();
        Map<LocalDate, Integer> daySeconds = new HashMap<>();
        for (StudySession s : sessionMapper.selectList(new LambdaQueryWrapper<StudySession>()
                .eq(StudySession::getUserId, userId)
                .ge(StudySession::getStartedAt, winStart)
                .lt(StudySession::getStartedAt, winEnd))) {
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
        for (Entry e : entryMapper.selectList(new LambdaQueryWrapper<Entry>()
                .eq(Entry::getUserId, userId)
                .ge(Entry::getCreatedAt, winStart)
                .lt(Entry::getCreatedAt, winEnd))) {
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
        for (CheckIn c : checkInMapper.selectList(new LambdaQueryWrapper<CheckIn>()
                .eq(CheckIn::getUserId, userId)
                .ge(CheckIn::getCheckInDate, from)
                .le(CheckIn::getCheckInDate, to)
                .orderByAsc(CheckIn::getCheckInDate))) {
            checkInDates.add(DAY_FMT.format(c.getCheckInDate()));
        }

        // ---- 稠密 days：[from, to] 每天一项（无数据=0/空，与前端 mock 语义一致）----
        List<DayStatVO> days = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            Map<String, Integer> subj = byDaySubject.getOrDefault(d, Map.of());
            List<SubjectSecondsVO> bySubject = subj.entrySet().stream()
                    .map(en -> new SubjectSecondsVO(en.getKey(), en.getValue()))
                    .sorted(Comparator.comparingInt(SubjectSecondsVO::getSeconds).reversed())
                    .toList();
            int[] c = dayCounts.getOrDefault(d, new int[2]);
            days.add(new DayStatVO(
                    d,
                    daySeconds.getOrDefault(d, 0),
                    bySubject,
                    c[0],
                    c[1]));
        }
        return new ProgressVO(days, checkInDates);
    }

    private LocalDate parseDay(String s, LocalDate fallback, String field) {
        if (s == null || s.isBlank()) {
            return fallback;
        }
        try {
            return LocalDate.parse(s.trim(), DAY_FMT);
        } catch (DateTimeParseException e) {
            throw BusinessException.badRequest(field + " 日期格式错误（应为 yyyy-MM-dd）");
        }
    }
}
