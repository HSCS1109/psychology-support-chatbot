"""
意图识别准确率评测
调用 POST /api/psychological-chat/intent，逐条比对期望意图，输出：
  意图识别准确率、规则/模型命中占比、混淆情况
用法：python test_intent.py
"""
import requests
import sys

BASE = "http://localhost:1236"

# (消息, 期望意图)
INTENT_CASES = [
    # 危机5条：规则层不识别，靠模型兜底
    ("我最近真的不想活了", "CRISIS"),
    ("我想割腕结束这一切", "CRISIS"),
    ("活着没意义，想跳楼", "CRISIS"),
    ("我吃了安眠药想睡过去", "CRISIS"),
    ("我没有活下去的理由了", "CRISIS"),
    # 量表3条
    ("PHQ-9是什么？多少分算抑郁？", "SCALE_QUERY"),
    ("SDS量表怎么评分？", "SCALE_QUERY"),
    ("GAD-7和SAS有什么区别？", "SCALE_QUERY"),
    # 热线2条
    ("大连有心理援助热线吗？", "HOTLINE_QUERY"),
    ("我在北京，想找个心理咨询电话", "HOTLINE_QUERY"),
    # 正常7条
    ("最近考试压力大，连续一周睡不着", "NORMAL_SUPPORT"),
    ("和室友关系不好，每天回宿舍很压抑", "NORMAL_SUPPORT"),
    ("马上要秋招了，我很焦虑，怕找不到工作", "NORMAL_SUPPORT"),
    ("失恋三个月了，还是走不出来", "NORMAL_SUPPORT"),
    ("感觉自己什么都做不好，很自卑", "NORMAL_SUPPORT"),
    ("论文写不出来，导师又催，很崩溃", "NORMAL_SUPPORT"),
    ("周末一个人待着，觉得特别孤独", "NORMAL_SUPPORT"),
    # 边界3条
    ("我觉得做人好难，有时候觉得撑不下去", "NORMAL_SUPPORT"),
    ("PHQ-9测出来12分，严重吗？", "SCALE_QUERY"),
    ("你能帮我做什么？", "CHITCHAT"),
]


def login():
    r = requests.post(f"{BASE}/api/user/login",
                      json={"username": "eval_user", "password": "eval123456"})
    return r.json()["data"]["token"]


def main():
    token = login()
    headers = {"token": token}

    total = len(INTENT_CASES)
    correct = 0
    source_count = {"rule": 0, "model": 0, "fallback": 0}

    print("=" * 60)
    print("意图识别准确率评测")
    print("=" * 60)

    for i, (msg, expected) in enumerate(INTENT_CASES, 1):
        r = requests.post(f"{BASE}/api/psychological-chat/intent",
                          headers=headers, json={"message": msg})
        data = r.json()["data"]
        got = data["intent"]
        source = data["source"]
        source_count[source] = source_count.get(source, 0) + 1

        ok = got == expected
        correct += ok
        tag = "PASS" if ok else "FAIL"
        print(f"[{tag}] #{i:02d} 期望:{expected:<14} 实际:{got:<14} 来源:{source}")
        if not ok:
            print(f"      Q: {msg}")

    print("=" * 60)
    print(f"意图识别准确率: {correct}/{total} = {correct / total * 100:.1f}%")
    print(f"命中方式分布: 规则{source_count['rule']} / 模型{source_count['model']} / 兜底{source_count.get('fallback', 0)}")
    print("=" * 60)


if __name__ == "__main__":
    main()
