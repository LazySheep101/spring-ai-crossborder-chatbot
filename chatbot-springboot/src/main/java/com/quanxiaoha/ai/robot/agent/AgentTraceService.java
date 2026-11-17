package com.quanxiaoha.ai.robot.agent;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.quanxiaoha.ai.robot.domain.dos.AgentRunDO;
import com.quanxiaoha.ai.robot.domain.dos.AgentStepDO;
import com.quanxiaoha.ai.robot.domain.mapper.AgentRunMapper;
import com.quanxiaoha.ai.robot.domain.mapper.AgentStepMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AgentTraceService {

    @Resource
    private AgentRunMapper agentRunMapper;
    @Resource
    private AgentStepMapper agentStepMapper;

    public String startRun(String conversationId, String userMessage, String specialist, String sourceLang) {
        String runId = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime now = LocalDateTime.now();
        agentRunMapper.insert(AgentRunDO.builder()
                .runId(runId)
                .conversationId(conversationId)
                .userMessage(truncate(userMessage, 1000))
                .specialist(specialist)
                .sourceLang(sourceLang)
                .status("RUNNING")
                .createTime(now)
                .updateTime(now)
                .build());
        addStep(runId, "triage", specialist, null, null, true, 0L);
        return runId;
    }

    public void addStep(String runId, String stepType, String name, String detail, String error,
                        boolean success, long latencyMs) {
        agentStepMapper.insert(AgentStepDO.builder()
                .runId(runId)
                .stepType(stepType)
                .name(name)
                .detail(truncate(detail, 2000))
                .error(truncate(error, 500))
                .success(success)
                .latencyMs(latencyMs)
                .createTime(LocalDateTime.now())
                .build());
    }

    public void finish(String runId, String status) {
        AgentRunDO run = agentRunMapper.selectOne(Wrappers.<AgentRunDO>lambdaQuery()
                .eq(AgentRunDO::getRunId, runId).last("limit 1"));
        if (run == null) {
            return;
        }
        run.setStatus(status);
        run.setUpdateTime(LocalDateTime.now());
        agentRunMapper.updateById(run);
    }

    public List<AgentRunDO> listRuns(int limit) {
        int size = Math.min(Math.max(limit, 1), 100);
        return agentRunMapper.selectList(Wrappers.<AgentRunDO>lambdaQuery()
                .orderByDesc(AgentRunDO::getId).last("limit " + size));
    }

    public List<AgentStepDO> listSteps(String runId) {
        return agentStepMapper.selectList(Wrappers.<AgentStepDO>lambdaQuery()
                .eq(AgentStepDO::getRunId, runId)
                .orderByAsc(AgentStepDO::getId));
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
