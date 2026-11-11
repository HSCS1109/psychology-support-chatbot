# -*- coding: utf-8 -*-
"""
trace回放脚本：读取后端落盘的JSONL，按traceId还原一次调用的完整链路。
用法：
  python replay_trace.py                 列出所有trace摘要
  python replay_trace.py <traceId前缀>   查看某条trace完整详情
  python replay_trace.py --stat          汇总统计（成功率、重试、工具调用等）
"""
import json
import os
import sys
from collections import Counter

# trace文件相对eval目录的位置
TRACE_PATH = os.path.join(
    os.path.dirname(os.path.abspath(__file__)),
    "..", "ai-springboot", "logs", "trace", "trace.jsonl"
)


def load_traces():
    if not os.path.exists(TRACE_PATH):
        print("未找到trace文件：%s" % os.path.abspath(TRACE_PATH))
        print("请先在前端或评测脚本中产生对话，再运行本脚本。")
        return []
    traces = []
    with open(TRACE_PATH, "r", encoding="utf-8") as f:
        for line_no, line in enumerate(f, 1):
            line = line.strip()
            if not line:
                continue
            try:
                traces.append(json.loads(line))
            except json.JSONDecodeError as e:
                print("第%d行JSON解析失败，已跳过：%s" % (line_no, e))
    return traces


def list_traces(traces):
    print("共 %d 条trace\n" % len(traces))
    print("%-10s %-22s %-16s %-8s %-6s %s" %
          ("序号", "开始时间", "意图", "状态", "工具数", "traceId"))
    for i, t in enumerate(traces, 1):
        t_time = t.get("startTime", "")[4:19]
        intent = t.get("intent", t.get("safetyGuard", "-"))
        tools = len(t.get("toolCalls", []))
        print("%-10d %-22s %-16s %-8s %-6d %s" %
              (i, t_time, intent, t.get("status", "-"), tools, t.get("traceId", "")[:8]))


def show_detail(traces, prefix):
    matched = [t for t in traces if t.get("traceId", "").startswith(prefix)]
    if not matched:
        print("没有以 %s 开头的traceId" % prefix)
        return
    if len(matched) > 1:
        print("匹配到多条，请用更长的前缀：")
        for t in matched:
            print(" ", t.get("traceId"))
        return
    t = matched[0]
    print("=" * 60)
    print("traceId    :", t.get("traceId"))
    print("开始时间   :", t.get("startTime"))
    print("结束时间   :", t.get("endTime"))
    print("会话ID     :", t.get("sessionId"))
    print("最终状态   :", t.get("status"))
    if "safetyGuard" in t:
        print("安全护栏   :", t.get("safetyGuard"))
    print("-" * 60)
    print("用户输入   :", t.get("userInput"))
    if "intent" in t:
        print("意图类型   :", t.get("intent"))
        print("识别来源   :", t.get("intentSource"))
        print("置信度     :", t.get("intentConfidence"))
    print("-" * 60)
    tools = t.get("toolCalls", [])
    print("工具调用   : %d次" % len(tools))
    for j, c in enumerate(tools, 1):
        print("  [%d] %s 参数=%s 状态=%s 耗时=%sms" %
              (j, c.get("toolName"), c.get("param"),
               c.get("status"), c.get("durationMs")))
    print("重试次数   :", t.get("retryCount", 0))
    print("-" * 60)
    print("token用量  : prompt=%s completion=%s total=%s" %
          (t.get("promptTokens", "-"), t.get("completionTokens", "-"),
           t.get("totalTokens", "-")))
    print("回复长度   :", t.get("responseLength", "-"))
    print("-" * 60)
    print("最终回复   :", t.get("finalResponse"))
    print("=" * 60)


def show_stat(traces):
    total = len(traces)
    if total == 0:
        print("无数据")
        return
    status_counter = Counter(t.get("status", "UNKNOWN") for t in traces)
    intent_counter = Counter(t.get("intent", t.get("safetyGuard", "UNKNOWN")) for t in traces)
    retried = sum(1 for t in traces if t.get("retryCount", 0) > 0)
    tool_calls = [c for t in traces for c in t.get("toolCalls", [])]
    tool_status = Counter(c.get("status") for c in tool_calls)
    durations = [c.get("durationMs") for c in tool_calls if c.get("durationMs") is not None]

    print("trace总数      :", total)
    print("状态分布       :", dict(status_counter))
    print("意图分布       :", dict(intent_counter))
    print("触发重试的trace:", retried)
    print("工具调用总数   :", len(tool_calls))
    print("工具状态分布   :", dict(tool_status))
    if durations:
        print("工具平均耗时   : %.0fms" % (sum(durations) / len(durations)))
        print("工具最大耗时   : %sms" % max(durations))


def main():
    traces = load_traces()
    if not traces:
        return
    arg = sys.argv[1] if len(sys.argv) > 1 else None
    if arg == "--stat":
        show_stat(traces)
    elif arg:
        show_detail(traces, arg)
    else:
        list_traces(traces)


if __name__ == "__main__":
    main()
