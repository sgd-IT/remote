---
name: code-conciseness-check
description: Checks code for conciseness issues including duplicate code blocks, overly long methods, unnecessary complexity, redundant conditional logic, and simplifiable expressions. Use when the user asks to check code conciseness, review code simplicity, simplify code, reduce code complexity, or make code cleaner.
---

# Code Conciseness Check

使用本 skill 时，输出开头标注【项目名·code-conciseness-check】，项目名取当前工作区根目录名（如 hc-standard、remote-monitor）。

## Quick Start

When checking code conciseness:

1. Read the target file(s) completely
2. Run through each check category below
3. Output a structured report with findings and suggestions

## Check Categories

### 1. Duplicate Code

- Identify identical or near-identical code blocks within the same file or across related files
- Flag copy-paste patterns that should be extracted into reusable methods
- Threshold: 3+ lines of identical logic

### 2. Overly Long Methods

- Flag methods exceeding 50 lines (excluding blank lines and comments)
- Suggest decomposition into smaller, focused methods
- Pay attention to methods with multiple levels of indentation

### 3. Unnecessary Complexity

- Nested conditionals that can be flattened (guard clauses, early returns)
- Over-engineered abstractions for simple operations
- Unnecessary design patterns that add indirection without benefit
- Excessive parameter counts (5+ parameters → consider parameter object)

### 4. Redundant Conditional Logic

- `if (x) return true; else return false;` → `return x;`
- Ternary wrapping a boolean: `condition ? true : false` → `condition`
- Redundant null checks after instanceof
- Empty if/else blocks
- Conditions that are always true or always false

### 5. Simplifiable Expressions

- String concatenation in loops → StringBuilder
- Repeated method calls with same arguments → store in variable
- Unnecessary boxing/unboxing
- Verbose collection initialization → use factory methods
- Multi-line operations that have built-in library equivalents

### 6. Dead Code

- Unreachable code after return/throw/break
- Unused private methods or fields
- Unnecessary imports
- Commented-out code blocks

## Report Template

Output findings in this format:

```
# 代码简洁度检查报告

**文件**: [file path]
**总体评分**: [A/B/C/D] (A=非常简洁, D=严重冗余)

## 发现问题

### [严重程度] 问题标题
- **位置**: 行 X-Y
- **类别**: [重复代码/过长方法/不必要复杂性/冗余条件/可简化表达式/死代码]
- **问题描述**: ...
- **建议修改**:
  ```java
  // 简化后的代码
  ```

## 统计摘要
- 检查文件数: X
- 发现问题数: X
  - 严重: X  |  建议: X  |  可选: X
```

## Severity Levels

- **严重 (Critical)**: Dead code, always-false conditions, logic errors from redundancy
- **建议 (Suggestion)**: Long methods, duplicate code, unnecessary complexity
- **可选 (Nice to have)**: Minor simplifications, style improvements

## Language-Specific Notes

For Java (this project's primary language):
- Prefer `try-with-resources` over manual close
- Use Stream API where it improves readability
- Prefer `Optional` over null checks where appropriate
- Use Lombok annotations to reduce boilerplate if the project uses Lombok
- Prefer method references over lambda when possible

## Additional Resources

- For concrete before/after examples of each check category, see [examples.md](examples.md)
