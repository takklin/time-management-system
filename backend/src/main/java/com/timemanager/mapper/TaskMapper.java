package com.timemanager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.timemanager.entity.Task;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface TaskMapper extends BaseMapper<Task> {
	// 近N天每天任务创建数
	@Select("SELECT DATE(created_at) as date, COUNT(*) as count FROM task WHERE deleted=0 AND created_at >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY) GROUP BY DATE(created_at) ORDER BY date")
	List<Map<String, Object>> countCreatedByDay(int days);

	// 近N天每天任务完成数
	@Select("SELECT DATE(completed_at) as date, COUNT(*) as count FROM task WHERE deleted=0 AND status=1 AND completed_at IS NOT NULL AND completed_at >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY) GROUP BY DATE(completed_at) ORDER BY date")
	List<Map<String, Object>> countFinishedByDay(int days);

	// 近N天任务完成率
	@Select("SELECT IFNULL(ROUND(SUM(CASE WHEN status=1 AND completed_at IS NOT NULL THEN 1 ELSE 0 END)/COUNT(*), 4)*100, 0) FROM task WHERE deleted=0 AND created_at >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY)")
	Double calcFinishRateInLastDays(int days);

	// 近N天任务创建总数
	@Select("SELECT COUNT(*) FROM task WHERE deleted=0 AND created_at >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY)")
	int countCreatedInLastDays(int days);

	// 近N天任务完成总数
	@Select("SELECT COUNT(*) FROM task WHERE deleted=0 AND status=1 AND completed_at IS NOT NULL AND completed_at >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY)")
	int countFinishedInLastDays(int days);

	// 日期范围内创建的任务数
	@Select("SELECT COUNT(*) FROM task WHERE deleted=0 AND created_at >= #{startTime} AND created_at <= #{endTime}")
	int countCreatedInRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

	// 日期范围内完成的任务数
	@Select("SELECT COUNT(*) FROM task WHERE deleted=0 AND status=1 AND completed_at >= #{startTime} AND completed_at <= #{endTime}")
	int countFinishedInRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

	// 查询全部任务（带分类名）
	@Select("SELECT t.*, c.name AS categoryName FROM task t LEFT JOIN category c ON t.category_id = c.id WHERE t.deleted = 0 AND t.user_id = #{userId} ORDER BY t.created_at DESC")
	List<Task> selectWithCategoryName(Long userId);

	// 按分类查任务（带分类名）
	@Select("SELECT t.*, c.name AS categoryName FROM task t LEFT JOIN category c ON t.category_id = c.id WHERE t.deleted = 0 AND t.user_id = #{userId} AND t.category_id = #{categoryId} ORDER BY t.created_at DESC")
	List<Task> selectWithCategoryNameByCategory(Long userId, Long categoryId);
}
