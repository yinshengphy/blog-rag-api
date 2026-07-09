package cn.yinsheng.blog.rag.skill.impl;

import cn.yinsheng.blog.rag.intent.IntentType;
import cn.yinsheng.blog.rag.skill.CapabilityDescriptor;
import cn.yinsheng.blog.rag.skill.Skill;
import cn.yinsheng.blog.rag.skill.SkillRequest;
import cn.yinsheng.blog.rag.skill.SkillResult;
import cn.yinsheng.blog.rag.skill.SkillRiskLevel;
import cn.yinsheng.blog.rag.skill.SkillStatus;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CalculatorSkill implements Skill {
  @Override
  public String id() {
    return "calculator";
  }

  @Override
  public CapabilityDescriptor descriptor() {
    return new CapabilityDescriptor(
        id(),
        "安全计算器",
        "通用工具",
        "计算只包含数字和 + - * / % 括号的小表达式。",
        SkillRiskLevel.READ_ONLY_LOCAL,
        SkillStatus.ENABLED,
        List.of(IntentType.CALCULATOR)
    );
  }

  @Override
  public SkillResult execute(SkillRequest request) {
    String expression = extractExpression(request.question());
    try {
      BigDecimal result = new Parser(expression).parse();
      String value = result.stripTrailingZeros().toPlainString();
      return SkillResult.answer(expression + " = " + value, Map.of("expression", expression, "result", value));
    } catch (RuntimeException ex) {
      return SkillResult.answer("这个表达式我没法安全计算。请只使用数字、+、-、*、/、%、小数点和括号。");
    }
  }

  private String extractExpression(String question) {
    String expression = question
        .replace("×", "*")
        .replace("÷", "/")
        .replaceAll("[^0-9+\\-*/%().\\s]", "")
        .trim();
    if (expression.isBlank() || !expression.matches("[0-9+\\-*/%().\\s]+")) {
      throw new IllegalArgumentException("表达式包含非法字符");
    }
    return expression;
  }

  private static class Parser {
    private final String text;
    private int pos;

    Parser(String text) {
      this.text = text.replaceAll("\\s+", "");
    }

    BigDecimal parse() {
      BigDecimal value = expression();
      if (pos != text.length()) {
        throw new IllegalArgumentException("表达式未完整解析");
      }
      return value;
    }

    private BigDecimal expression() {
      BigDecimal value = term();
      while (match('+') || match('-')) {
        char operator = text.charAt(pos - 1);
        BigDecimal right = term();
        value = operator == '+' ? value.add(right) : value.subtract(right);
      }
      return value;
    }

    private BigDecimal term() {
      BigDecimal value = factor();
      while (match('*') || match('/') || match('%')) {
        char operator = text.charAt(pos - 1);
        BigDecimal right = factor();
        if (BigDecimal.ZERO.compareTo(right) == 0 && (operator == '/' || operator == '%')) {
          throw new ArithmeticException("除数不能为 0");
        }
        if (operator == '*') {
          value = value.multiply(right);
        } else if (operator == '/') {
          value = value.divide(right, MathContext.DECIMAL64);
        } else {
          value = value.remainder(right);
        }
      }
      return value;
    }

    private BigDecimal factor() {
      if (match('+')) {
        return factor();
      }
      if (match('-')) {
        return factor().negate();
      }
      if (match('(')) {
        BigDecimal value = expression();
        if (!match(')')) {
          throw new IllegalArgumentException("缺少右括号");
        }
        return value;
      }
      int start = pos;
      while (pos < text.length() && (Character.isDigit(text.charAt(pos)) || text.charAt(pos) == '.')) {
        pos++;
      }
      if (start == pos) {
        throw new IllegalArgumentException("缺少数字");
      }
      return new BigDecimal(text.substring(start, pos));
    }

    private boolean match(char expected) {
      if (pos < text.length() && text.charAt(pos) == expected) {
        pos++;
        return true;
      }
      return false;
    }
  }
}
