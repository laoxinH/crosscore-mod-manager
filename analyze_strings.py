#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import os
import re
import xml.etree.ElementTree as ET
from pathlib import Path
from collections import defaultdict

# 项目根目录
project_root = "C:\\Users\\thixi\\Documents\\GitHub\\crosscore-mod-manager"
app_dir = os.path.join(project_root, "app", "src", "main")

# 读取strings.xml
def parse_strings_xml(filepath):
    """解析strings.xml文件，返回所有字符串名称"""
    strings = set()
    try:
        tree = ET.parse(filepath)
        root = tree.getroot()
        for string_elem in root.findall('string'):
            name = string_elem.get('name')
            if name:
                strings.add(name)
    except Exception as e:
        print(f"Error parsing {filepath}: {e}")
    return strings

# 扫描所有源代码文件中的字符串引用
def find_used_strings(root_dir):
    """扫描所有.kt和.xml文件，找出所有被引用的字符串"""
    used_strings = set()

    # 正则表达式匹配 R.string.xxx 和 @string/xxx
    patterns = [
        r'R\.string\.(\w+)',
        r'@string/(\w+)',
    ]

    # 扫描所有.kt文件
    for root, dirs, files in os.walk(os.path.join(root_dir, "kotlin")):
        for file in files:
            if file.endswith('.kt'):
                filepath = os.path.join(root, file)
                try:
                    with open(filepath, 'r', encoding='utf-8') as f:
                        content = f.read()
                        for pattern in patterns:
                            matches = re.findall(pattern, content)
                            used_strings.update(matches)
                except Exception as e:
                    print(f"Error reading {filepath}: {e}")

    # 扫描res目录下的所有.xml文件
    res_dir = os.path.join(root_dir, "res")
    if os.path.exists(res_dir):
        for root, dirs, files in os.walk(res_dir):
            for file in files:
                if file.endswith('.xml'):
                    filepath = os.path.join(root, file)
                    try:
                        with open(filepath, 'r', encoding='utf-8') as f:
                            content = f.read()
                            for pattern in patterns:
                                matches = re.findall(pattern, content)
                                used_strings.update(matches)
                    except Exception as e:
                        print(f"Error reading {filepath}: {e}")

    return used_strings

# 主程序
def main():
    # 读取两个strings.xml文件
    cn_strings_file = os.path.join(app_dir, "res", "values", "strings.xml")
    en_strings_file = os.path.join(app_dir, "res", "values-en", "strings.xml")

    print("=== 分析项目中未使用的字符串资源 ===\n")

    # 解析strings.xml
    cn_strings = parse_strings_xml(cn_strings_file)
    en_strings = parse_strings_xml(en_strings_file)

    print(f"中文 strings.xml: {len(cn_strings)} 个字符串")
    print(f"英文 strings.xml: {len(en_strings)} 个字符串\n")

    # 找出所有被使用的字符串
    used_strings = find_used_strings(app_dir)
    print(f"项目中被使用的字符串: {len(used_strings)} 个\n")

    # 找出未被使用的字符串
    unused_cn = cn_strings - used_strings
    unused_en = en_strings - used_strings

    print(f"=== 未被使用的字符串 ===\n")
    print(f"中文中未使用: {len(unused_cn)} 个")
    print(f"英文中未使用: {len(unused_en)} 个")
    print(f"两个文件都未使用: {len(unused_cn & unused_en)} 个\n")

    # 输出详细列表
    if unused_cn:
        print("\n--- 中文 (values/strings.xml) 中未使用的字符串 ---")
        for name in sorted(unused_cn):
            print(f"  {name}")

    if unused_en:
        print("\n--- 英文 (values-en/strings.xml) 中未使用的字符串 ---")
        for name in sorted(unused_en):
            print(f"  {name}")

    # 找出仅在中文或仅在英文中未使用的
    only_cn_unused = unused_cn - unused_en
    only_en_unused = unused_en - unused_cn
    both_unused = unused_cn & unused_en

    if both_unused:
        print(f"\n--- 两个文件中都未使用的字符串 ({len(both_unused)} 个) ---")
        for name in sorted(both_unused):
            print(f"  {name}")

if __name__ == "__main__":
    main()

