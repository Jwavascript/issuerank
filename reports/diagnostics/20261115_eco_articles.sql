-- 2026-11-15에 경제(eco)로 수집한 기사와 현재 연결 이슈를 조회합니다.
-- 읽기 전용 SELECT이며 기사·이슈·요약·북마크를 변경하지 않습니다.
-- issue_id가 NULL이면 미연결입니다. 제외 가능성이 있지만 제외 사유는 이 DB에 없습니다.
SELECT
  a.id AS article_id,
  a.title AS collected_title,
  a.source_id,
  a.section AS collected_section,
  a.issue_id,
  i.section AS final_section,
  i.title AS issue_title,
  i.article_count,
  CASE
    WHEN a.issue_id IS NULL THEN '미연결(제외 가능)'
    WHEN i.section = 'eco' THEN '경제 유지'
    ELSE '다른 섹션으로 분류'
  END AS classification
FROM public.articles AS a
LEFT JOIN public.issues AS i
  ON i.id = a.issue_id AND i.date = a.date
WHERE a.date = DATE '2026-11-15'
  AND a.section = 'eco'
ORDER BY a.id;
